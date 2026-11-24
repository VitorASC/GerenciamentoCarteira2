package com.projeto.sistemabancario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.projeto.sistemabancario.AmbienteDeTeste;
import com.projeto.sistemabancario.util.HashSha256;

@AmbienteDeTeste
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:aula17;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AuditoriaSegurancaHttpTest extends CenarioSessaoHttp {
	private static final Instant INICIO = Instant.parse("2026-01-01T12:00:00Z");
	private static final String AUDITORIA = "/auditoria-seguranca";
	@MockitoBean
	private Clock clock;
	@Autowired
	private JdbcTemplate jdbc;
	private String email;
	private String chave;

	@BeforeEach
	void preparar() throws Exception {
		when(clock.instant()).thenReturn(INICIO);
		jdbc.update("delete from eventos_seguranca");
		jdbc.update("delete from protecao_autenticacao");
		jdbc.update("delete from usuarios");
		email = cadastrar("10000000017");
		chave = HashSha256.calcular(email);
	}

	@Test
	void bloqueioPersisteApos401EAcabaNoInstanteExato() throws Exception {
		String variante = "  " + email.toUpperCase(Locale.ROOT) + " ";
		String errada = UUID.randomUUID().toString();
		for (int i = 0; i < 5; i++) {
			recusaLogin(variante, errada);
		}
		when(clock.instant()).thenReturn(INICIO.plusSeconds(899));
		recusaLogin(email, senha);
		assertThat(jdbc.queryForList("select tipo from eventos_seguranca where identificador_hash = ? order by id",
				String.class, chave)).containsExactly("LOGIN_FALHA", "LOGIN_FALHA", "LOGIN_FALHA", "LOGIN_FALHA",
						"LOGIN_FALHA", "BLOQUEIO_TEMPORARIO_ATIVADO", "LOGIN_RECUSADO_BLOQUEIO");
		assertThat(jdbc.queryForList("select chave_login from protecao_autenticacao", String.class))
				.containsExactly(chave);
		when(clock.instant()).thenReturn(INICIO.plusSeconds(900));
		var par = login(variante);
		assertThat(jdbc.queryForObject("""
				select count(*) from protecao_autenticacao where chave_login = ? and falhas_consecutivas = 0
				and janela_iniciada_em is null and bloqueada_ate is null
				""", Integer.class, chave)).isOne();
		recusaLogin(email, errada);
		assertThat(jdbc.queryForObject("select falhas_consecutivas from protecao_autenticacao where chave_login = ?",
				Integer.class, chave)).isOne();
		String refresh = par.get("refreshToken").asText();
		par(renovar(par));
		String eventos = jdbc.queryForList("select * from eventos_seguranca").toString();
		assertThat(eventos).contains("LOGIN_SUCESSO", "REFRESH_SUCESSO")
				.doesNotContain(email, errada, senha, par.get("accessToken").asText(), refresh, HashSha256.calcular(refresh));
	}

	@Test
	void apenasAdminConsultaPaginasEFiltrosComDtoRestrito() throws Exception {
		var padrao = login(email);
		mvc.perform(get(AUDITORIA)).andExpect(status().isUnauthorized());
		mvc.perform(get(AUDITORIA).header("Authorization", bearer(padrao))).andExpect(status().isForbidden());
		String adminEmail = cadastrar("20000000017");
		jdbc.update("update usuarios set perfil_acesso = 'ADMIN' where email = ?", adminEmail);
		var admin = login(adminEmail);
		String errada = UUID.randomUUID().toString();
		recusaLogin(email, errada);
		recusaLogin(email, errada);
		long primeiroId = 0;
		for (int pagina = 0; pagina < 2; pagina++) {
			var resposta = mvc.perform(get(AUDITORIA).header("Authorization", bearer(admin))
					.param("page", String.valueOf(pagina)).param("size", "1").param("tipo", "LOGIN_FALHA")
					.param("inicio", INICIO.toString()).param("fim", INICIO.plusSeconds(1).toString()))
					.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
					.andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.number").value(pagina))
					.andExpect(jsonPath("$.content[0].tipo").value("LOGIN_FALHA"))
					.andReturn().getResponse().getContentAsByteArray();
			JsonNode evento = mapper.readTree(resposta).get("content").get(0);
			assertThat(evento.fieldNames()).toIterable().containsExactlyInAnyOrder("id", "tipo", "ocorridoEm",
					"atorUsuarioId", "alvoUsuarioId", "enderecoOrigem", "motivoRevogacao");
			if (pagina == 0) {
				primeiroId = evento.get("id").asLong();
			}
			else {
				assertThat(evento.get("id").asLong()).isLessThan(primeiroId);
			}
		}
		mvc.perform(get(AUDITORIA).header("Authorization", bearer(admin)).param("fim", INICIO.toString()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void alteracoesReaisDeIdentidadeRegistramAtorAlvoEInstante() throws Exception {
		String adminEmail = cadastrar("20000000017");
		jdbc.update("update usuarios set perfil_acesso = 'ADMIN' where email = ?", adminEmail);
		var admin = login(adminEmail);
		Long alvo = id(email);
		Long ator = id(adminEmail);
		alterar(admin, alvo + "/perfil-acesso", Map.of("perfilAcesso", "ADMIN")).andExpect(status().isOk());
		alterar(admin, alvo.toString(), Map.of("ativo", false, "email", UUID.randomUUID() + "@example.test"))
				.andExpect(status().isOk());
		alterar(admin, alvo.toString(), Map.of("ativo", true)).andExpect(status().isOk());
		alterar(admin, alvo.toString(), Map.of("ativo", true)).andExpect(status().isOk());
		mvc.perform(get(AUDITORIA).header("Authorization", bearer(admin))
				.param("ator", ator.toString()).param("alvo", alvo.toString()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(4))
				.andExpect(jsonPath("$.content[0].tipo").value("USUARIO_ATIVADO"))
				.andExpect(jsonPath("$.content[1].tipo").value("USUARIO_INATIVADO"))
				.andExpect(jsonPath("$.content[2].tipo").value("EMAIL_ALTERADO"))
				.andExpect(jsonPath("$.content[3].tipo").value("PERFIL_ALTERADO"))
				.andExpect(jsonPath("$.content[3].ocorridoEm").value(INICIO.toString()));
	}

	@Test
	void falhaAoGravarEventoDesfazAlteracaoAdministrativa() throws Exception {
		String adminEmail = cadastrar("20000000017");
		jdbc.update("update usuarios set perfil_acesso = 'ADMIN' where email = ?", adminEmail);
		var admin = login(adminEmail);
		jdbc.execute("alter table eventos_seguranca add constraint simular_falha check (tipo <> 'USUARIO_INATIVADO')");
		try {
			alterar(admin, id(email).toString(), Map.of("ativo", false, "email", UUID.randomUUID() + "@example.test"))
					.andExpect(status().is4xxClientError());
			assertThat(jdbc.queryForObject("select ativo from usuarios where email = ?", Boolean.class, email)).isTrue();
			assertThat(jdbc.queryForList("select tipo from eventos_seguranca", String.class)).containsExactly("LOGIN_SUCESSO");
		}
		finally {
			jdbc.execute("alter table eventos_seguranca drop constraint simular_falha");
		}
	}

	private void recusaLogin(String identificador, String credencial) throws Exception {
		enviar("/auth/login", Map.of("email", identificador, "senha", credencial))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.erro").value("Credenciais inválidas"))
				.andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
	}

	private Long id(String titular) {
		return jdbc.queryForObject("select id from usuarios where email = ?", Long.class, titular);
	}

	private ResultActions alterar(JsonNode par, String caminho, Object campos) throws Exception {
		return mvc.perform(put("/usuarios/" + caminho).header("Authorization", bearer(par))
				.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(campos)));
	}

	private String bearer(JsonNode par) {
		return "Bearer " + par.get("accessToken").asText();
	}
}
