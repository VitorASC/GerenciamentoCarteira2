package com.projeto.sistemabancario.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.projeto.sistemabancario.domains.enums.PerfilInvestidor;

import jakarta.validation.constraints.Email;

@JsonIgnoreProperties("perfilAcesso")
public record UsuarioAtualizacaoRequest(
		String nomeCompleto,
		@Email String email,
		PerfilInvestidor perfilInvestidor,
		Boolean ativo) {

	public UsuarioAtualizacaoRequest {
		email = email == null ? null : email.trim();
	}
}
