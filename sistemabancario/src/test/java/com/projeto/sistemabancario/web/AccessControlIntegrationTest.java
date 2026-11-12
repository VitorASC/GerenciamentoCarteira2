package com.projeto.sistemabancario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
	void cadastroPersistePerfilAcessoSemConfundirComPerfilInvestidor() throws Exception {
		mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
				{"nomeCompleto":"Nova Padrão","cpf":"44444444444","email":"nova.padrao@teste.local",
				 "senha":"Senha123!","perfilInvestidor":"ARROJADO","perfilAcesso":"PADRAO"}
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
				.andExpect(jsonPath("$.perfilAcesso").value("ADMIN"));

		assertThat(usuarioRepository.findByEmailIgnoreCase("nova.padrao@teste.local").orElseThrow().getPerfilAcesso())
				.isEqualTo(PerfilAcesso.PADRAO);
		assertThat(usuarioRepository.findByEmailIgnoreCase("nova.admin@teste.local").orElseThrow().getPerfilAcesso())
				.isEqualTo(PerfilAcesso.ADMIN);
	}

	@Test
	void cadastroRejeitaPerfilAcessoForaDoEnum() throws Exception {
		mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
				{"nomeCompleto":"Inválido","cpf":"66666666666","email":"invalido@teste.local",
				 "senha":"Senha123!","perfilInvestidor":"MODERADO","perfilAcesso":"SUPER_ADMIN"}
				"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("Validação"));
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
	void tokenInvalidoOuExpiradoRetorna401MasFaltaDePermissaoRetorna403() throws Exception {
		mockMvc.perform(get("/carteiras").header("Authorization", "Bearer token-invalido"))
				.andExpect(status().isUnauthorized());

		JwtProperties properties = new JwtProperties();
		properties.setSecret("test-jwt-secret-at-least-32-characters-long-for-hs256!!");
		properties.setExpirationMs(-1L);
		String expirado = new JwtService(properties).gerarToken(
				padrao.getId(), padrao.getEmail(), true, PerfilAcesso.PADRAO);
		mockMvc.perform(get("/carteiras").header("Authorization", bearer(expirado)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/acoes").header("Authorization", bearer(tokenPadrao))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"ticker\":\"VALE3\",\"mercado\":\"BRASIL\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensagem").value("Você não tem permissão para realizar esta operação."));
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

	private record OperacaoPayload(Long acaoId, String quantidade, String precoUnitario) {
	}
}
