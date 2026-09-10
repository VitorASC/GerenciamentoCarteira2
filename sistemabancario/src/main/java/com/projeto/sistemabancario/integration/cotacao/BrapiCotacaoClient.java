package com.projeto.sistemabancario.integration.cotacao;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.projeto.sistemabancario.exception.RegraNegocioException;
import com.projeto.sistemabancario.integration.config.IntegrationProperties;
import com.projeto.sistemabancario.integration.dto.CotacaoConsultaResult;
import com.projeto.sistemabancario.integration.exception.ExternalIntegrationException;

@Component
class BrapiCotacaoClient {

	private static final Logger log = LoggerFactory.getLogger(BrapiCotacaoClient.class);

	/** Documentação BRAPI: apenas estes papéis respondem sem token. */
	private static final Set<String> TICKERS_BRAPI_SEM_TOKEN_OBRIGATORIO = Set.of("PETR4", "MGLU3", "VALE3", "ITUB4");

	private final RestClient restClient;
	private final String token;

	BrapiCotacaoClient(RestClient.Builder restClientBuilder, IntegrationProperties properties, Environment environment) {
		this.restClient = restClientBuilder.baseUrl(properties.getBrapi().getBaseUrl()).build();
		this.token = resolveToken(properties, environment);
		if (this.token == null) {
			log.warn(
					"Token BRAPI ausente na inicialização da JVM. Sem token, só PETR4, VALE3, MGLU3 e ITUB4. "
							+ "Defina BRAPI_TOKEN ou INTEGRATION_BRAPI_TOKEN e reinicie o processo Java (feche o IDE/terminal ou faça logoff no Windows após alterar variáveis de sistema).");
		}
		else {
			log.info("Token BRAPI carregado ({} caracteres).", this.token.length());
		}
	}

	/**
	 * Lê o token de várias fontes: propriedade resolvida, nomes explícitos de env e getenv (útil se o binding
	 * divergir do esperado no SO).
	 */
	private static String resolveToken(IntegrationProperties properties, Environment environment) {
		String t = normalizeToken(properties.getBrapi().getToken());
		if (t != null) {
			return t;
		}
		t = normalizeToken(environment.getProperty("BRAPI_TOKEN"));
		if (t != null) {
			return t;
		}
		t = normalizeToken(environment.getProperty("INTEGRATION_BRAPI_TOKEN"));
		if (t != null) {
			return t;
		}
		t = normalizeToken(System.getenv("BRAPI_TOKEN"));
		if (t != null) {
			return t;
		}
		return normalizeToken(System.getenv("INTEGRATION_BRAPI_TOKEN"));
	}

