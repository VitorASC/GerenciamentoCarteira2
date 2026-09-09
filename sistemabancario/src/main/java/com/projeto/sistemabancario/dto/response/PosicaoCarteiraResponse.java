package com.projeto.sistemabancario.dto.response;

import java.math.BigDecimal;

public record PosicaoCarteiraResponse(
		Long acaoId,
		String ticker,
		String nomeEmpresa,
		BigDecimal quantidade,
		BigDecimal precoMedioPonderado,
		BigDecimal cotacaoAtual,
		BigDecimal valorMercadoAtual,
		BigDecimal valorCustoTotal) {
}
