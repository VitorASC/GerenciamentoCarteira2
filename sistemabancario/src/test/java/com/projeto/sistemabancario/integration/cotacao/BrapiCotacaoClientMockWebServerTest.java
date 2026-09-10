package com.projeto.sistemabancario.integration.cotacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestClient;

import com.projeto.sistemabancario.exception.RegraNegocioException;
import com.projeto.sistemabancario.integration.config.IntegrationProperties;
import com.projeto.sistemabancario.integration.exception.ExternalIntegrationException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class BrapiCotacaoClientMockWebServerTest {

	private MockWebServer server;
	private BrapiCotacaoClient client;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();
		IntegrationProperties properties = new IntegrationProperties();
		properties.getBrapi().setBaseUrl("http://localhost:" + server.getPort());
		client = new BrapiCotacaoClient(RestClient.builder(), properties, new MockEnvironment());
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	@Test
	void sucessoRetornaCotacao() {
		server.enqueue(json("""
				{"results":[{"symbol":"PETR4","shortName":"Petrobras","currency":"BRL",
				"regularMarketPrice":38.12,"regularMarketTime":1788966000}]}
				"""));

		var resultado = client.buscar("PETR4");

		assertThat(resultado).isPresent();
		assertThat(resultado.get().preco()).isEqualByComparingTo("38.120000");
		assertThat(resultado.get().nomeEmpresa()).isEqualTo("Petrobras");
	}

	@Test
	void tickerInexistenteRetornaVazio() {
		server.enqueue(new MockResponse().setResponseCode(404));

		assertThat(client.buscar("PETR4")).isEmpty();
	}

	@Test
	void http429SinalizaFalhaExterna() {
		server.enqueue(new MockResponse().setResponseCode(429));

		assertThatThrownBy(() -> client.buscar("PETR4"))
			.isInstanceOf(ExternalIntegrationException.class)
			.hasMessageContaining("429");
	}

	@Test
	void http5xxSinalizaFalhaExterna() {
		server.enqueue(new MockResponse().setResponseCode(503));

		assertThatThrownBy(() -> client.buscar("PETR4"))
			.isInstanceOf(ExternalIntegrationException.class);
	}

	@Test
	void payloadDeLimitePreservaExcecaoDeNegocio() {
		server.enqueue(json("{" + "\"error\":true,\"message\":\"rate limit exceeded\",\"code\":402}"));

		assertThatThrownBy(() -> client.buscar("PETR4"))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessageContaining("limite");
	}

	private static MockResponse json(String body) {
		return new MockResponse().setBody(body).addHeader("Content-Type", "application/json");
	}
}
