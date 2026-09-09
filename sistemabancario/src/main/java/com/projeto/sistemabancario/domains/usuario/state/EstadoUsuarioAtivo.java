package com.projeto.sistemabancario.domains.usuario.state;

public final class EstadoUsuarioAtivo implements EstadoUsuario {

	public static final EstadoUsuarioAtivo INSTANCE = new EstadoUsuarioAtivo();

	private EstadoUsuarioAtivo() {
	}

	@Override
	public boolean isContaHabilitada() {
		return true;
	}

	@Override
	public EstadoUsuario ativar() {
		return INSTANCE;
	}

	@Override
	public EstadoUsuario inativar() {
		return EstadoUsuarioInativo.INSTANCE;
	}
}
