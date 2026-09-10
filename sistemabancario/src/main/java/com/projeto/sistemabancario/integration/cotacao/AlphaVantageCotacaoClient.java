package com.projeto.sistemabancario.integration.cotacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.projeto.sistemabancario.exception.RegraNegocioException;
import com.projeto.sistemabancario.integration.config.IntegrationProperties;
import com.projeto.sistemabancario.integration.dto.CotacaoConsultaResult;
import com.projeto.sistemabancario.integration.exception.ExternalIntegrationException;

@Component
class AlphaVantageCotacaoClient {

	private static final Logger log = LoggerFactory.getLogger(AlphaVantageCotacaoClient.class);

	private final RestClient restClient;
	private final String apiKey;

	AlphaVantageCotacaoClient(RestClient.Builder restClientBuilder, IntegrationProperties properties,
			Environment environment) {
		this.restClient = restClientBuilder.baseUrl(properties.getAlphaVantage().getBaseUrl()).build();
		this.apiKey = resolveApiKey(properties, environment);
		if (this.apiKey == null) {
			log.warn(
					"Chave Alpha Vantage ausente. Defina ALPHAVANTAGE_API_KEY, INTEGRATION_ALPHA_VANTAGE_API_KEY ou integration.alpha-vantage.api-key (grátis em https://www.alphavantage.co/support/#api-key).");
		}
		else if ("demo".equalsIgnoreCase(this.apiKey.trim())) {
			log.warn(
					"Alpha Vantage usando api-key \"demo\" (limite baixo; muitos tickers exigem chave própria). Obtenha ALPHAVANTAGE_API_KEY.");
		}
	}

	private static String resolveApiKey(IntegrationProperties properties, Environment environment) {
		String k = normalizeKey(properties.getAlphaVantage().getApiKey());
		if (k != null) {
			return k;
		}
		k = normalizeKey(environment.getProperty("ALPHAVANTAGE_API_KEY"));
		if (k != null) {
			return k;
		}
		k = normalizeKey(environment.getProperty("INTEGRATION_ALPHA_VANTAGE_API_KEY"));
		if (k != null) {
			return k;
		}
		k = normalizeKey(System.getenv("ALPHAVANTAGE_API_KEY"));
		if (k != null) {
			return k;
		}
		return normalizeKey(System.getenv("INTEGRATION_ALPHA_VANTAGE_API_KEY"));
	}

	private static String normalizeKey(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	Optional<CotacaoConsultaResult> buscar(String tickerNormalizado) {
		if (apiKey == null) {
			throw new RegraNegocioException(
					"Cotação em bolsa americana exige chave da Alpha Vantage. Defina a variável de ambiente ALPHAVANTAGE_API_KEY "
							+ "(ou INTEGRATION_ALPHA_VANTAGE_API_KEY / integration.alpha-vantage.api-key no application.properties). "
							+ "Cadastro gratuito: https://www.alphavantage.co/support/#api-key");
		}
		try {
			AlphaVantageQuoteResponse body = restClient.get()
				.uri(uriBuilder -> uriBuilder.path("/query")
					.queryParam("function", "GLOBAL_QUOTE")
					.queryParam("symbol", tickerNormalizado)
					.queryParam("apikey", apiKey)
					.build())
				.retrieve()
				.body(AlphaVantageQuoteResponse.class);
			if (body == null) {
				return Optional.empty();
			}
			throwIfAlphaVantageSinalizaProblemaDeChaveOuLimite(body);
			if (body.globalQuote() == null || body.globalQuote().isEmpty()) {
				return Optional.empty();
			}
			Map<String, String> q = body.globalQuote();
			String priceStr = q.get("05. price");
			if (priceStr == null || priceStr.isBlank()) {
				return Optional.empty();
			}
			BigDecimal preco = new BigDecimal(priceStr.trim()).setScale(6, RoundingMode.HALF_UP);
			String nome = Optional.ofNullable(q.get("01. symbol")).orElse(tickerNormalizado);
			Instant instant = parseQuoteInstant(q.get("07. latest trading day"));
			String moeda = Optional.ofNullable(q.get("08. currency")).orElse("USD");
			return Optional.of(new CotacaoConsultaResult(tickerNormalizado, nome, moeda, preco, instant));
		}
		catch (RestClientResponseException ex) {
			int status = ex.getStatusCode().value();
			if (status == 429) {
				throw new ExternalIntegrationException("Alpha Vantage atingiu o limite de requisições (HTTP 429)", ex);
			}
			if (status == 401 || status == 403) {
				throw new RegraNegocioException(
						"Alpha Vantage recusou a requisição (HTTP " + status + "). Verifique ALPHAVANTAGE_API_KEY / integration.alpha-vantage.api-key.");
			}
			if (ex.getStatusCode().is4xxClientError()) {
				return Optional.empty();
			}
			throw new ExternalIntegrationException("Falha ao consultar cotação na Alpha Vantage", ex);
		}
		catch (RegraNegocioException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new ExternalIntegrationException("Erro inesperado ao consultar cotação na Alpha Vantage", ex);
		}
	}

	/**
	 * A Alpha Vantage frequentemente responde HTTP 200 com campos {@code Error Message}, {@code Note} ou
	 * {@code Information} quando a chave é inválida, o ticker não existe ou o limite de requisições foi excedido.
	 */
	private static void throwIfAlphaVantageSinalizaProblemaDeChaveOuLimite(AlphaVantageQuoteResponse body) {
		boolean semCotacao = body.globalQuote() == null || body.globalQuote().isEmpty();
		if (!semCotacao) {
			return;
		}
		String err = trimToNull(body.errorMessage());
		if (err != null) {
			String lower = err.toLowerCase();
			if (lower.contains("api key") || (lower.contains("parameter") && lower.contains("invalid"))) {
				throw new RegraNegocioException(
						"Alpha Vantage: " + err + " Verifique ALPHAVANTAGE_API_KEY / integration.alpha-vantage.api-key.");
			}
			throw new RegraNegocioException("Alpha Vantage: " + err);
		}
		String note = trimToNull(body.note());
		if (note != null) {
			throw new RegraNegocioException(
					"Alpha Vantage (limite ou indisponibilidade): " + note + " Aguarde ou use uma chave própria.");
		}
		String info = trimToNull(body.information());
		if (info != null) {
			throw new RegraNegocioException(
					"Alpha Vantage (limite ou indisponibilidade): " + info + " Aguarde ou use uma chave própria.");
		}
	}

	private static String trimToNull(String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}

	private static Instant parseQuoteInstant(String latestTradingDay) {
		if (latestTradingDay == null || latestTradingDay.isBlank()) {
			return Instant.now();
		}
		try {
			LocalDate d = LocalDate.parse(latestTradingDay.trim());
			return d.atStartOfDay().toInstant(ZoneOffset.UTC);
		}
		catch (Exception ignored) {
			return Instant.now();
		}
	}
}
