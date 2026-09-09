package com.projeto.sistemabancario.integration.cnpj;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.projeto.sistemabancario.integration.config.IntegrationProperties;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class BrasilApiCnpjClientMockWebServerTest {

	private MockWebServer server;
	private BrasilApiCnpjClient client;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();
		IntegrationProperties properties = new IntegrationProperties();
		properties.getBrasilApi().setBaseUrl("http://localhost:" + server.getPort());
		client = new BrasilApiCnpjClient(RestClient.builder(), properties);
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	@Test
	void consultarCnpjValidoRetornaDados() {
		server.enqueue(new MockResponse()
				.setBody(
						"""
								{
								  "cnpj": "12.345.678/0001-95",
								  "razao_social": "ACME Serviços Ltda",
								  "nome_fantasia": "ACME",
								  "email": "contato@acme.test",
								  "telefone": "11999999999",
								  "descricao_situacao_cadastral": "ATIVA",
								  "cep": "01310-100",
								  "logradouro": "Av Paulista",
								  "numero": "1000",
								  "complemento": "Sala 1",
								  "bairro": "Bela Vista",
								  "municipio": "São Paulo",
								  "uf": "SP"
								}
								""")
				.addHeader("Content-Type", "application/json"));

		var result = client.consultar("12.345.678/0001-95");

		assertThat(result).isPresent();
		assertThat(result.get().razaoSocial()).contains("ACME");
		assertThat(result.get().uf()).isEqualTo("SP");
	}

	@Test
	void consultarCnpjTamanhoInvalidoNaoChamaRede() {
		assertThat(client.consultar("123")).isEmpty();
		assertThat(server.getRequestCount()).isZero();
	}

	@Test
	void consultarCnpj404RetornaVazio() {
		server.enqueue(new MockResponse().setResponseCode(404));

		assertThat(client.consultar("12345678000195")).isEmpty();
	}
}
