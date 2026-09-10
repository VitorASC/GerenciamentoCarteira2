package com.projeto.sistemabancario.integration.cep;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.projeto.sistemabancario.integration.config.CacheNames;
import com.projeto.sistemabancario.integration.config.IntegrationProperties;
import com.projeto.sistemabancario.integration.dto.CepConsultaResult;
import com.projeto.sistemabancario.integration.exception.ExternalIntegrationException;

@Service
public class ViaCepClient implements CepConsultationPort {

	private final RestClient restClient;

	public ViaCepClient(RestClient.Builder restClientBuilder, IntegrationProperties properties) {
		this.restClient = restClientBuilder.baseUrl(properties.getViaCep().getBaseUrl()).build();
	}

	@Override
	@Cacheable(cacheNames = CacheNames.CEP, key = "#cepSomenteDigitos",
			unless = "T(org.springframework.util.ObjectUtils).isEmpty(#result)")
	public Optional<CepConsultaResult> consultar(String cepSomenteDigitos) {
		String digits = normalizeDigits(cepSomenteDigitos);
		if (digits.length() != 8) {
			return Optional.empty();
		}
		try {
			ViaCepResponse body = restClient.get()
				.uri("/ws/{cep}/json/", digits)
				.retrieve()
				.body(ViaCepResponse.class);
			if (body == null || body.isErroResposta()) {
				return Optional.empty();
			}
			return Optional.of(new CepConsultaResult(
					sanitizeDigits(body.cep()),
					body.logradouro(),
					body.complemento(),
					body.bairro(),
					body.localidade(),
					body.uf()));
		}
		catch (RestClientResponseException ex) {
			if (ex.getStatusCode().value() == 429) {
				throw new ExternalIntegrationException("ViaCEP atingiu o limite de requisições (HTTP 429)", ex);
			}
			if (ex.getStatusCode().is4xxClientError()) {
				return Optional.empty();
			}
			throw new ExternalIntegrationException("Falha ao consultar CEP no ViaCEP", ex);
		}
		catch (Exception ex) {
			throw new ExternalIntegrationException("Erro inesperado ao consultar CEP no ViaCEP", ex);
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
