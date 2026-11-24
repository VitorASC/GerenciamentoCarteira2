package com.projeto.sistemabancario.controller;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final SessaoService sessoes;

	public AuthController(AuthenticationManager authenticationManager, SessaoService sessoes) {
		this.authenticationManager = authenticationManager;
		this.sessoes = sessoes;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		var auth = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.email().trim(), request.senha()));
		var principal = (UsuarioPrincipal) auth.getPrincipal();
		return sessoes.iniciar(principal.getUsuarioId());
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
