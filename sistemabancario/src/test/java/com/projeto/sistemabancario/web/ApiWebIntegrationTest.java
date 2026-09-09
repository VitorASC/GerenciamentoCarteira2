package com.projeto.sistemabancario.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.sistemabancario.config.security.JwtService;
import com.projeto.sistemabancario.domains.entity.Acao;
import com.projeto.sistemabancario.domains.entity.HistoricoCotacao;
import com.projeto.sistemabancario.domains.entity.Usuario;
import com.projeto.sistemabancario.domains.enums.Mercado;
import com.projeto.sistemabancario.domains.enums.PerfilInvestidor;
import com.projeto.sistemabancario.repository.AcaoRepository;
import com.projeto.sistemabancario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiWebIntegrationTest {

	private static final String EMAIL = "api.web.integration@test.local";
	private static final String SENHA = "SenhaSegura1!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private AcaoRepository acaoRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	private String accessToken;
	private Long acaoId;

	@BeforeEach
	void setUp() {
		acaoRepository.deleteAll();
		usuarioRepository.deleteAll();

		Usuario u = new Usuario();
		u.setNomeCompleto("Usuário API Test");
		u.setCpf("52998224725");
		u.setEmail(EMAIL);
		u.setSenhaHash(passwordEncoder.encode(SENHA));
		u.setDataNascimento(LocalDate.of(1990, 5, 10));
		u.setPerfilInvestidor(PerfilInvestidor.MODERADO);
		u.setDataCriacao(LocalDateTime.now());
		u.setAtivo(Boolean.TRUE);
		u = usuarioRepository.saveAndFlush(u);

		Acao acao = new Acao();
		acao.setTicker("T" + UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase());
		acao.setNomeEmpresa("Empresa API");
		acao.setMercado(Mercado.BRASIL);
		acao.setMoeda("BRL");
		acao.setCotacaoAtual(new BigDecimal("15.50"));
		acao.setDataHoraCotacao(LocalDateTime.now());

		HistoricoCotacao h = new HistoricoCotacao();
		h.setAcao(acao);
		h.setValor(new BigDecimal("15.50"));
		h.setDataHora(LocalDateTime.of(2025, 3, 1, 15, 30));
		acao.getHistoricoCotacoes().add(h);

		acao = acaoRepository.saveAndFlush(acao);
		acaoId = acao.getId();

		accessToken = jwtService.gerarToken(u.getId(), u.getEmail(), true);
	}

	@Test
	void dashboardPublicoEncaminhaParaSpa() throws Exception {
		mockMvc.perform(get("/dashboard"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/index.html"));
	}

	@Test
	void loginRetornaJwt() throws Exception {
		var body = objectMapper.createObjectNode();
		body.put("email", EMAIL);
		body.put("senha", SENHA);

		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.tokenType").value("Bearer"));
	}

	@Test
	void historicoCotacoesSemTokenRetorna401() throws Exception {
		mockMvc.perform(get("/acoes/" + acaoId + "/historico-cotacoes")).andExpect(status().isUnauthorized());
	}

	@Test
	void historicoCotacoesComTokenRetornaConteudo() throws Exception {
		mockMvc.perform(get("/acoes/" + acaoId + "/historico-cotacoes").param("size", "50")
				.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].valor").value(15.5));
	}
}
