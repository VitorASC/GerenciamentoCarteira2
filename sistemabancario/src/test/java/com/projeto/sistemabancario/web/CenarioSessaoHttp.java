package com.projeto.sistemabancario.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class CenarioSessaoHttp {
	@Autowired
	protected MockMvc mvc;
	@Autowired
	protected ObjectMapper mapper;

	protected final String senha = UUID.randomUUID().toString();

	protected String cadastrar(String cpf) throws Exception {
		String email = UUID.randomUUID() + "@example.test";
		var cadastro = Map.of("nomeCompleto", "Titular de teste", "cpf", cpf, "email", email,
				"senha", senha, "perfilInvestidor", "MODERADO");
		enviar("/usuarios", cadastro).andExpect(status().isCreated());
		return email;
	}

	protected JsonNode login(String email) throws Exception {
		return par(enviar("/auth/login", Map.of("email", email, "senha", senha)));
	}

	protected JsonNode par(ResultActions resposta) throws Exception {
		byte[] json = resposta.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.refreshToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenHash").doesNotExist())
				.andReturn().getResponse().getContentAsByteArray();
		return mapper.readTree(json);
	}

	protected ResultActions enviar(String caminho, Object dados) throws Exception {
		return mvc.perform(post(caminho).contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(dados)));
	}

	protected ResultActions renovar(JsonNode par) throws Exception {
		return enviar("/auth/refresh", Map.of("refreshToken", par.get("refreshToken").asText()));
	}

	protected void verificarRecusa(ResultActions resposta) throws Exception {
		resposta.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.erro").value("Não autenticado"))
				.andExpect(jsonPath("$.mensagem").value("Refresh token inválido ou expirado"))
				.andExpect(jsonPath("$.motivoRevogacao").doesNotExist());
	}
}
