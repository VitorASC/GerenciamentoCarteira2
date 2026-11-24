package com.projeto.sistemabancario.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.projeto.sistemabancario.domains.entity.EventoSeguranca;
import com.projeto.sistemabancario.domains.enums.MotivoRevogacao;
import com.projeto.sistemabancario.domains.enums.TipoEventoSeguranca;
import com.projeto.sistemabancario.dto.response.EventoSegurancaResponse;
import com.projeto.sistemabancario.exception.RegraNegocioException;
import com.projeto.sistemabancario.repository.EventoSegurancaRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class AuditoriaSegurancaService {
	private final EventoSegurancaRepository eventos;
	private final Clock clock;

	public AuditoriaSegurancaService(EventoSegurancaRepository eventos, Clock clock) {
		this.eventos = eventos;
		this.clock = clock;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void registrar(TipoEventoSeguranca tipo, Long ator, Long alvo, String hash, MotivoRevogacao motivo) {
		String origem = RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes http
				? http.getRequest().getRemoteAddr() : null;
		eventos.save(new EventoSeguranca(tipo, clock.instant(), ator, alvo, hash, origem, motivo));
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasRole('ADMIN')")
	public Page<EventoSegurancaResponse> consultar(TipoEventoSeguranca tipo, Instant inicio, Instant fim,
			Long ator, Long alvo, Pageable pagina) {
		if (inicio != null && fim != null && !inicio.isBefore(fim)) {
			throw new RegraNegocioException("O fim do período deve ser posterior ao início.");
		}
		var ordem = Sort.by(Sort.Direction.DESC, "ocorridoEm", "id");
		var pedido = PageRequest.of(pagina.getPageNumber(), pagina.getPageSize(), ordem);
		return eventos.findAll((evento, consulta, cb) -> {
			var filtros = new ArrayList<Predicate>();
			if (tipo != null) {
				filtros.add(cb.equal(evento.get("tipo"), tipo));
			}
			if (inicio != null) {
				filtros.add(cb.greaterThanOrEqualTo(evento.get("ocorridoEm"), inicio));
			}
			if (fim != null) {
				filtros.add(cb.lessThan(evento.get("ocorridoEm"), fim));
			}
			if (ator != null) {
				filtros.add(cb.equal(evento.get("atorUsuarioId"), ator));
			}
			if (alvo != null) {
				filtros.add(cb.equal(evento.get("alvoUsuarioId"), alvo));
			}
			return cb.and(filtros.toArray(Predicate[]::new));
		}, pedido).map(EventoSegurancaResponse::de);
	}
}
