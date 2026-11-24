package com.projeto.sistemabancario.integration.cvm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.projeto.sistemabancario.integration.config.IntegrationProperties;
import com.projeto.sistemabancario.integration.exception.ExternalIntegrationException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;

class CvmCadastroCorretoraClientMockWebServerTest {

	@Test
	void sinalizaZipSemCadastroEConsultaNovamenteQuandoOArquivoVoltaAoNormal() throws IOException {
		String cnpjFicticio = "00000000000000";
		String csv = "TP_PARTIC;CNPJ;SIT\nCORRETORAS;" + cnpjFicticio + ";EM FUNCIONAMENTO NORMAL\n";
		try (MockWebServer servidor = new MockWebServer()) {
			servidor.start();
			servidor.enqueue(respostaZip("leia-me.txt", "Cadastro indisponível"));
			servidor.enqueue(respostaZip("dados/cad_intermed.csv", csv));
			IntegrationProperties propriedades = new IntegrationProperties();
			propriedades.getCvm().setCadastroZipUrl(servidor.url("/cad_intermed.zip").toString());
			var cliente = new CvmCadastroCorretoraClient(RestClient.builder(), propriedades);

			assertThatThrownBy(() -> cliente.validar(cnpjFicticio))
					.isInstanceOf(ExternalIntegrationException.class)
					.hasMessageContaining("cad_intermed.csv");

			var resultado = cliente.validar(cnpjFicticio);

			assertThat(resultado.encontradoNoCadastroCvmComoCorretora()).isTrue();
			assertThat(resultado.emFuncionamentoNormal()).isTrue();
			assertThat(servidor.getRequestCount()).isEqualTo(2);
		}
	}

	private static MockResponse respostaZip(String nomeArquivo, String conteudo) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
			zip.putNextEntry(new ZipEntry(nomeArquivo));
			zip.write(conteudo.getBytes(StandardCharsets.ISO_8859_1));
			zip.closeEntry();
		}
		return new MockResponse().setHeader("Content-Type", "application/zip")
				.setBody(new Buffer().write(bytes.toByteArray()));
	}
}
