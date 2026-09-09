package com.projeto.sistemabancario.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.projeto.sistemabancario.domains.enums.Mercado;

public record AcaoResponse(
		Long id,
		String ticker,
		String nomeEmpresa,
		Mercado mercado,
		String moeda,
		BigDecimal cotacaoAtual,
		LocalDateTime dataHoraCotacao,
		Long corretoraId) {
}
