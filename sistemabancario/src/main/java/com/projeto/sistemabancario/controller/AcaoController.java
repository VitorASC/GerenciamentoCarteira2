package com.projeto.sistemabancario.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.sistemabancario.dto.request.AcaoCadastroRequest;
import com.projeto.sistemabancario.dto.response.AcaoResponse;
import com.projeto.sistemabancario.dto.response.HistoricoCotacaoResponse;
import com.projeto.sistemabancario.service.AcaoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/acoes")
public class AcaoController {

	private final AcaoService acaoService;

	public AcaoController(AcaoService acaoService) {
		this.acaoService = acaoService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AcaoResponse cadastrar(@Valid @RequestBody AcaoCadastroRequest request) {
		return acaoService.cadastrar(request);
	}

	@GetMapping
	public Page<AcaoResponse> listar(@PageableDefault(size = 20) Pageable pageable) {
		return acaoService.listar(pageable);
	}

	@GetMapping("/{id}")
	public AcaoResponse buscarPorId(@PathVariable Long id) {
		return acaoService.buscarPorId(id);
	}

	@GetMapping("/ticker/{ticker}")
	public AcaoResponse buscarPorTicker(@PathVariable String ticker) {
		return acaoService.buscarPorTicker(ticker);
	}

	@GetMapping("/{id}/historico-cotacoes")
	public Page<HistoricoCotacaoResponse> historicoCotacoes(@PathVariable Long id,
			@PageableDefault(size = 100) Pageable pageable) {
		return acaoService.listarHistoricoCotacoes(id, pageable);
	}

	@PutMapping("/{id}/atualizar-cotacao")
	public AcaoResponse atualizarCotacao(@PathVariable Long id) {
		return acaoService.atualizarCotacao(id);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id) {
		acaoService.excluir(id);
	}
}
