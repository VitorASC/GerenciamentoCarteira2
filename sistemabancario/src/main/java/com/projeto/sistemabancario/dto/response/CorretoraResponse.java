package com.projeto.sistemabancario.dto.response;

import java.time.LocalDateTime;

public record CorretoraResponse(
		Long id,
		String cnpj,
		String razaoSocial,
		String nomeFantasia,
		String email,
		String telefone,
		String cep,
		String logradouro,
		String numero,
		String complemento,
		String bairro,
		String cidade,
		String uf,
		String situacaoCadastral,
		Boolean validadaNaCvm,
		LocalDateTime dataCadastro) {
}
