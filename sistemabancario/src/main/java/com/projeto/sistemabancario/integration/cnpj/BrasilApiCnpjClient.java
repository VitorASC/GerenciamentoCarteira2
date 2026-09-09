package com.projeto.sistemabancario.integration.cnpj;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.projeto.sistemabancario.integration.config.CacheNames;
import com.projeto.sistemabancario.integration.config.IntegrationProperties;
import com.projeto.sistemabancario.integration.dto.CnpjConsultaResult;
import com.projeto.sistemabancario.integration.exception.ExternalIntegrationException;

@Service
public class BrasilApiCnpjClient implements CnpjConsultationPort {

	private final RestClient restClient;

	public BrasilApiCnpjClient(RestClient.Builder restClientBuilder, IntegrationProperties properties) {
		this.restClient = restClientBuilder.baseUrl(properties.getBrasilApi().getBaseUrl()).build();
	}

	@Override
	@Cacheable(cacheNames = CacheNames.CNPJ, key = "#cnpjSomenteDigitos",
			unless = "T(org.springframework.util.ObjectUtils).isEmpty(#result)")
	public Optional<CnpjConsultaResult> consultar(String cnpjSomenteDigitos) {
		String digits = normalizeDigits(cnpjSomenteDigitos);
		if (digits.length() != 14) {
			return Optional.empty();
		}
		try {
			BrasilApiCnpjResponse body = restClient.get()
				.uri("/cnpj/v1/{cnpj}", digits)
				.retrieve()
				.body(BrasilApiCnpjResponse.class);
			if (body == null || body.cnpj() == null || body.razaoSocial() == null) {
				return Optional.empty();
			}
			String telefone = body.telefone() != null && !body.telefone().isBlank()
					? body.telefone()
					: body.dddTelefone1();
			return Optional.of(new CnpjConsultaResult(
					body.cnpj(),
					body.razaoSocial(),
					body.nomeFantasia(),
					body.email(),
					telefone,
					body.descricaoSituacaoCadastral(),
					sanitizeDigits(body.cep()),
					body.logradouro(),
					body.numero(),
					body.complemento(),
					body.bairro(),
					body.municipio(),
					body.uf()));
		}
		catch (RestClientResponseException ex) {
			if (ex.getStatusCode().is4xxClientError()) {
				return Optional.empty();
			}
			throw new ExternalIntegrationException("Falha ao consultar CNPJ na BrasilAPI", ex);
		}
		catch (Exception ex) {
			throw new ExternalIntegrationException("Erro inesperado ao consultar CNPJ na BrasilAPI", ex);
		}
	}

	private static String normalizeDigits(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replaceAll("\\D", "");
	}

	private static String sanitizeDigits(String raw) {
		if (raw == null) {
			return null;
		}
		return raw.replaceAll("\\D", "");
	}
}
