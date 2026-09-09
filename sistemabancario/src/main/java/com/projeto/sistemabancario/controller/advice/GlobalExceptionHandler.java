package com.projeto.sistemabancario.controller.advice;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.projeto.sistemabancario.dto.response.ErroResponse;
import com.projeto.sistemabancario.exception.DuplicateResourceException;
import com.projeto.sistemabancario.exception.RegraNegocioException;
import com.projeto.sistemabancario.exception.ResourceNotFoundException;
import com.projeto.sistemabancario.integration.exception.ExternalIntegrationException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErroResponse> credenciaisInvalidas(BadCredentialsException ex, HttpServletRequest req) {
		return montar(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", "E-mail ou senha inválidos.", req);
	}

	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ErroResponse> usuarioInativo(DisabledException ex, HttpServletRequest req) {
		return montar(HttpStatus.FORBIDDEN, "Conta inativa", "Usuário inativo. Entre em contato com o suporte.", req);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErroResponse> autenticacao(AuthenticationException ex, HttpServletRequest req) {
		return montar(HttpStatus.UNAUTHORIZED, "Não autenticado", ex.getMessage() != null ? ex.getMessage() : "Falha na autenticação.", req);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException ex, HttpServletRequest req) {
		String mensagem = ex.getMessage() != null && !ex.getMessage().isBlank()
				? ex.getMessage()
				: "Você não tem permissão para este recurso.";
		return montar(HttpStatus.FORBIDDEN, "Acesso negado", mensagem, req);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErroResponse> recursoNaoEncontrado(ResourceNotFoundException ex, HttpServletRequest req) {
		return montar(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), req);
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ErroResponse> duplicado(DuplicateResourceException ex, HttpServletRequest req) {
		return montar(HttpStatus.CONFLICT, "Conflito", ex.getMessage(), req);
	}

	@ExceptionHandler(RegraNegocioException.class)
	public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest req) {
		return montar(HttpStatus.BAD_REQUEST, "Regra de negócio", ex.getMessage(), req);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErroResponse> integridadeDados(DataIntegrityViolationException ex, HttpServletRequest req) {
		Throwable cause = ex.getMostSpecificCause();
		String detalhe = cause != null ? cause.getMessage() : ex.getMessage();
		log.warn("Violação de integridade ao persistir: {}", detalhe);
		String mensagem = "Não foi possível concluir o cadastro (restrição do banco de dados). Verifique dados duplicados ou tamanho dos campos.";
		if (detalhe != null && detalhe.contains("value too long")) {
			mensagem = "Um ou mais campos retornados pelas APIs públicas excedem o limite de armazenamento. Tente novamente ou informe o suporte.";
		}
		if (detalhe != null && detalhe.contains("duplicate key")) {
			mensagem = "Registro duplicado: já existe um cadastro com estes dados.";
		}
		return montar(HttpStatus.BAD_REQUEST, "Integridade dos dados", mensagem, req);
	}

	@ExceptionHandler(ExternalIntegrationException.class)
	public ResponseEntity<ErroResponse> integracaoExterna(ExternalIntegrationException ex, HttpServletRequest req) {
		return montar(HttpStatus.BAD_GATEWAY, "Falha em integração externa", ex.getMessage(), req);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
		String mensagem = ex.getBindingResult().getFieldErrors().stream()
			.map(FieldError::getDefaultMessage)
			.findFirst()
			.orElse("Dados inválidos.");
		return montar(HttpStatus.BAD_REQUEST, "Validação", mensagem, req);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErroResponse> generico(Exception ex, HttpServletRequest req) {
		log.error("Erro não tratado", ex);
		return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
				"Ocorreu um erro inesperado. Tente novamente mais tarde.", req);
	}

	private static ResponseEntity<ErroResponse> montar(HttpStatus status, String erro, String mensagem,
			HttpServletRequest req) {
		ErroResponse body = new ErroResponse(status.value(), erro, mensagem, OffsetDateTime.now(), req.getRequestURI());
		return ResponseEntity.status(status).body(body);
	}
}
