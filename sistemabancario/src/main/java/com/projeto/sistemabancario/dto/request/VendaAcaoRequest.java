package com.projeto.sistemabancario.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VendaAcaoRequest(
		@NotNull Long acaoId,
		@NotNull @Positive BigDecimal quantidade,
		@NotNull @Positive BigDecimal precoUnitario) {
}
