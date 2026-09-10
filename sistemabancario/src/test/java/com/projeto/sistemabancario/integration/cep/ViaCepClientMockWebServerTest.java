package com.projeto.sistemabancario.integration.cep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.projeto.sistemabancario.integration.config.IntegrationProperties;
import com.projeto.sistemabancario.integration.exception.ExternalIntegrationException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class ViaCepClientMockWebServerTest {

	private MockWebServer server;
	private ViaCepClient client;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();
		IntegrationProperties properties = new IntegrationProperties();
		properties.getViaCep().setBaseUrl("http://localhost:" + server.getPort());
		client = new ViaCepClient(RestClient.builder(), properties);
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	@Test
	void consultarCepValidoRetornaEndereco() {
		server.enqueue(new MockResponse()
				.setBody(
						"""
								{
								  "cep": "01001-000",
								  "logradouro": "Praça da Sé",
								  "complemento": "lado ímpar",
								  "bairro": "Sé",
								  "localidade": "São Paulo",
								  "uf": "SP",
								  "erro": false
								}
								""")
				.addHeader("Content-Type", "application/json"));

		var result = client.consultar("01001000");

		assertThat(result).isPresent();
		assertThat(result.get().logradouro()).contains("Praça");
		assertThat(result.get().uf()).isEqualTo("SP");
		assertThat(server.getRequestCount()).isEqualTo(1);
	}

	@Test
	void consultarCepComErroTrueRetornaVazio() {
		server.enqueue(new MockResponse()
				.setBody("{\"erro\": true}")
				.addHeader("Content-Type", "application/json"));

		assertThat(client.consultar("99999999")).isEmpty();
	}

	@Test
	void consultarCepFormatoInvalidoNaoChamaRede() {
		assertThat(client.consultar("123")).isEmpty();
		assertThat(server.getRequestCount()).isZero();
	}

	@Test
	void consultarCep404RetornaVazio() {
		server.enqueue(new MockResponse().setResponseCode(404));

		assertThat(client.consultar("01001000")).isEmpty();
	}

	@Test
	void consultarCep429SinalizaFalhaExterna() {
		server.enqueue(new MockResponse().setResponseCode(429));

		assertThatThrownBy(() -> client.consultar("01001000"))
			.isInstanceOf(ExternalIntegrationException.class)
			.hasMessageContaining("429");
	}
}
