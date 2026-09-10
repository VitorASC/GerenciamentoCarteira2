package com.projeto.sistemabancario.integration.cotacao;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.projeto.sistemabancario.integration.config.CacheNames;
import com.projeto.sistemabancario.integration.dto.CotacaoConsultaResult;
import com.projeto.sistemabancario.domains.enums.Mercado;

@Service
@Primary
public class RoutingCotacaoClient implements CotacaoConsultationPort {

	private final BrapiCotacaoClient brapi;
	private final AlphaVantageCotacaoClient alphaVantage;

	public RoutingCotacaoClient(BrapiCotacaoClient brapi, AlphaVantageCotacaoClient alphaVantage) {
		this.brapi = brapi;
		this.alphaVantage = alphaVantage;
	}

	@Override
	@Cacheable(cacheNames = CacheNames.COTACAO,
			key = "#mercado.name() + '_' + (#ticker != null ? #ticker.toUpperCase() : '')",
			unless = "T(org.springframework.util.ObjectUtils).isEmpty(#result)")
	public Optional<CotacaoConsultaResult> buscar(Mercado mercado, String ticker) {
		return consultarCliente(mercado, ticker);
	}

	@Override
	@CachePut(cacheNames = CacheNames.COTACAO,
			key = "#mercado.name() + '_' + (#ticker != null ? #ticker.toUpperCase() : '')",
			unless = "T(org.springframework.util.ObjectUtils).isEmpty(#result)")
	public Optional<CotacaoConsultaResult> buscarAtualizada(Mercado mercado, String ticker) {
		return consultarCliente(mercado, ticker);
	}

	private Optional<CotacaoConsultaResult> consultarCliente(Mercado mercado, String ticker) {
		if (ticker == null || ticker.isBlank()) {
			return Optional.empty();
		}
		String normalized = ticker.trim().toUpperCase();
		return switch (mercado) {
			case BRASIL -> brapi.buscar(normalized);
			case ESTADOS_UNIDOS -> alphaVantage.buscar(normalized);
		};
	}
}
