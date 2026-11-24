package com.projeto.sistemabancario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.projeto.sistemabancario.AmbienteDeTeste;
import com.projeto.sistemabancario.config.security.JwtService;

@AmbienteDeTeste
@SpringBootTest(properties = {"app.security.jwt.expiration-ms=900000", "app.security.refresh.absolute-expiration=PT45M"})
@AutoConfigureMockMvc
class SessaoHttpIntegrationTest extends CenarioSessaoHttp {

	private static final Instant INICIO = Instant.parse("2026-01-01T08:00:00Z");
	@MockitoBean
	private Clock clock;
	@Autowired
	private JwtService jwt;
	@Autowired
	private JdbcTemplate jdbc;
	private String email;
	private String outroEmail;

	@BeforeEach
	void preparar() throws Exception {
		when(clock.instant()).thenReturn(INICIO);
		email = cadastrar("10000000016");
	}

	@AfterEach
	void limparContasDeTeste() {
		jdbc.update("delete from usuarios where email in (?, ?)", email, outroEmail);
	}

	@Test
	void rotacionaDoisParesEReplayDaPrimeiraGeracaoRevogaAteATerceira() throws Exception {
		JsonNode primeiro = login(email);
		assertThat(primeiro.get("expiresIn").asLong()).isEqualTo(900);
		String original = primeiro.get("refreshToken").asText();
		assertThat(original).matches("[A-Za-z0-9_-]{43}");
		String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(original.getBytes(StandardCharsets.UTF_8)));
		UUID familia = familia(primeiro);
		assertThat(jdbc.queryForList("select token_hash from sessoes_refresh_token where familia_id = ?", String.class, familia))
				.containsExactly(hash).doesNotContain(original);
		var segundo = par(renovar(primeiro));
		var terceiro = par(renovar(segundo));
		assertThat(segundo.get("refreshToken")).isNotEqualTo(primeiro.get("refreshToken"));
		assertThat(segundo.get("accessToken")).isNotEqualTo(primeiro.get("accessToken"));
		assertThat(familia(terceiro)).isEqualTo(familia);
		var outraFamilia = login(email);
		assertThat(familia(outraFamilia)).isNotEqualTo(familia);

		verificarRecusa(renovar(primeiro));
		verificarRecusa(renovar(terceiro));
		assertThat(jdbc.queryForList("select motivo_revogacao from sessoes_refresh_token where familia_id = ?", String.class, familia))
				.containsExactly("REUTILIZACAO", "REUTILIZACAO", "REUTILIZACAO");
		assertThat(jdbc.queryForObject("select count(utilizada_em) from sessoes_refresh_token where familia_id = ?", Integer.class, familia))
				.isEqualTo(2);
		par(renovar(outraFamilia));
	}

	@Test
	void inatividadeNoLimiteRevogaMasUmSegundoAntesPermiteRenovar() throws Exception {
		var primeira = login(email);
		var segunda = login(email);
		when(clock.instant()).thenReturn(INICIO.plusSeconds(1799));
		par(renovar(primeira));
		when(clock.instant()).thenReturn(INICIO.plusSeconds(1800));
		verificarRecusa(renovar(segunda));
		verificarMotivo(email, "INATIVIDADE", 1);
	}

	@Test
	void renovarNaVesperaDoLimiteAbsolutoNaoProrrogaFamilia() throws Exception {
		var sessao = login(email);
		when(clock.instant()).thenReturn(INICIO.plusSeconds(1700));
		sessao = par(renovar(sessao));
		when(clock.instant()).thenReturn(INICIO.plusSeconds(2699));
		sessao = par(renovar(sessao));
		when(clock.instant()).thenReturn(INICIO.plusSeconds(2700));
		verificarRecusa(renovar(sessao));
		verificarMotivo(email, "EXPIRACAO_ABSOLUTA", 3);
	}

	@Test
	void desativarContaImpedeRenovacaoMesmoDepoisDeReativar() throws Exception {
		var sessao = login(email);
		jdbc.update("update usuarios set ativo = false where email = ?", email);
		verificarRecusa(renovar(sessao));
		jdbc.update("update usuarios set ativo = true where email = ?", email);
		verificarRecusa(renovar(sessao));
		verificarMotivo(email, "USUARIO_INATIVO", 1);
	}

	@Test
	void logoutLocalEncerraSomenteAFamiliaEOAccessRespeitaSeuExp() throws Exception {
		var sessao = login(email);
		var outra = login(email);
		var atual = par(renovar(sessao));
		enviar("/auth/logout", Map.of("refreshToken", atual.get("refreshToken").asText())).andExpect(status().isNoContent());
		verificarRecusa(renovar(atual));
		verificarMotivo(email, "LOGOUT", 2);
		par(renovar(outra));
		String bearer = "Bearer " + atual.get("accessToken").asText();
		when(clock.instant()).thenReturn(INICIO.plusSeconds(899));
		mvc.perform(get("/acoes").header("Authorization", bearer)).andExpect(status().isOk());
		when(clock.instant()).thenReturn(INICIO.plusSeconds(900));
		mvc.perform(get("/acoes").header("Authorization", bearer)).andExpect(status().isUnauthorized());
	}

	@Test
	void logoutGlobalRequerBearerEEncerraSomenteAsFamiliasDoTitular() throws Exception {
		var primeira = login(email);
		var segunda = login(email);
		outroEmail = cadastrar("20000000016");
		var alheia = login(outroEmail);
		mvc.perform(post("/auth/logout-global")).andExpect(status().isUnauthorized());
		mvc.perform(post("/auth/logout-global").header("Authorization", "Bearer " + primeira.get("accessToken").asText()))
				.andExpect(status().isNoContent());
		verificarRecusa(renovar(primeira));
		verificarRecusa(renovar(segunda));
		verificarMotivo(email, "LOGOUT_GLOBAL", 2);
		par(renovar(alheia));
	}

	@Test
	void renovaAposExpDoAccessEnquantoRefreshEstaAtivo() throws Exception {
		var sessao = login(email);
		when(clock.instant()).thenReturn(INICIO.plusSeconds(901));
		mvc.perform(get("/acoes").header("Authorization", "Bearer " + sessao.get("accessToken").asText()))
				.andExpect(status().isUnauthorized());
		var novo = par(renovar(sessao));
		mvc.perform(get("/acoes").header("Authorization", "Bearer " + novo.get("accessToken").asText()))
				.andExpect(status().isOk());
	}

	@Test
	void refreshAusenteVazioEDesconhecidoRecebemMesmaResposta() throws Exception {
		verificarRecusa(mvc.perform(post("/auth/refresh")));
		verificarRecusa(enviar("/auth/refresh", Map.of()));
		verificarRecusa(enviar("/auth/refresh", Map.of("refreshToken", "")));
		verificarRecusa(enviar("/auth/refresh", Map.of("refreshToken", "x".repeat(43))));
	}

	private UUID familia(JsonNode par) {
		return UUID.fromString(jwt.parseClaims(par.get("accessToken").asText()).get("sid", String.class));
	}

	private void verificarMotivo(String titular, String motivo, int quantidade) {
		assertThat(jdbc.queryForObject("""
				select count(*) from sessoes_refresh_token s join usuarios u on u.id = s.usuario_id
				where u.email = ? and s.motivo_revogacao = ? and s.revogada_em is not null
				""", Integer.class, titular, motivo)).isEqualTo(quantidade);
	}
}
