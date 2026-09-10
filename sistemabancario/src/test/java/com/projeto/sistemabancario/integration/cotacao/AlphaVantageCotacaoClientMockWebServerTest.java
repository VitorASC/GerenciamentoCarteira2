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

class AlphaVantageCotacaoClientMockWebServerTest {

	private MockWebServer server;
	private AlphaVantageCotacaoClient client;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();
		IntegrationProperties properties = new IntegrationProperties();
		properties.getAlphaVantage().setBaseUrl("http://localhost:" + server.getPort());
		properties.getAlphaVantage().setApiKey("test-key");
		client = new AlphaVantageCotacaoClient(RestClient.builder(), properties, new MockEnvironment());
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	@Test
	void sucessoRetornaCotacao() {
		server.enqueue(json("""
				{"Global Quote":{"01. symbol":"IBM","05. price":"245.5000",
				"07. latest trading day":"2026-09-08"}}
				"""));

		var resultado = client.buscar("IBM");

		assertThat(resultado).isPresent();
		assertThat(resultado.get().preco()).isEqualByComparingTo("245.500000");
		assertThat(resultado.get().moeda()).isEqualTo("USD");
	}

	@Test
	void tickerInexistenteRetornaVazio() {
		server.enqueue(json("{\"Global Quote\":{}}"));

		assertThat(client.buscar("INVALIDO")).isEmpty();
	}

	@Test
	void http429SinalizaFalhaExterna() {
		server.enqueue(new MockResponse().setResponseCode(429));

		assertThatThrownBy(() -> client.buscar("IBM"))
			.isInstanceOf(ExternalIntegrationException.class)
			.hasMessageContaining("429");
	}

	@Test
	void http5xxSinalizaFalhaExterna() {
		server.enqueue(new MockResponse().setResponseCode(503));

		assertThatThrownBy(() -> client.buscar("IBM"))
			.isInstanceOf(ExternalIntegrationException.class);
	}

	@Test
	void payloadDeLimitePreservaExcecaoDeNegocio() {
		server.enqueue(json("{\"Note\":\"API rate limit reached\"}"));

		assertThatThrownBy(() -> client.buscar("IBM"))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessageContaining("limite");
	}

	@Test
	void payloadDeChaveInvalidaPreservaExcecaoDeNegocio() {
		server.enqueue(json("{\"Error Message\":\"Invalid API key\"}"));

		assertThatThrownBy(() -> client.buscar("IBM"))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessageContaining("API key");
	}

	private static MockResponse json(String body) {
		return new MockResponse().setBody(body).addHeader("Content-Type", "application/json");
	}
}
