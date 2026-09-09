package com.projeto.sistemabancario.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.sistemabancario.config.security.JwtProperties;
import com.projeto.sistemabancario.config.security.JwtService;
import com.projeto.sistemabancario.config.security.UsuarioPrincipal;
import com.projeto.sistemabancario.dto.request.LoginRequest;
import com.projeto.sistemabancario.dto.response.LoginResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final JwtProperties jwtProperties;

	public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
			JwtProperties jwtProperties) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.jwtProperties = jwtProperties;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		var auth = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.email().trim(), request.senha()));
		var principal = (UsuarioPrincipal) auth.getPrincipal();
		String token = jwtService.gerarToken(principal.getUsuarioId(), principal.getUsername(), principal.isEnabled());
		return new LoginResponse("Bearer", token, jwtProperties.getExpirationMs());
	}
}
