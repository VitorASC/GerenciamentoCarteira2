package com.projeto.sistemabancario.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties("perfilAcesso")
public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {
	public LoginRequest {
		email = email == null ? null : email.trim();
	}
}
