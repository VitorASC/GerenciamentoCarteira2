package com.projeto.sistemabancario.config.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private static final int MINIMUM_HMAC_KEY_BYTES = 32;

	private final JwtProperties properties;
	private final SecretKey signingKey;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		String secret = properties.getSecret();
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("APP_SECURITY_JWT_SECRET deve ser definida");
		}
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < MINIMUM_HMAC_KEY_BYTES) {
			throw new IllegalStateException("APP_SECURITY_JWT_SECRET deve possuir pelo menos 32 bytes (256 bits)");
		}
		this.signingKey = Keys.hmacShaKeyFor(keyBytes);
	}

	public String gerarToken(Long usuarioId, String email, boolean ativo) {
		long now = System.currentTimeMillis();
		Date issuedAt = new Date(now);
		Date expiresAt = new Date(now + properties.getExpirationMs());
		return Jwts.builder()
			.subject(String.valueOf(usuarioId))
			.claim("email", email)
			.claim("ativo", ativo)
			.issuedAt(issuedAt)
			.expiration(expiresAt)
			.signWith(signingKey)
			.compact();
	}

	public Claims parseClaims(String token) {
		return Jwts.parser()
			.verifyWith(signingKey)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}
}
