package com.projeto.sistemabancario.integration.exception;

public class ExternalIntegrationException extends RuntimeException {

	public ExternalIntegrationException(String message) {
		super(message);
	}

	public ExternalIntegrationException(String message, Throwable cause) {
		super(message, cause);
	}
}
