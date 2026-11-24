package com.projeto.sistemabancario.dto.request;

public record RefreshTokenRequest(String refreshToken) {
	@Override
	public String toString() {
		return "RefreshTokenRequest[protegido]";
	}
}
