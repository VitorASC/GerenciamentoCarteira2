package com.projeto.sistemabancario.domains.usuario.state;

public final class EstadoUsuarioInativo implements EstadoUsuario {

	public static final EstadoUsuarioInativo INSTANCE = new EstadoUsuarioInativo();

	private EstadoUsuarioInativo() {
	}

	@Override
	public boolean isContaHabilitada() {
		return false;
	}

	@Override
	public EstadoUsuario ativar() {
		return EstadoUsuarioAtivo.INSTANCE;
	}

	@Override
	public EstadoUsuario inativar() {
		return INSTANCE;
	}
}
