package com.projeto.sistemabancario.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CorretoraCadastroRequest(
		@NotBlank(message = "CNPJ é obrigatório") String cnpj,
		@NotBlank(message = "CEP é obrigatório") String cep,
		String numero,
		String complemento) {
}
