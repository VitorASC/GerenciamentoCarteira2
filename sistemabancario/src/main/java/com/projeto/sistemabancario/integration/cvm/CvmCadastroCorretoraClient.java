package com.projeto.sistemabancario.integration.cvm;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.projeto.sistemabancario.integration.config.IntegrationProperties;
import com.projeto.sistemabancario.integration.exception.ExternalIntegrationException;

/**
 * Valida se o CNPJ consta como {@code CORRETORAS} no cadastro público de intermediários da CVM,
 * disponível em {@link IntegrationProperties.Cvm#getCadastroZipUrl()}.
 * <p>
 * Fonte: Portal Dados Abertos CVM — Participantes Intermediários / Informação Cadastral (arquivo
 * {@code cad_intermed.csv} dentro do ZIP, atualização diária).
 */
@Service
public class CvmCadastroCorretoraClient implements CvmCorretoraValidationPort {

	public static final String TIPO_CORRETORAS = "CORRETORAS";
	public static final String SIT_EM_FUNCIONAMENTO = "EM FUNCIONAMENTO NORMAL";

	private static final String CSV_ENTRY_NAME = "cad_intermed.csv";
	private static final Charset CSV_CHARSET = Charset.forName("Windows-1252");

	private final RestClient http;
	private final IntegrationProperties integrationProperties;

	private final Object loadLock = new Object();
	private volatile boolean registryLoaded;
	private volatile Map<String, String> situacaoPorCnpjCorretora = Map.of();

	public CvmCadastroCorretoraClient(RestClient.Builder restClientBuilder, IntegrationProperties integrationProperties) {
		this.http = restClientBuilder.build();
		this.integrationProperties = integrationProperties;
	}

	@Override
	public CvmCorretoraValidationResult validar(String cnpjSomenteDigitos) {
		String cnpj = normalizeDigits(cnpjSomenteDigitos);
		if (cnpj.length() != 14) {
			return CvmCorretoraValidationResult.naoEncontrado();
		}
		ensureLoaded();
		String situacao = situacaoPorCnpjCorretora.get(cnpj);
		if (situacao == null) {
			return CvmCorretoraValidationResult.naoEncontrado();
		}
		boolean ativa = SIT_EM_FUNCIONAMENTO.equalsIgnoreCase(situacao.trim());
		return new CvmCorretoraValidationResult(true, ativa, situacao.trim());
	}

	private void ensureLoaded() {
		if (registryLoaded) {
			return;
		}
		synchronized (loadLock) {
			if (registryLoaded) {
				return;
			}
			situacaoPorCnpjCorretora = downloadAndParse();
			registryLoaded = true;
		}
	}

	private Map<String, String> downloadAndParse() {
		String url = integrationProperties.getCvm().getCadastroZipUrl();
		try {
			byte[] zipBytes = http.get()
				.uri(url)
				.retrieve()
				.body(byte[].class);
			if (zipBytes == null || zipBytes.length == 0) {
				throw new ExternalIntegrationException("Download do cadastro CVM retornou vazio: " + url);
			}
			return parseCadZip(zipBytes);
		}
		catch (RestClientException ex) {
			throw new ExternalIntegrationException(
					"Não foi possível baixar o cadastro de intermediários da CVM (rede, tempo esgotado ou servidor indisponível).",
					ex);
		}
		catch (IOException ex) {
			throw new ExternalIntegrationException("Falha ao ler cadastro de intermediários da CVM", ex);
		}
	}

	private Map<String, String> parseCadZip(byte[] zipBytes) throws IOException {
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (!CSV_ENTRY_NAME.equalsIgnoreCase(simpleName(entry.getName()))) {
					continue;
				}
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(zis, CSV_CHARSET))) {
					return CvmCorretoraCsvParser.parseCorretorasSituacao(reader);
				}
			}
		}
		throw new ExternalIntegrationException("Arquivo " + CSV_ENTRY_NAME + " não encontrado no ZIP da CVM");
	}

	private static String simpleName(String path) {
		if (path == null) {
			return "";
		}
		int i = path.lastIndexOf('/');
		int j = path.lastIndexOf('\\');
		int cut = Math.max(i, j);
		return cut >= 0 ? path.substring(cut + 1) : path;
	}

	private static String normalizeDigits(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replaceAll("\\D", "");
	}
}
