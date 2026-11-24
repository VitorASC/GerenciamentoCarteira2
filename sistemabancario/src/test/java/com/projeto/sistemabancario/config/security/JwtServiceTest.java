package com.projeto.sistemabancario.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;

class JwtServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

	@Test
	void gerarEValidarTokenPreservaSubjectEmailEAtivo() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret(UUID.randomUUID().toString());
		properties.setExpirationMs(120_000L);
		JwtService jwtService = new JwtService(properties, CLOCK);

		String token = jwtService.gerarToken(99L, "user@test.local", true);
		var claims = jwtService.parseClaims(token);

		assertThat(claims.getSubject()).isEqualTo("99");
		assertThat(claims.get("email", String.class)).isEqualTo("user@test.local");
		assertThat(claims.get("ativo", Boolean.class)).isTrue();
		String cabecalho = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
		assertThat(cabecalho).contains("\"alg\":\"HS256\"");
	}

	@Test
	void rejeitaSecretHmacComMenosDe256BitsNaInicializacao() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("curta");

		assertThatThrownBy(() -> new JwtService(properties, CLOCK))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("APP_SECURITY_JWT_SECRET")
			.hasMessageContaining("32 bytes");
	}

	@Test
	void rejeitaDuracaoDoAccessNulaEmMilissegundosOuNegativa() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret(UUID.randomUUID().toString());
		for (long invalida : new long[]{0, -1}) {
			properties.setExpirationMs(invalida);
			assertThatThrownBy(() -> new JwtService(properties, CLOCK)).isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("EXPIRATION_MS");
		}
	}

	@Test
	void rejeitaTokenAssinadoSemExpiracaoEAssinaturaComOutroAlgoritmo() {
		String segredo = UUID.randomUUID().toString() + UUID.randomUUID();
		JwtProperties properties = new JwtProperties();
		properties.setSecret(segredo);
		JwtService jwtService = new JwtService(properties, CLOCK);
		byte[] bytesChave = segredo.getBytes(StandardCharsets.UTF_8);
		Instant agora = CLOCK.instant();
		var token = Jwts.builder().issuer(properties.getIssuer()).subject("99").id("teste")
				.issuedAt(Date.from(agora)).claim("perfilAcesso", "PADRAO").claim("ativo", true);
		String semExpiracao = token.signWith(new SecretKeySpec(bytesChave, "HmacSHA256"), Jwts.SIG.HS256).compact();
		String hs384 = token.expiration(Date.from(agora.plusSeconds(60)))
				.signWith(new SecretKeySpec(bytesChave, "HmacSHA384"), Jwts.SIG.HS384).compact();

		assertThatThrownBy(() -> jwtService.parseClaims(semExpiracao)).isInstanceOf(MalformedJwtException.class);
		assertThatThrownBy(() -> jwtService.parseClaims(hs384)).isInstanceOf(JwtException.class);
	}
}
