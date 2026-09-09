package com.projeto.sistemabancario.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IndicadorTickerCarteiraResponse(
		Long carteiraId,
		String ticker,
		BigDecimal quantidade,
		BigDecimal precoMedioPonderado,
		BigDecimal cotacaoMercadoAtual,
		LocalDateTime dataHoraCotacaoMercado,
		String moeda,
		BigDecimal valorCustoTotal,
		BigDecimal valorMercadoAtual) {
}
