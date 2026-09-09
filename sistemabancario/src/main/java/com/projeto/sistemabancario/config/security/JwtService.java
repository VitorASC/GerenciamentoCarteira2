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

	private final JwtProperties properties;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
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
			.signWith(signingKey())
			.compact();
	}

	public Claims parseClaims(String token) {
		return Jwts.parser()
			.verifyWith(signingKey())
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	private SecretKey signingKey() {
		byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
