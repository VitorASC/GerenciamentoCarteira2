package com.projeto.sistemabancario.controller;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.sistemabancario.config.security.UsuarioPrincipal;
import com.projeto.sistemabancario.dto.request.LoginRequest;
import com.projeto.sistemabancario.dto.request.RefreshTokenRequest;
import com.projeto.sistemabancario.dto.response.ErroResponse;
import com.projeto.sistemabancario.dto.response.LoginResponse;
import com.projeto.sistemabancario.service.SessaoService;
import com.projeto.sistemabancario.service.LoginService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final LoginService loginService;
	private final SessaoService sessoes;

	public AuthController(LoginService loginService, SessaoService sessoes) {
		this.loginService = loginService;
		this.sessoes = sessoes;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return loginService.tentar(request).orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
	}

	@PostMapping("/refresh")
	public ResponseEntity<?> renovar(@RequestBody(required = false) RefreshTokenRequest request, HttpServletRequest http) {
		var par = sessoes.renovar(request == null ? null : request.refreshToken());
		if (par.isPresent()) {
			return ResponseEntity.ok(par.get());
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErroResponse(401, "Não autenticado",
				"Refresh token inválido ou expirado", OffsetDateTime.now(), http.getRequestURI()));
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@RequestBody(required = false) RefreshTokenRequest request) {
		sessoes.encerrar(request == null ? null : request.refreshToken());
	}

	@PostMapping("/logout-global")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logoutGlobal(@AuthenticationPrincipal UsuarioPrincipal principal) {
		sessoes.encerrarTodas(principal.getUsuarioId());
	}
}
