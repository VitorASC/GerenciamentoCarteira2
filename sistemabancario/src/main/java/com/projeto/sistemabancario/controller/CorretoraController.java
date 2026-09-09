package com.projeto.sistemabancario.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.sistemabancario.dto.request.CorretoraCadastroRequest;
import com.projeto.sistemabancario.dto.response.CorretoraResponse;
import com.projeto.sistemabancario.service.CorretoraService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/corretoras")
public class CorretoraController {

	private final CorretoraService corretoraService;

	public CorretoraController(CorretoraService corretoraService) {
		this.corretoraService = corretoraService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CorretoraResponse cadastrar(@Valid @RequestBody CorretoraCadastroRequest request) {
		return corretoraService.cadastrar(request);
	}

	@GetMapping
	public Page<CorretoraResponse> listar(@PageableDefault(size = 20) Pageable pageable) {
		return corretoraService.listar(pageable);
	}

	@GetMapping("/{id}")
	public CorretoraResponse buscarPorId(@PathVariable Long id) {
		return corretoraService.buscarPorId(id);
	}

	@GetMapping("/cnpj/{cnpj}")
	public CorretoraResponse buscarPorCnpj(@PathVariable String cnpj) {
		return corretoraService.buscarPorCnpj(cnpj);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id) {
		corretoraService.excluir(id);
	}
}
