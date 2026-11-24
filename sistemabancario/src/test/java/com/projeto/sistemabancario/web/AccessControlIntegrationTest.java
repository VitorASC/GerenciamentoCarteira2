package com.projeto.sistemabancario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.sistemabancario.AmbienteDeTeste;
import com.projeto.sistemabancario.config.security.JwtProperties;
import com.projeto.sistemabancario.config.security.JwtService;
import com.projeto.sistemabancario.domains.entity.Acao;
import com.projeto.sistemabancario.domains.entity.CarteiraInvestimento;
import com.projeto.sistemabancario.domains.entity.Usuario;
import com.projeto.sistemabancario.domains.enums.Mercado;
import com.projeto.sistemabancario.domains.enums.PerfilAcesso;
import com.projeto.sistemabancario.domains.enums.PerfilInvestidor;
import com.projeto.sistemabancario.repository.AcaoRepository;
import com.projeto.sistemabancario.repository.CarteiraInvestimentoRepository;
import com.projeto.sistemabancario.repository.UsuarioRepository;
import com.projeto.sistemabancario.service.AcaoService;
import com.projeto.sistemabancario.service.CorretoraService;

import io.jsonwebtoken.Jwts;

@AmbienteDeTeste
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccessControlIntegrationTest {

	private static final String SENHA = "SenhaSegura1!";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private CarteiraInvestimentoRepository carteiraRepository;
	@Autowired
	private AcaoRepository acaoRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private JwtService jwtService;
	@Autowired
	private JwtProperties jwtProperties;

	@MockitoBean
	private AcaoService acaoService;
	@MockitoBean
	private CorretoraService corretoraService;

	private Usuario padrao;
	private Usuario outroPadrao;
	private Usuario admin;
	private CarteiraInvestimento carteiraPadrao;
	private CarteiraInvestimento carteiraOutro;
	private Acao acao;
	private String tokenPadrao;
	private String tokenAdmin;

	@BeforeEach
	void setUp() {
		padrao = salvarUsuario("Usuário Padrão", "11111111111", "padrao@teste.local", PerfilAcesso.PADRAO);
		outroPadrao = salvarUsuario("Outro Usuário", "22222222222", "outro@teste.local", PerfilAcesso.PADRAO);
		admin = salvarUsuario("Usuário Admin", "33333333333", "admin@teste.local", PerfilAcesso.ADMIN);

		acao = new Acao();
		acao.setTicker("TEST3");
		acao.setNomeEmpresa("Empresa Teste");
		acao.setMercado(Mercado.BRASIL);
		acao.setMoeda("BRL");
		acao.setCotacaoAtual(new BigDecimal("10.00"));
		acao.setDataHoraCotacao(LocalDateTime.now());
		acao = acaoRepository.saveAndFlush(acao);

		carteiraPadrao = salvarCarteira(padrao, "Minha carteira");
		carteiraOutro = salvarCarteira(outroPadrao, "Carteira de terceiro");

		tokenPadrao = jwtService.gerarToken(padrao.getId(), padrao.getEmail(), true, PerfilAcesso.PADRAO);
		tokenAdmin = jwtService.gerarToken(admin.getId(), admin.getEmail(), true, PerfilAcesso.ADMIN);
	}

	@Test
	void cadastroPublicoSemprePersistePadraoSemConfundirComPerfilInvestidor() throws Exception {
		mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
				{"nomeCompleto":"Nova Padrão","cpf":"44444444444","email":"nova.padrao@teste.local",
				 "senha":"Senha123!","perfilInvestidor":"ARROJADO"}
				"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.perfilInvestidor").value("ARROJADO"))
				.andExpect(jsonPath("$.perfilAcesso").value("PADRAO"));

		mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
				{"nomeCompleto":"Nova Admin","cpf":"55555555555","email":"nova.admin@teste.local",
				 "senha":"Senha123!","perfilInvestidor":"CONSERVADOR","perfilAcesso":"ADMIN"}
				"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.perfilInvestidor").value("CONSERVADOR"))
				.andExpect(jsonPath("$.perfilAcesso").value("PADRAO"));

		assertThat(usuarioRepository.findByEmailIgnoreCase("nova.padrao@teste.local").orElseThrow().getPerfilAcesso())
				.isEqualTo(PerfilAcesso.PADRAO);
		assertThat(usuarioRepository.findByEmailIgnoreCase("nova.admin@teste.local").orElseThrow().getPerfilAcesso())
				.isEqualTo(PerfilAcesso.PADRAO);
	}

	@Test
	void cadastroIgnoraPerfilAcessoForaDoEnum() throws Exception {
		mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
				{"nomeCompleto":"Inválido","cpf":"66666666666","email":"invalido@teste.local",
				 "senha":"Senha123!","perfilInvestidor":"MODERADO","perfilAcesso":"SUPER_ADMIN"}
				"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.perfilAcesso").value("PADRAO"));
	}

	@Test
	void loginDePadraoEAdminRetornaPerfilEJwtComAuthorityCorreta() throws Exception {
		String jwtPadrao = login(padrao.getEmail(), "PADRAO");
		String jwtAdmin = login(admin.getEmail(), "ADMIN");

		assertThat(jwtService.parseClaims(jwtPadrao).get("perfilAcesso", String.class)).isEqualTo("PADRAO");
		assertThat(jwtService.parseClaims(jwtAdmin).get("perfilAcesso", String.class)).isEqualTo("ADMIN");

		mockMvc.perform(get("/usuarios").header("Authorization", bearer(jwtPadrao)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/usuarios").header("Authorization", bearer(jwtAdmin)))
				.andExpect(status().isOk());
	}

	@Test
	void loginEmiteJwtComClaimsControladasPeloServidorEJtiUnico() throws Exception {
		String resposta = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new LoginComPerfilPayload(padrao.getEmail(), SENHA, "ADMIN"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresInMs").value(jwtProperties.getExpirationMs()))
				.andExpect(jsonPath("$.perfilAcesso").value("PADRAO"))
				.andReturn().getResponse().getContentAsString();
		String jwt = objectMapper.readTree(resposta).get("accessToken").asText();
		var claims = jwtService.parseClaims(jwt);
		assertThat(claims.getSubject()).isEqualTo(padrao.getId().toString());
		assertThat(claims.getIssuer()).isEqualTo(jwtProperties.getIssuer());
		assertThat(claims.getIssuedAt()).isNotNull();
		assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
		assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
				.isEqualTo(jwtProperties.getExpirationMs());
		assertThat(claims.getId()).isNotBlank()
				.isNotEqualTo(jwtService.parseClaims(login(padrao.getEmail(), "PADRAO")).getId());
		assertThat(claims.get("perfilAcesso")).isEqualTo("PADRAO");
		String payload = new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
		assertThat(payload).doesNotContain("senha", "password", SENHA, padrao.getSenhaHash());
	}

	@Test
	void loginDeCredenciaisInvalidasOuContaInativaTemAMesmaResposta() throws Exception {
		outroPadrao.setAtivo(false);
		usuarioRepository.saveAndFlush(outroPadrao);
		for (LoginPayload login : new LoginPayload[]{
				new LoginPayload(padrao.getEmail(), "senha-errada"),
				new LoginPayload("inexistente@teste.local", SENHA),
				new LoginPayload(outroPadrao.getEmail(), SENHA)}) {
			mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(login)))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.erro").value("Credenciais inválidas"))
					.andExpect(jsonPath("$.mensagem").value(
							"E-mail ou senha inválidos. Se houver muitas tentativas consecutivas, aguarde alguns minutos antes de tentar novamente."));
		}
	}

	@Test
	void frontendContinuaPublicoERotasSemPoliticaSaoNegadas() throws Exception {
		for (String path : new String[]{"/", "/dashboard"}) {
			mockMvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
		}
		mockMvc.perform(get("/index.html")).andExpect(status().isOk());
		mockMvc.perform(get("/assets/favicon.svg")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/actuator/health").header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/usuarios/exportacao").header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/rotas-sem-politica")).andExpect(status().isUnauthorized());
	}

	@Test
	void somenteAdminExecutaOperacoesAdministrativasDeAcoes() throws Exception {
		String body = "{\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\"}";
		mockMvc.perform(post("/acoes").header("Authorization", bearer(tokenAdmin))
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
		mockMvc.perform(post("/acoes").header("Authorization", bearer(tokenPadrao))
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));

		mockMvc.perform(put("/acoes/1/atualizar-cotacao").header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk());
		mockMvc.perform(put("/acoes/1/atualizar-cotacao").header("Authorization", bearer(tokenPadrao)))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/acoes/1").header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/acoes/1").header("Authorization", bearer(tokenPadrao)))
				.andExpect(status().isForbidden());
	}

	@Test
	void somenteAdminExecutaOperacoesAdministrativasDeCorretoras() throws Exception {
		String body = "{\"cnpj\":\"11222333000181\",\"cep\":\"01001000\"}";
		mockMvc.perform(post("/corretoras").header("Authorization", bearer(tokenAdmin))
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
		mockMvc.perform(post("/corretoras").header("Authorization", bearer(tokenPadrao))
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
		mockMvc.perform(delete("/corretoras/1").header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/corretoras/1").header("Authorization", bearer(tokenPadrao)))
				.andExpect(status().isForbidden());
	}

	@Test
	void leiturasDeAcoesECorretorasContinuamDisponiveisAoPadrao() throws Exception {
		mockMvc.perform(get("/acoes").header("Authorization", bearer(tokenPadrao))).andExpect(status().isOk());
		mockMvc.perform(get("/corretoras").header("Authorization", bearer(tokenPadrao))).andExpect(status().isOk());
	}

	@Test
	void padraoAdministraSomenteAPropriaContaEAdminPodeAdministrarOutras() throws Exception {
		mockMvc.perform(get("/usuarios/" + padrao.getId()).header("Authorization", bearer(tokenPadrao)))
				.andExpect(status().isOk());
		mockMvc.perform(put("/usuarios/" + padrao.getId()).header("Authorization", bearer(tokenPadrao))
				.contentType(MediaType.APPLICATION_JSON).content("{\"nomeCompleto\":\"Nome Atualizado\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nomeCompleto").value("Nome Atualizado"));
		mockMvc.perform(get("/usuarios/" + outroPadrao.getId()).header("Authorization", bearer(tokenPadrao)))
				.andExpect(status().isForbidden());
		mockMvc.perform(put("/usuarios/" + outroPadrao.getId()).header("Authorization", bearer(tokenPadrao))
				.contentType(MediaType.APPLICATION_JSON).content("{\"nomeCompleto\":\"Ataque\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(put("/usuarios/" + outroPadrao.getId()).header("Authorization", bearer(tokenAdmin))
				.contentType(MediaType.APPLICATION_JSON).content("{\"nomeCompleto\":\"Alterado pelo admin\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void padraoAcessaEOperaSomenteCarteiraPropriaEnquantoAdminMantemAcesso() throws Exception {
		mockMvc.perform(get("/carteiras/" + carteiraPadrao.getId()).header("Authorization", bearer(tokenPadrao)))
				.andExpect(status().isOk());
		mockMvc.perform(put("/carteiras/" + carteiraPadrao.getId()).header("Authorization", bearer(tokenPadrao))
				.contentType(MediaType.APPLICATION_JSON).content("{\"nomeDaCarteira\":\"Atualizada\"}"))
				.andExpect(status().isOk());

		String compra = operacaoBody("1", "10");
		String venda = operacaoBody("1", "11");
		mockMvc.perform(post("/carteiras/" + carteiraPadrao.getId() + "/compras")
				.header("Authorization", bearer(tokenPadrao)).contentType(MediaType.APPLICATION_JSON).content(compra))
				.andExpect(status().isOk());
		mockMvc.perform(post("/carteiras/" + carteiraPadrao.getId() + "/vendas")
				.header("Authorization", bearer(tokenPadrao)).contentType(MediaType.APPLICATION_JSON).content(venda))
				.andExpect(status().isOk());

		mockMvc.perform(get("/carteiras/" + carteiraOutro.getId()).header("Authorization", bearer(tokenPadrao)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/carteiras/" + carteiraOutro.getId() + "/compras")
				.header("Authorization", bearer(tokenPadrao)).contentType(MediaType.APPLICATION_JSON).content(compra))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/carteiras/" + carteiraOutro.getId() + "/vendas")
				.header("Authorization", bearer(tokenPadrao)).contentType(MediaType.APPLICATION_JSON).content(venda))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/carteiras/" + carteiraOutro.getId()).header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk());
	}

	@Test
	void credenciaisValidasComBasicRecebem401EBearerDoLoginPermiteAcesso() throws Exception {
		String email = padrao.getEmail();
		String accessToken = login(email, "PADRAO");
		mockMvc.perform(get("/carteiras").with(httpBasic(email, SENHA)))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", "Bearer"));
		mockMvc.perform(get("/carteiras").header("Authorization", bearer(accessToken)))
				.andExpect(status().isOk());
	}

	@Test
	void tokenInvalidoOuExpiradoRetorna401MasFaltaDePermissaoRetorna403() throws Exception {
		mockMvc.perform(get("/carteiras")).andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", "Bearer"));
		mockMvc.perform(get("/carteiras").header("Authorization", bearer(tokenPadrao)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/carteiras").header("Authorization", "Bearer token-invalido"))
				.andExpect(status().isUnauthorized());

		Instant agora = Instant.now();
		String expirado = assinarToken(jwtProperties.getIssuer(), agora.minusSeconds(60), agora.minusSeconds(30));
		String outroEmissor = assinarToken("outra-aplicacao", agora.minusSeconds(1), agora.plusSeconds(60));
		int inicioAssinatura = tokenPadrao.lastIndexOf('.') + 1;
		char caractereAlterado = tokenPadrao.charAt(inicioAssinatura) == 'A' ? 'B' : 'A';
		String adulterado = tokenPadrao.substring(0, inicioAssinatura) + caractereAlterado
				+ tokenPadrao.substring(inicioAssinatura + 1);
		for (String token : new String[]{expirado, outroEmissor, adulterado}) {
			mockMvc.perform(get("/carteiras").header("Authorization", bearer(token)))
					.andExpect(status().isUnauthorized());
		}

		mockMvc.perform(post("/acoes").header("Authorization", bearer(tokenPadrao))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"ticker\":\"VALE3\",\"mercado\":\"BRASIL\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensagem").value("Você não tem permissão para realizar esta operação."));
	}

	@Test
	void somenteAdminAlteraPerfilDeOutroUsuarioEJwtAnteriorNaoAmpliaPrivilegios() throws Exception {
		String rota = "/usuarios/" + padrao.getId() + "/perfil-acesso";
		String promover = "{\"perfilAcesso\":\"ADMIN\"}";
		mockMvc.perform(put(rota).contentType(MediaType.APPLICATION_JSON).content(promover))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(put(rota).header("Authorization", bearer(tokenPadrao))
				.contentType(MediaType.APPLICATION_JSON).content(promover))
				.andExpect(status().isForbidden());
		mockMvc.perform(put(rota).header("Authorization", bearer(tokenAdmin))
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put(rota).header("Authorization", bearer(tokenAdmin))
				.contentType(MediaType.APPLICATION_JSON).content(promover))
				.andExpect(status().isOk()).andExpect(jsonPath("$.perfilAcesso").value("ADMIN"));
		mockMvc.perform(get("/usuarios").header("Authorization", bearer(tokenPadrao)))
				.andExpect(status().isForbidden());

		String tokenPromovido = login(padrao.getEmail(), "ADMIN");
		mockMvc.perform(get("/usuarios").header("Authorization", bearer(tokenPromovido)))
				.andExpect(status().isOk());
		mockMvc.perform(put(rota).header("Authorization", bearer(tokenAdmin))
				.contentType(MediaType.APPLICATION_JSON).content("{\"perfilAcesso\":\"PADRAO\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.perfilAcesso").value("PADRAO"));
		mockMvc.perform(get("/usuarios").header("Authorization", bearer(tokenPromovido)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/carteiras").header("Authorization", bearer(tokenPromovido)))
				.andExpect(status().isOk());
	}

	@Test
	void administradorNaoAlteraProprioPerfilENaoDesativaUltimoAdminAtivo() throws Exception {
		String contaAdmin = "/usuarios/" + admin.getId();
		mockMvc.perform(put(contaAdmin + "/perfil-acesso").header("Authorization", bearer(tokenAdmin))
				.contentType(MediaType.APPLICATION_JSON).content("{\"perfilAcesso\":\"PADRAO\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(put(contaAdmin).header("Authorization", bearer(tokenAdmin))
				.contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put(contaAdmin).header("Authorization", bearer(tokenAdmin))
				.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"email-invalido\"}"))
				.andExpect(status().isBadRequest());
		assertThat(admin.getAtivo()).isTrue();
		assertThat(admin.getPerfilAcesso()).isEqualTo(PerfilAcesso.ADMIN);
	}

	@Test
	void alteracaoDeContaIgnoraPerfilSolicitadoEAContaDesativadaPerdeAcesso() throws Exception {
		String contaPadrao = "/usuarios/" + padrao.getId();
		mockMvc.perform(put(contaPadrao).header("Authorization", bearer(tokenPadrao))
				.contentType(MediaType.APPLICATION_JSON).content("{\"perfilAcesso\":\"ADMIN\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.perfilAcesso").value("PADRAO"));
		mockMvc.perform(put(contaPadrao).header("Authorization", bearer(tokenAdmin))
				.contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));
		mockMvc.perform(get("/carteiras").header("Authorization", bearer(tokenPadrao)))
				.andExpect(status().isUnauthorized());
	}

	private String assinarToken(String emissor, Instant emissao, Instant expiracao) {
		var chave = new SecretKeySpec(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		return Jwts.builder().issuer(emissor).subject(padrao.getId().toString()).id(UUID.randomUUID().toString())
				.issuedAt(Date.from(emissao)).expiration(Date.from(expiracao))
				.claim("ativo", true).claim("perfilAcesso", "PADRAO").signWith(chave, Jwts.SIG.HS256).compact();
	}

	private String login(String email, String perfilAcesso) throws Exception {
		String response = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new LoginPayload(email, SENHA))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.perfilAcesso").value(perfilAcesso))
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("accessToken").asText();
	}

	private Usuario salvarUsuario(String nome, String cpf, String email, PerfilAcesso perfilAcesso) {
		Usuario usuario = new Usuario();
		usuario.setNomeCompleto(nome);
		usuario.setCpf(cpf);
		usuario.setEmail(email);
		usuario.setSenhaHash(passwordEncoder.encode(SENHA));
		usuario.setPerfilInvestidor(PerfilInvestidor.MODERADO);
		usuario.setPerfilAcesso(perfilAcesso);
		usuario.setDataCriacao(LocalDateTime.now());
		usuario.setAtivo(true);
		return usuarioRepository.saveAndFlush(usuario);
	}

	private CarteiraInvestimento salvarCarteira(Usuario usuario, String nome) {
		CarteiraInvestimento carteira = new CarteiraInvestimento();
		carteira.setUsuario(usuario);
		carteira.setNomeDaCarteira(nome);
		carteira.setSaldoTotal(new BigDecimal("1000.00"));
		carteira.setLucroPrejuizoRealizado(BigDecimal.ZERO);
		carteira.setDataCriacao(LocalDateTime.now());
		return carteiraRepository.saveAndFlush(carteira);
	}

	private String operacaoBody(String quantidade, String preco) throws Exception {
		return objectMapper.writeValueAsString(new OperacaoPayload(acao.getId(), quantidade, preco));
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	private record LoginPayload(String email, String senha) {
	}

	private record LoginComPerfilPayload(String email, String senha, String perfilAcesso) {
	}

	private record OperacaoPayload(Long acaoId, String quantidade, String precoUnitario) {
	}
}
