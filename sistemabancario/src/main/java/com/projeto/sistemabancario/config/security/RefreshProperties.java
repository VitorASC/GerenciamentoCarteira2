package com.projeto.sistemabancario.config.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.security.refresh")
public record RefreshProperties(@DefaultValue("PT30M") Duration idleTimeout,
		@DefaultValue("PT8H") Duration absoluteExpiration) {

	public RefreshProperties {
		validar(idleTimeout, "idle-timeout");
		validar(absoluteExpiration, "absolute-expiration");
	}

	private static void validar(Duration valor, String nome) {
		if (valor == null || valor.isZero() || valor.isNegative()) {
			throw new IllegalArgumentException("app.security.refresh." + nome + " deve ser positiva");
		}
	}
}
