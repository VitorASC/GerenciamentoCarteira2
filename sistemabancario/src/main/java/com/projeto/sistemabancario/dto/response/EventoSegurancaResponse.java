package com.projeto.sistemabancario.dto.response;

import java.time.Instant;

import com.projeto.sistemabancario.domains.entity.EventoSeguranca;
import com.projeto.sistemabancario.domains.enums.MotivoRevogacao;
import com.projeto.sistemabancario.domains.enums.TipoEventoSeguranca;

public record EventoSegurancaResponse(Long id, TipoEventoSeguranca tipo, Instant ocorridoEm,
		Long atorUsuarioId, Long alvoUsuarioId, String enderecoOrigem, MotivoRevogacao motivoRevogacao) {

	public static EventoSegurancaResponse de(EventoSeguranca evento) {
		return new EventoSegurancaResponse(evento.getId(), evento.getTipo(), evento.getOcorridoEm(),
				evento.getAtorUsuarioId(), evento.getAlvoUsuarioId(), evento.getEnderecoOrigem(),
				evento.getMotivoRevogacao());
	}
}
