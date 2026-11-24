package com.projeto.sistemabancario.controller;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.ResponseEntity;

import com.projeto.sistemabancario.domains.enums.TipoEventoSeguranca;
import com.projeto.sistemabancario.dto.response.EventoSegurancaResponse;
import com.projeto.sistemabancario.service.AuditoriaSegurancaService;

@RestController
public class AuditoriaSegurancaController {
	private final AuditoriaSegurancaService auditoria;

	public AuditoriaSegurancaController(AuditoriaSegurancaService auditoria) {
		this.auditoria = auditoria;
	}

	@GetMapping("/auditoria-seguranca")
	public Page<EventoSegurancaResponse> consultar(@RequestParam(required = false) TipoEventoSeguranca tipo,
			@RequestParam(required = false) Instant inicio, @RequestParam(required = false) Instant fim,
			@RequestParam(required = false) Long ator, @RequestParam(required = false) Long alvo,
			@PageableDefault(size = 20) Pageable pageable) {
		return auditoria.consultar(tipo, inicio, fim, ator, alvo, pageable);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Void> filtroInvalido() {
		return ResponseEntity.badRequest().build();
	}
}
