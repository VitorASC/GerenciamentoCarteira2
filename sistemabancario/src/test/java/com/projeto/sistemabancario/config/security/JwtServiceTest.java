package com.projeto.sistemabancario.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

	@Test
	void gerarEValidarTokenPreservaSubjectEmailEAtivo() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("chave-de-teste-com-pelo-menos-trinta-e-dois-bytes!!");
		properties.setExpirationMs(120_000L);
		JwtService jwtService = new JwtService(properties);

		String token = jwtService.gerarToken(99L, "user@test.local", true);
		var claims = jwtService.parseClaims(token);

		assertThat(claims.getSubject()).isEqualTo("99");
		assertThat(claims.get("email", String.class)).isEqualTo("user@test.local");
		assertThat(claims.get("ativo", Boolean.class)).isTrue();
	}

	@Test
	void rejeitaSecretHmacComMenosDe256BitsNaInicializacao() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("curta");

		assertThatThrownBy(() -> new JwtService(properties))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("APP_SECURITY_JWT_SECRET")
			.hasMessageContaining("32 bytes");
	}
}
