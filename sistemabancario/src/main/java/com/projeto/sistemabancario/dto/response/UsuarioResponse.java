package com.projeto.sistemabancario.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.projeto.sistemabancario.domains.enums.PerfilInvestidor;
import com.projeto.sistemabancario.domains.enums.PerfilAcesso;

public record UsuarioResponse(
		Long id,
		String nomeCompleto,
		String cpf,
		String email,
		LocalDate dataNascimento,
		PerfilInvestidor perfilInvestidor,
		PerfilAcesso perfilAcesso,
		LocalDateTime dataCriacao,
		Boolean ativo) {
}
