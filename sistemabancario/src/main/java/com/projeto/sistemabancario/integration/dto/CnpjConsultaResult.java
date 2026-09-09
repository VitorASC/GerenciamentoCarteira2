package com.projeto.sistemabancario.integration.dto;

public record CnpjConsultaResult(
		String cnpj,
		String razaoSocial,
		String nomeFantasia,
		String email,
		String telefone,
		String situacaoCadastral,
		String cep,
		String logradouro,
		String numero,
		String complemento,
		String bairro,
		String cidade,
		String uf) {
}
