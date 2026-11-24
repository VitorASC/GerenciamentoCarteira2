package com.projeto.sistemabancario.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

	/**
	 * Segredo HMAC (HS256). Deve ter pelo menos 256 bits (32 caracteres ASCII).
	 */
	private String secret = "";

	private String issuer = "gestaoativa";
	private long expirationMs = 1_800_000L;

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public long getExpirationMs() {
		return expirationMs;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public void setExpirationMs(long expirationMs) {
		this.expirationMs = expirationMs;
	}
}
