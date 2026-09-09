package com.projeto.sistemabancario.dto.response;

import java.time.OffsetDateTime;

public record ErroResponse(int status, String erro, String mensagem, OffsetDateTime timestamp, String path) {
}
