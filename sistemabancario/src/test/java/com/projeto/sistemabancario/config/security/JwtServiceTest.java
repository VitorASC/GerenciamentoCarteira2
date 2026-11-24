package com.projeto.sistemabancario.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;

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
		String cabecalho = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
		assertThat(cabecalho).contains("\"alg\":\"HS256\"");
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

	@Test
	void rejeitaTokenAssinadoSemExpiracaoEAssinaturaComOutroAlgoritmo() {
		String segredo = "chave-de-teste-com-pelo-menos-quarenta-e-oito-bytes-para-jwt";
		JwtProperties properties = new JwtProperties();
		properties.setSecret(segredo);
		JwtService jwtService = new JwtService(properties);
		byte[] bytesChave = segredo.getBytes(StandardCharsets.UTF_8);
		Instant agora = Instant.now();
		var token = Jwts.builder().issuer(properties.getIssuer()).subject("99").id("teste")
				.issuedAt(Date.from(agora)).claim("perfilAcesso", "PADRAO").claim("ativo", true);
		String semExpiracao = token.signWith(new SecretKeySpec(bytesChave, "HmacSHA256"), Jwts.SIG.HS256).compact();
		String hs384 = token.expiration(Date.from(agora.plusSeconds(60)))
				.signWith(new SecretKeySpec(bytesChave, "HmacSHA384"), Jwts.SIG.HS384).compact();

		assertThatThrownBy(() -> jwtService.parseClaims(semExpiracao)).isInstanceOf(MalformedJwtException.class);
		assertThatThrownBy(() -> jwtService.parseClaims(hs384)).isInstanceOf(JwtException.class);
	}
}
