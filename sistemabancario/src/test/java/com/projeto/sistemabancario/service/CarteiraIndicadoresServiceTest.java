package com.projeto.sistemabancario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.projeto.sistemabancario.domains.entity.Acao;
import com.projeto.sistemabancario.domains.entity.CarteiraInvestimento;
import com.projeto.sistemabancario.domains.entity.PosicaoCarteira;
import com.projeto.sistemabancario.domains.entity.Usuario;
import com.projeto.sistemabancario.domains.enums.Mercado;
import com.projeto.sistemabancario.integration.cotacao.CotacaoConsultationPort;
import com.projeto.sistemabancario.integration.dto.CotacaoConsultaResult;
import com.projeto.sistemabancario.repository.CarteiraInvestimentoRepository;
import com.projeto.sistemabancario.repository.PosicaoCarteiraRepository;

class CarteiraIndicadoresServiceTest {

	private CarteiraInvestimentoRepository carteiras;
	private PosicaoCarteiraRepository posicoes;
	private CotacaoConsultationPort cotacoes;
	private CarteiraIndicadoresService service;
	private CarteiraInvestimento carteira;

	@BeforeEach
	void setUp() {
		carteiras = mock(CarteiraInvestimentoRepository.class);
		posicoes = mock(PosicaoCarteiraRepository.class);
		cotacoes = mock(CotacaoConsultationPort.class);
		service = new CarteiraIndicadoresService(carteiras, posicoes, cotacoes);

		Usuario usuario = new Usuario();
		ReflectionTestUtils.setField(usuario, "id", 7L);
		carteira = new CarteiraInvestimento();
		ReflectionTestUtils.setField(carteira, "id", 1L);
		carteira.setUsuario(usuario);
		when(carteiras.findById(1L)).thenReturn(Optional.of(carteira));
	}

	@Test
	void calculaSeparadamenteMediaDeCustoMediaDeMercadoERentabilidadeNaoRealizada() {
		PosicaoCarteira petr4 = posicao("PETR4", "10", "30");
		PosicaoCarteira vale3 = posicao("VALE3", "5", "60");
		when(posicoes.findAllByCarteiraIdWithAcao(1L)).thenReturn(List.of(petr4, vale3));
		when(cotacoes.buscar(Mercado.BRASIL, "PETR4")).thenReturn(Optional.of(cotacao("PETR4", "40")));
		when(cotacoes.buscar(Mercado.BRASIL, "VALE3")).thenReturn(Optional.of(cotacao("VALE3", "80")));

		var antes = service.indicadorMediaCarteira(1L, 7L);
		assertThat(antes.mediaPrecoMedioPonderado()).isEqualByComparingTo("40");
		assertThat(antes.mediaValorMercadoPorTitulo()).isEqualByComparingTo("53.333333");
		assertThat(antes.rentabilidadeNaoRealizadaPercentual()).isEqualByComparingTo("33.333333");

		petr4.setQuantidade(new BigDecimal("5"));
		var depois = service.indicadorMediaCarteira(1L, 7L);
		assertThat(depois.mediaPrecoMedioPonderado()).isEqualByComparingTo("45");
		assertThat(depois.mediaValorMercadoPorTitulo()).isEqualByComparingTo("60");
		assertThat(depois.rentabilidadeNaoRealizadaPercentual()).isEqualByComparingTo("33.333333");
	}

	@Test
	void carteiraVaziaRetornaIndicadoresZeradosSemConsultarCotacoes() {
		when(posicoes.findAllByCarteiraIdWithAcao(1L)).thenReturn(List.of());

		var resposta = service.indicadorMediaCarteira(1L, 7L);

		assertThat(resposta.quantidadePosicoes()).isZero();
		assertThat(resposta.quantidadeTotalTitulos()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(resposta.custoTotalCarteira()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(resposta.valorMercadoTotalCarteira()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(resposta.mediaPrecoMedioPonderado()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(resposta.mediaValorMercadoPorTitulo()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(resposta.rentabilidadeNaoRealizadaPercentual()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(resposta.posicoes()).isEmpty();
		verifyNoInteractions(cotacoes);
	}

	private PosicaoCarteira posicao(String ticker, String quantidade, String precoMedio) {
		Acao acao = new Acao();
		acao.setTicker(ticker);
		acao.setMercado(Mercado.BRASIL);
		acao.setMoeda("BRL");
		PosicaoCarteira posicao = new PosicaoCarteira();
		posicao.setCarteira(carteira);
		posicao.setAcao(acao);
		posicao.setQuantidade(new BigDecimal(quantidade));
		posicao.setPrecoMedioPonderado(new BigDecimal(precoMedio));
		return posicao;
	}

	private static CotacaoConsultaResult cotacao(String ticker, String preco) {
		return new CotacaoConsultaResult(ticker, ticker, "BRL", new BigDecimal(preco), Instant.parse("2026-09-09T15:00:00Z"));
	}
}
