package com.projeto.sistemabancario.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.projeto.sistemabancario.domains.enums.PerfilInvestidor;

public record UsuarioResponse(
		Long id,
		String nomeCompleto,
		String cpf,
		String email,
		LocalDate dataNascimento,
		PerfilInvestidor perfilInvestidor,
		LocalDateTime dataCriacao,
		Boolean ativo) {
}
