package com.projeto.sistemabancario.dto.request;

import com.projeto.sistemabancario.domains.enums.Mercado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AcaoCadastroRequest(
		@NotBlank(message = "Ticker é obrigatório") String ticker,
		@NotNull(message = "Mercado é obrigatório") Mercado mercado,
		Long corretoraId) {
}
