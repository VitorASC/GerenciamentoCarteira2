package com.projeto.sistemabancario.integration.cnpj;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record BrasilApiCnpjResponse(
		String cnpj,
		@JsonProperty("razao_social") String razaoSocial,
		@JsonProperty("nome_fantasia") String nomeFantasia,
		String email,
		String telefone,
		@JsonProperty("ddd_telefone_1") String dddTelefone1,
		@JsonProperty("descricao_situacao_cadastral") String descricaoSituacaoCadastral,
		String cep,
		String logradouro,
		String numero,
		String complemento,
		String bairro,
		String municipio,
		String uf) {
}
