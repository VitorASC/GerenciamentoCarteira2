package com.projeto.sistemabancario.dto.response;

public record LoginResponse(String tokenType, String accessToken, long expiresInMs) {
}
