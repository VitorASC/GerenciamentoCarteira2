package com.projeto.sistemabancario.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration")
public class IntegrationProperties {

	private BrasilApi brasilApi = new BrasilApi();
	private ViaCep viaCep = new ViaCep();
	private Brapi brapi = new Brapi();
	private AlphaVantage alphaVantage = new AlphaVantage();
	private Cvm cvm = new Cvm();

	public BrasilApi getBrasilApi() {
		return brasilApi;
	}

	public void setBrasilApi(BrasilApi brasilApi) {
		this.brasilApi = brasilApi;
	}

	public ViaCep getViaCep() {
		return viaCep;
	}

	public void setViaCep(ViaCep viaCep) {
		this.viaCep = viaCep;
	}

	public Brapi getBrapi() {
		return brapi;
	}

	public void setBrapi(Brapi brapi) {
		this.brapi = brapi;
	}

	public AlphaVantage getAlphaVantage() {
		return alphaVantage;
	}

	public void setAlphaVantage(AlphaVantage alphaVantage) {
		this.alphaVantage = alphaVantage;
	}

	public Cvm getCvm() {
		return cvm;
	}

	public void setCvm(Cvm cvm) {
		this.cvm = cvm;
	}

	public static class BrasilApi {

		private String baseUrl = "https://brasilapi.com.br/api";

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}
	}

	public static class ViaCep {

		private String baseUrl = "https://viacep.com.br";

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}
	}

	public static class Brapi {

		private String baseUrl = "https://brapi.dev/api";

		/** Opcional: https://brapi.dev/ — limite maior com token */
		private String token = "";

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}
	}

	public static class AlphaVantage {

		private String baseUrl = "https://www.alphavantage.co";

		/** Vazio até definir via application / ALPHAVANTAGE_API_KEY; use "demo" só para testes limitados. */
		private String apiKey = "";

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}
	}

	public static class Cvm {

		/** ZIP diário com cad_intermed.csv — Portal Dados Abertos CVM */
		private String cadastroZipUrl = "https://dados.cvm.gov.br/dados/INTERMED/CAD/DADOS/cad_intermed.zip";

		public String getCadastroZipUrl() {
			return cadastroZipUrl;
		}

		public void setCadastroZipUrl(String cadastroZipUrl) {
			this.cadastroZipUrl = cadastroZipUrl;
		}
	}
}
