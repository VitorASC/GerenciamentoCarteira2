package com.projeto.sistemabancario.integration.cotacao;

import java.util.Optional;

import com.projeto.sistemabancario.integration.dto.CotacaoConsultaResult;
import com.projeto.sistemabancario.domains.enums.Mercado;

public interface CotacaoConsultationPort {

	Optional<CotacaoConsultaResult> buscar(Mercado mercado, String ticker);

	Optional<CotacaoConsultaResult> buscarAtualizada(Mercado mercado, String ticker);
}
