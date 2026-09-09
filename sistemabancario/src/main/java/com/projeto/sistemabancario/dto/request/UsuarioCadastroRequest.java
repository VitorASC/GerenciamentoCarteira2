package com.projeto.sistemabancario.dto.request;

import java.time.LocalDate;

import com.projeto.sistemabancario.domains.enums.PerfilInvestidor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioCadastroRequest(
		@NotBlank String nomeCompleto,
		@NotBlank @Size(min = 11, max = 11) String cpf,
		@NotBlank @Email String email,
		@NotBlank @Size(min = 6, max = 120) String senha,
		LocalDate dataNascimento,
		@NotNull PerfilInvestidor perfilInvestidor) {
}
