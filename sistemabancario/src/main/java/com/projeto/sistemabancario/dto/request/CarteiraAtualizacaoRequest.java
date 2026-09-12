package com.projeto.sistemabancario.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.PositiveOrZero;

public record CarteiraAtualizacaoRequest(
		String nomeDaCarteira,
		Long corretoraId,
		@PositiveOrZero BigDecimal saldoInicial) {
}
