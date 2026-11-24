package com.projeto.sistemabancario.dto.response;

import com.projeto.sistemabancario.domains.enums.PerfilAcesso;

public record LoginResponse(String tokenType, String accessToken, String refreshToken, long expiresInMs,
		PerfilAcesso perfilAcesso) {

	public long getExpiresIn() {
		return expiresInMs / 1000;
	}

	@Override
	public String toString() {
		return "LoginResponse[protegido]";
	}
}
