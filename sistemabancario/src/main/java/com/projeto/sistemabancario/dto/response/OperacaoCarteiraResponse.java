package com.projeto.sistemabancario.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.projeto.sistemabancario.domains.enums.TipoTransacao;

public record OperacaoCarteiraResponse(
		Long transacaoId,
		TipoTransacao tipo,
		Long acaoId,
		String ticker,
		BigDecimal quantidade,
		BigDecimal precoUnitario,
		LocalDateTime dataHora,
		BigDecimal saldoCarteiraApos,
		BigDecimal rentabilidadeAcumuladaCarteira) {
}
