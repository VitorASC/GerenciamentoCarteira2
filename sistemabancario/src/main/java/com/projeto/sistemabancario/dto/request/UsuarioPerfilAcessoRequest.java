package com.projeto.sistemabancario.dto.request;

import com.projeto.sistemabancario.domains.enums.PerfilAcesso;

import jakarta.validation.constraints.NotNull;

public record UsuarioPerfilAcessoRequest(@NotNull PerfilAcesso perfilAcesso) {
}
