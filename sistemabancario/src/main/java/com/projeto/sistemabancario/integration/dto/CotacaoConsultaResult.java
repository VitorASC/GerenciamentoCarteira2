package com.projeto.sistemabancario.integration.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CotacaoConsultaResult(
		String tickerNormalizado,
		String nomeEmpresa,
		String moeda,
		BigDecimal preco,
		Instant dataHoraReferencia) {
}
