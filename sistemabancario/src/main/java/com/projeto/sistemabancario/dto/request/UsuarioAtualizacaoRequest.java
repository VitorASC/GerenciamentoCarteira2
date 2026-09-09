package com.projeto.sistemabancario.dto.request;

import com.projeto.sistemabancario.domains.enums.PerfilInvestidor;

import jakarta.validation.constraints.Email;

public record UsuarioAtualizacaoRequest(
		String nomeCompleto,
		@Email String email,
		PerfilInvestidor perfilInvestidor,
		Boolean ativo) {
}
