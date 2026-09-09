package com.projeto.sistemabancario.integration.dto;

public record CepConsultaResult(
		String cep,
		String logradouro,
		String complemento,
		String bairro,
		String cidade,
		String uf) {
}
