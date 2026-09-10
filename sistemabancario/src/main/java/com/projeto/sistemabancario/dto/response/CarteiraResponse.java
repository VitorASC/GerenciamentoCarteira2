package com.projeto.sistemabancario.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CarteiraResponse(
		Long id,
		Long usuarioId,
		String nomeDaCarteira,
		BigDecimal saldoTotal,
		BigDecimal lucroPrejuizoRealizado,
		Long corretoraId,
		LocalDateTime dataCriacao,
		List<PosicaoCarteiraResponse> posicoes) {
}
