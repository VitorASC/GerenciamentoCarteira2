package com.projeto.sistemabancario.integration.cotacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.projeto.sistemabancario.domains.entity.Acao;
import com.projeto.sistemabancario.domains.enums.Mercado;
import com.projeto.sistemabancario.integration.config.CacheNames;
import com.projeto.sistemabancario.integration.dto.CotacaoConsultaResult;
import com.projeto.sistemabancario.repository.AcaoRepository;
import com.projeto.sistemabancario.repository.CorretoraRepository;
import com.projeto.sistemabancario.repository.HistoricoCotacaoRepository;
import com.projeto.sistemabancario.repository.PosicaoCarteiraRepository;
import com.projeto.sistemabancario.repository.TransacaoRepository;
import com.projeto.sistemabancario.service.AcaoService;

@SpringJUnitConfig(AtualizacaoManualCotacaoCacheTest.Config.class)
class AtualizacaoManualCotacaoCacheTest {

	@Autowired
	private CotacaoConsultationPort cotacoes;

	@Autowired
	private BrapiCotacaoClient brapi;

	@Autowired
	private AcaoRepository acoes;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private AcaoService service;

	@BeforeEach
	void setUp() {
		reset(brapi, acoes);
		cacheManager.getCache(CacheNames.COTACAO).clear();
	}

	@Test
	void atualizacaoManualConsultaExternamenteEAtualizaCotacaoHistoricoECache() {
		Instant instanteAntigo = Instant.parse("2026-09-08T15:00:00Z");
		Instant instanteNovo = Instant.parse("2026-09-09T15:00:00Z");
		var antiga = new CotacaoConsultaResult("PETR4", "Petrobras", "BRL", new BigDecimal("30.00"), instanteAntigo);
		var nova = new CotacaoConsultaResult("PETR4", "Petrobras", "BRL", new BigDecimal("31.50"), instanteNovo);
		when(brapi.buscar("PETR4")).thenReturn(Optional.of(antiga), Optional.of(nova));

		assertThat(cotacoes.buscar(Mercado.BRASIL, "PETR4")).contains(antiga);
		assertThat(cotacoes.buscar(Mercado.BRASIL, "PETR4")).contains(antiga);

		Acao acao = new Acao();
		acao.setTicker("PETR4");
		acao.setMercado(Mercado.BRASIL);
		acao.setMoeda("BRL");
		acao.setNomeEmpresa("Petrobras");
		acao.setCotacaoAtual(antiga.preco());
		when(acoes.findById(1L)).thenReturn(Optional.of(acao));
		when(acoes.save(any(Acao.class))).thenReturn(acao);

		var resposta = service.atualizarCotacao(1L);

		assertThat(resposta.cotacaoAtual()).isEqualByComparingTo("31.50");
		assertThat(acao.getHistoricoCotacoes()).singleElement()
			.satisfies(h -> assertThat(h.getValor()).isEqualByComparingTo("31.50"));
		assertThat(cotacoes.buscar(Mercado.BRASIL, "PETR4")).contains(nova);
		verify(brapi, times(2)).buscar("PETR4");
	}

	@Configuration
	@EnableCaching
	static class Config {

		@Bean
		CacheManager cacheManager() {
			return new ConcurrentMapCacheManager(CacheNames.COTACAO);
		}

		@Bean
		BrapiCotacaoClient brapi() {
			return mock(BrapiCotacaoClient.class);
		}

		@Bean
		AlphaVantageCotacaoClient alphaVantage() {
			return mock(AlphaVantageCotacaoClient.class);
		}

		@Bean
		RoutingCotacaoClient cotacoes(BrapiCotacaoClient brapi, AlphaVantageCotacaoClient alphaVantage) {
			return new RoutingCotacaoClient(brapi, alphaVantage);
		}

		@Bean
		AcaoRepository acoes() {
			return mock(AcaoRepository.class);
		}

		@Bean
		CorretoraRepository corretoras() {
			return mock(CorretoraRepository.class);
		}

		@Bean
		HistoricoCotacaoRepository historicos() {
			return mock(HistoricoCotacaoRepository.class);
		}

		@Bean
		PosicaoCarteiraRepository posicoes() {
			return mock(PosicaoCarteiraRepository.class);
		}

		@Bean
		TransacaoRepository transacoes() {
			return mock(TransacaoRepository.class);
		}

		@Bean
		AcaoService acaoService(AcaoRepository acoes, CorretoraRepository corretoras,
				CotacaoConsultationPort cotacoes, HistoricoCotacaoRepository historicos,
				PosicaoCarteiraRepository posicoes, TransacaoRepository transacoes) {
			return new AcaoService(acoes, corretoras, cotacoes, historicos, posicoes, transacoes);
		}
	}
}
