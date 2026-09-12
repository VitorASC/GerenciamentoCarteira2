package com.projeto.sistemabancario.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.sistemabancario.config.security.UsuarioPrincipal;
import com.projeto.sistemabancario.dto.request.CarteiraAtualizacaoRequest;
import com.projeto.sistemabancario.dto.request.CarteiraCadastroRequest;
import com.projeto.sistemabancario.dto.request.CompraAcaoRequest;
import com.projeto.sistemabancario.dto.request.VendaAcaoRequest;
import com.projeto.sistemabancario.dto.response.CarteiraResponse;
import com.projeto.sistemabancario.dto.response.IndicadorMediaCarteiraResponse;
import com.projeto.sistemabancario.dto.response.IndicadorTickerCarteiraResponse;
import com.projeto.sistemabancario.dto.response.OperacaoCarteiraResponse;
import com.projeto.sistemabancario.service.CarteiraIndicadoresService;
import com.projeto.sistemabancario.service.CarteiraService;
import com.projeto.sistemabancario.service.CarteiraTradingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/carteiras")
public class CarteiraController {

	private final CarteiraService carteiraService;
	private final CarteiraTradingService carteiraTradingService;
	private final CarteiraIndicadoresService carteiraIndicadoresService;

	public CarteiraController(CarteiraService carteiraService, CarteiraTradingService carteiraTradingService,
			CarteiraIndicadoresService carteiraIndicadoresService) {
		this.carteiraService = carteiraService;
		this.carteiraTradingService = carteiraTradingService;
		this.carteiraIndicadoresService = carteiraIndicadoresService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CarteiraResponse cadastrar(@AuthenticationPrincipal UsuarioPrincipal principal,
			@Valid @RequestBody CarteiraCadastroRequest request) {
		return carteiraService.cadastrar(request, principal.getUsuarioId());
	}

	@GetMapping
	public Page<CarteiraResponse> listar(@AuthenticationPrincipal UsuarioPrincipal principal,
			@PageableDefault(size = 20) Pageable pageable) {
		return carteiraService.listar(pageable, principal.getUsuarioId());
	}

	@GetMapping("/{id}")
	public CarteiraResponse buscarPorId(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
		return carteiraService.buscarPorId(id, principal.getUsuarioId());
	}

	@GetMapping("/usuario/{usuarioId}")
	public List<CarteiraResponse> listarPorUsuario(@AuthenticationPrincipal UsuarioPrincipal principal,
			@PathVariable Long usuarioId) {
		return carteiraService.listarPorUsuario(usuarioId, principal.getUsuarioId());
	}

	@PutMapping("/{id}")
	public CarteiraResponse atualizar(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
			@Valid @RequestBody CarteiraAtualizacaoRequest request) {
		return carteiraService.atualizar(id, request, principal.getUsuarioId());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
		carteiraService.excluir(id, principal.getUsuarioId());
	}

	@PostMapping("/{carteiraId}/compras")
	public OperacaoCarteiraResponse comprar(@AuthenticationPrincipal UsuarioPrincipal principal,
			@PathVariable Long carteiraId, @Valid @RequestBody CompraAcaoRequest request) {
		return carteiraTradingService.comprar(carteiraId, request, principal.getUsuarioId());
	}

	@PostMapping("/{carteiraId}/vendas")
	public OperacaoCarteiraResponse vender(@AuthenticationPrincipal UsuarioPrincipal principal,
			@PathVariable Long carteiraId, @Valid @RequestBody VendaAcaoRequest request) {
		return carteiraTradingService.vender(carteiraId, request, principal.getUsuarioId());
	}

	@GetMapping("/{carteiraId}/indicadores/ticker/{ticker}")
	public IndicadorTickerCarteiraResponse indicadorPorTicker(@AuthenticationPrincipal UsuarioPrincipal principal,
			@PathVariable Long carteiraId, @PathVariable String ticker) {
		return carteiraIndicadoresService.indicadorPorTicker(carteiraId, ticker, principal.getUsuarioId());
	}

	@GetMapping("/{carteiraId}/indicadores/media-carteira")
	public IndicadorMediaCarteiraResponse indicadorMediaCarteira(@AuthenticationPrincipal UsuarioPrincipal principal,
			@PathVariable Long carteiraId) {
		return carteiraIndicadoresService.indicadorMediaCarteira(carteiraId, principal.getUsuarioId());
	}
}
