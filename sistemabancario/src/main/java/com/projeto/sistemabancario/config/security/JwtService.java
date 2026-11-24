package com.projeto.sistemabancario.config.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.projeto.sistemabancario.domains.enums.PerfilAcesso;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;

@Service
public class JwtService {

	private static final int MINIMUM_HMAC_KEY_BYTES = 32;

	private final JwtProperties properties;
	private final SecretKey signingKey;
	private final JwtParser parser;

	private final Clock clock;

	public JwtService(JwtProperties properties, Clock clock) {
		this.clock = clock;
		this.properties = properties;
		String secret = properties.getSecret();
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("APP_SECURITY_JWT_SECRET deve ser definida");
		}
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < MINIMUM_HMAC_KEY_BYTES) {
			throw new IllegalStateException("APP_SECURITY_JWT_SECRET deve possuir pelo menos 32 bytes (256 bits)");
		}
		if (properties.getIssuer() == null || properties.getIssuer().isBlank()) {
			throw new IllegalStateException("APP_SECURITY_JWT_ISSUER deve ser definido");
		}
		if (properties.getExpirationMs() < 1_000) {
			throw new IllegalStateException("APP_SECURITY_JWT_EXPIRATION_MS deve ser de pelo menos 1000 ms");
		}
		this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		var algoritmos = Jwts.parser()
			.clock(() -> Date.from(clock.instant()))
			.verifyWith(signingKey)
			.requireIssuer(properties.getIssuer())
			.sig();
		for (var algoritmo : Jwts.SIG.get().values()) {
			if (!Jwts.SIG.HS256.equals(algoritmo)) {
				algoritmos.remove(algoritmo);
			}
		}
		this.parser = algoritmos.and().build();
	}

	public String gerarToken(Long usuarioId, String email, boolean ativo, PerfilAcesso perfilAcesso) {
		return gerarToken(usuarioId, email, ativo, perfilAcesso, UUID.randomUUID());
	}

	public String gerarToken(Long usuarioId, String email, boolean ativo, PerfilAcesso perfilAcesso, UUID familiaId) {
		var agora = clock.instant();
		Date issuedAt = Date.from(agora);
		Date expiresAt = Date.from(agora.plusMillis(properties.getExpirationMs()));
		return Jwts.builder()
			.issuer(properties.getIssuer())
			.subject(String.valueOf(usuarioId))
			.id(UUID.randomUUID().toString())
			.claim("sid", familiaId.toString())
			.claim("email", email)
			.claim("ativo", ativo)
			.claim("perfilAcesso", perfilAcesso.name())
			.issuedAt(issuedAt)
			.expiration(expiresAt)
			.signWith(signingKey, Jwts.SIG.HS256)
			.compact();
	}

	public String gerarToken(Long usuarioId, String email, boolean ativo) {
		return gerarToken(usuarioId, email, ativo, PerfilAcesso.PADRAO);
	}

	public Claims parseClaims(String token) {
		Claims claims = parser.parseSignedClaims(token).getPayload();
		if (claims.getSubject() == null || claims.getSubject().isBlank()
				|| claims.getId() == null || claims.getId().isBlank()
				|| claims.getIssuedAt() == null || claims.getExpiration() == null
				|| !claims.getExpiration().after(claims.getIssuedAt())
				|| !clock.instant().isBefore(claims.getExpiration().toInstant())
				|| claims.get("perfilAcesso", String.class) == null) {
			throw new MalformedJwtException("Claims obrigatórias do JWT inválidas ou ausentes");
		}
		return claims;
	}
}
