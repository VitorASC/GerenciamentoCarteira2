package com.projeto.sistemabancario.dto.response;

import com.projeto.sistemabancario.domains.enums.PerfilAcesso;

public record LoginResponse(String tokenType, String accessToken, long expiresInMs, PerfilAcesso perfilAcesso) {
}
