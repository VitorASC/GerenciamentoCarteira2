package com.projeto.sistemabancario.domains.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UsuarioTest {

	@Test
	void desabilitaAContaAoInativarEVoltaAHabilitarAoReativar() {
		Usuario usuario = new Usuario();
		assertThat(usuario.getEstadoUsuario().isContaHabilitada()).isTrue();

		usuario.setEstadoUsuario(usuario.getEstadoUsuario().inativar());

		assertThat(usuario.getAtivo()).isFalse();
		assertThat(usuario.getEstadoUsuario().isContaHabilitada()).isFalse();

		usuario.setEstadoUsuario(usuario.getEstadoUsuario().ativar());

		assertThat(usuario.getAtivo()).isTrue();
		assertThat(usuario.getEstadoUsuario().isContaHabilitada()).isTrue();
	}
}
