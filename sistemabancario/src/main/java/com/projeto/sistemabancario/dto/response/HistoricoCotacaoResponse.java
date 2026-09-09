package com.projeto.sistemabancario.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricoCotacaoResponse(Long id, BigDecimal valor, LocalDateTime dataHora) {
}
