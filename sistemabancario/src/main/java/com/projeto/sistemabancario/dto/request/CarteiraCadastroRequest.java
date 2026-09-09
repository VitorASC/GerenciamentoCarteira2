package com.projeto.sistemabancario.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CarteiraCadastroRequest(
		@NotNull Long usuarioId,
		@NotBlank String nomeDaCarteira,
		Long corretoraId,
		BigDecimal saldoInicial) {
}
