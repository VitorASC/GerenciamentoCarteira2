package com.projeto.sistemabancario.config.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.sistemabancario.dto.response.ErroResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		String mensagem = accessDeniedException.getMessage() != null && !accessDeniedException.getMessage().isBlank()
				? accessDeniedException.getMessage()
				: "Acesso negado.";
		ErroResponse body = new ErroResponse(
				HttpServletResponse.SC_FORBIDDEN,
				"Acesso negado",
				mensagem,
				OffsetDateTime.now(),
				request.getRequestURI());
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
