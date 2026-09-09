package com.projeto.sistemabancario.domains.usuario.state;

public interface EstadoUsuario {

	boolean isContaHabilitada();

	EstadoUsuario ativar();

	EstadoUsuario inativar();
}
