package com.projeto.sistemabancario.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.sistemabancario.config.security.UsuarioPrincipal;
import com.projeto.sistemabancario.dto.request.UsuarioAtualizacaoRequest;
import com.projeto.sistemabancario.dto.request.UsuarioCadastroRequest;
import com.projeto.sistemabancario.dto.response.UsuarioResponse;
import com.projeto.sistemabancario.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UsuarioResponse cadastrar(@Valid @RequestBody UsuarioCadastroRequest request) {
		return usuarioService.cadastrar(request);
	}

	@GetMapping
	public Page<UsuarioResponse> listar(@PageableDefault(size = 20) Pageable pageable) {
		return usuarioService.listar(pageable);
	}

	@GetMapping("/{id}")
	public UsuarioResponse buscarPorId(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
		return usuarioService.buscarPorId(id, principal.getUsuarioId());
	}

	@GetMapping("/cpf/{cpf}")
	public UsuarioResponse buscarPorCpf(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable String cpf) {
		return usuarioService.buscarPorCpf(cpf, principal.getUsuarioId());
	}

	@PutMapping("/{id}")
	public UsuarioResponse atualizar(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
			@RequestBody UsuarioAtualizacaoRequest request) {
		return usuarioService.atualizar(id, request, principal.getUsuarioId());
	}
}
