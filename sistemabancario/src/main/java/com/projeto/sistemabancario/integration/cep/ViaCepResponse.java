package com.projeto.sistemabancario.integration.cep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record ViaCepResponse(String cep, String logradouro, String complemento, String bairro, String localidade,
		String uf, Object erro) {

	boolean isErroResposta() {
		if (erro == null) {
			return false;
		}
		if (erro instanceof Boolean b) {
			return b;
		}
		if (erro instanceof String s) {
			return "true".equalsIgnoreCase(s);
		}
		return false;
	}
}
