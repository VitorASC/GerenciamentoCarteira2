package com.projeto.sistemabancario.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record IndicadorMediaCarteiraResponse(
		Long carteiraId,
		int quantidadePosicoes,
		BigDecimal quantidadeTotalTitulos,
		BigDecimal custoTotalCarteira,
		BigDecimal valorMercadoTotalCarteira,
		/** Custo total atual dividido pela quantidade total de títulos. */
		BigDecimal mediaPrecoMedioPonderado,
		/** Σ(valor de mercado de cada posição) ÷ Σ(quantidade de papéis). Equivale a média ponderada das cotações pela quantidade. */
		BigDecimal mediaValorMercadoPorTitulo,
		/** ((valor de mercado atual - custo atual) / custo atual) × 100. */
		BigDecimal rentabilidadeNaoRealizadaPercentual,
		List<PosicaoIndicadorMercadoResponse> posicoes) {
}