	private static String normalizeToken(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	Optional<CotacaoConsultaResult> buscar(String tickerNormalizado) {
		if (token == null && !TICKERS_BRAPI_SEM_TOKEN_OBRIGATORIO.contains(tickerNormalizado)) {
			throw new RegraNegocioException(
					"Cotação na BRAPI para o ticker " + tickerNormalizado
							+ " exige token. Sem autenticação, só são permitidos os papéis de teste: PETR4, VALE3, MGLU3 e ITUB4. "
							+ "Crie uma chave em https://brapi.dev/dashboard e defina BRAPI_TOKEN, INTEGRATION_BRAPI_TOKEN ou integration.brapi.token.");
		}
		try {
			var req = restClient.get()
				.uri(uriBuilder -> {
					var b = uriBuilder.path("/quote/{ticker}");
					if (token != null) {
						b.queryParam("token", token);
					}
					return b.build(tickerNormalizado);
				});
			BrapiQuoteEnvelope body = req.retrieve().body(BrapiQuoteEnvelope.class);
			if (body == null) {
				return Optional.empty();
			}
			if (Boolean.TRUE.equals(body.error) || (body.message != null && !body.message.isBlank())) {
				String msg = body.message != null ? body.message.trim() : "falha na consulta de cotação";
				int code = body.code != null ? body.code : 0;
				if (code == 401 || msg.toLowerCase().contains("unauthorized") || msg.toLowerCase().contains("token")) {
					throw new RegraNegocioException(
							"BRAPI: " + msg + " Verifique BRAPI_TOKEN / INTEGRATION_BRAPI_TOKEN / integration.brapi.token.");
				}
				if (code == 402 || msg.toLowerCase().contains("limit")) {
					throw new RegraNegocioException("BRAPI (limite do plano): " + msg);
				}
				throw new RegraNegocioException("BRAPI: " + msg);
			}
			if (body.results == null || body.results.isEmpty()) {
				return Optional.empty();
			}
			BrapiQuoteItem item = body.results.get(0);
			if (item.regularMarketPrice == null) {
				return Optional.empty();
			}
			BigDecimal preco = BigDecimal.valueOf(item.regularMarketPrice).setScale(6, RoundingMode.HALF_UP);
			Instant instant = item.regularMarketTime != null ? item.regularMarketTime : Instant.now();
			String moeda = item.currency != null ? item.currency : "BRL";
			String nome = item.shortName != null ? item.shortName : item.longName != null ? item.longName : tickerNormalizado;
			return Optional.of(new CotacaoConsultaResult(tickerNormalizado, nome, moeda, preco, instant));
		}
		catch (RestClientResponseException ex) {
			int status = ex.getStatusCode().value();
			if (status == 429) {
				throw new ExternalIntegrationException("BRAPI atingiu o limite de requisições (HTTP 429)", ex);
			}
			if (status == 401 || status == 403) {
				boolean autenticado = token != null;
				if (!autenticado) {
					throw new RegraNegocioException(
							"Cotação na BRAPI exige token para este ativo. Sem token, só são liberados: PETR4, VALE3, MGLU3 e ITUB4. "
									+ "Para BBAS3, BBDC4 e demais papéis da B3, crie uma conta em https://brapi.dev/dashboard, gere uma chave "
									+ "e defina BRAPI_TOKEN, INTEGRATION_BRAPI_TOKEN ou integration.brapi.token.");
				}
				throw new RegraNegocioException(
						"A BRAPI recusou a autenticação (HTTP " + status + "). Verifique se BRAPI_TOKEN / INTEGRATION_BRAPI_TOKEN / integration.brapi.token está correto.");
			}
			if (status == 402) {
				throw new RegraNegocioException(
						"BRAPI retornou HTTP 402 (limite do plano ou pagamento). Verifique seu plano em https://brapi.dev/dashboard ou aguarde a renovação do limite.");
			}
			if (ex.getStatusCode().is4xxClientError()) {
				return Optional.empty();
			}
			throw new ExternalIntegrationException("Falha ao consultar cotação na BRAPI", ex);
		}
		catch (RegraNegocioException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new ExternalIntegrationException("Erro inesperado ao consultar cotação na BRAPI", ex);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static final class BrapiQuoteEnvelope {
		public Boolean error;
		public String message;
		public Integer code;
		public List<BrapiQuoteItem> results;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static final class BrapiQuoteItem {
		public String symbol;
		public String shortName;
		public String longName;
		public String currency;
		public Double regularMarketPrice;

		@JsonDeserialize(using = BrapiMarketTimeDeserializer.class)
		public Instant regularMarketTime;
	}

	private static final class BrapiMarketTimeDeserializer extends JsonDeserializer<Instant> {

		@Override
		public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			JsonToken token = p.currentToken();
			if (token == JsonToken.VALUE_NULL) {
				return null;
			}
			if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
				return Instant.ofEpochSecond(p.getLongValue());
			}
			if (token == JsonToken.VALUE_STRING) {
				String s = p.getText().trim();
				if (s.isEmpty()) {
					return null;
				}
				try {
					return Instant.parse(s);
				}
				catch (DateTimeParseException ex) {
					return Instant.ofEpochSecond(Long.parseLong(s));
				}
			}
			return null;
		}
	}
}
