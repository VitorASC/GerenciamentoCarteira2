package com.projeto.sistemabancario.dto.response;

import java.math.BigDecimal;

public record PosicaoIndicadorMercadoResponse(
		String ticker,
		BigDecimal quantidade,
		BigDecimal precoMedioPonderado,
		BigDecimal cotacaoMercadoAtual,
		BigDecimal valorCusto,
		BigDecimal valorMercado) {
}
