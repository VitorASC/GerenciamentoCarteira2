package com.projeto.sistemabancario.config.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.security.login")
public record ProtecaoLoginProperties(@DefaultValue("5") int maxFailedAttempts,
		@DefaultValue("PT15M") Duration failureWindow, @DefaultValue("PT15M") Duration lockDuration) {

	public ProtecaoLoginProperties {
		if (maxFailedAttempts < 1) {
			throw new IllegalArgumentException("max-failed-attempts deve ser pelo menos 1");
		}
		validarDuracao(failureWindow, "failure-window");
		validarDuracao(lockDuration, "lock-duration");
	}

	private static void validarDuracao(Duration duracao, String nome) {
		if (duracao == null || duracao.isZero() || duracao.isNegative()) {
			throw new IllegalArgumentException(nome + " deve ser positiva");
		}
	}
}
