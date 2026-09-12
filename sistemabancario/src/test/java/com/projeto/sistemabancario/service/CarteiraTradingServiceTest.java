package com.projeto.sistemabancario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.projeto.sistemabancario.domains.entity.Acao;
import com.projeto.sistemabancario.domains.entity.CarteiraInvestimento;
import com.projeto.sistemabancario.domains.entity.PosicaoCarteira;
import com.projeto.sistemabancario.domains.entity.Usuario;
import com.projeto.sistemabancario.dto.request.CompraAcaoRequest;
import com.projeto.sistemabancario.dto.request.VendaAcaoRequest;
import com.projeto.sistemabancario.exception.RegraNegocioException;
import com.projeto.sistemabancario.repository.AcaoRepository;
import com.projeto.sistemabancario.repository.CarteiraInvestimentoRepository;
import com.projeto.sistemabancario.repository.PosicaoCarteiraRepository;

class CarteiraTradingServiceTest {

	private CarteiraInvestimentoRepository carteiras;
	private AcaoRepository acoes;
	private PosicaoCarteiraRepository posicoes;
	private CarteiraTradingService service;
	private CarteiraInvestimento carteira;
	private Acao acao;

	@BeforeEach
	void setUp() {
		carteiras = mock(CarteiraInvestimentoRepository.class);
		acoes = mock(AcaoRepository.class);
		posicoes = mock(PosicaoCarteiraRepository.class);
		service = new CarteiraTradingService(carteiras, acoes, posicoes);

		Usuario usuario = new Usuario();
		ReflectionTestUtils.setField(usuario, "id", 7L);
		carteira = new CarteiraInvestimento();
		ReflectionTestUtils.setField(carteira, "id", 1L);
		carteira.setUsuario(usuario);
		carteira.setSaldoTotal(new BigDecimal("10000.00"));
		carteira.setLucroPrejuizoRealizado(BigDecimal.ZERO);

		acao = new Acao();
		ReflectionTestUtils.setField(acao, "id", 2L);
		acao.setTicker("PETR4");
		acao.setMoeda("BRL");
		acao.setCotacaoAtual(new BigDecimal("49.00"));

		when(carteiras.findById(1L)).thenReturn(Optional.of(carteira));
		when(acoes.findById(2L)).thenReturn(Optional.of(acao));
		when(posicoes.findByCarteiraIdAndAcaoId(1L, 2L)).thenAnswer(ignored ->
				carteira.getPosicoes().stream().filter(p -> p.getAcao().getId().equals(2L)).findFirst());
	}

	@Test
	void compraPonderadaVendaParcialRecompraEVendaTotalPreservamCustoDaPosicaoAtual() {
		service.comprar(1L, new CompraAcaoRequest(2L, new BigDecimal("10"), new BigDecimal("100")), 7L);
		PosicaoCarteira posicao = carteira.getPosicoes().get(0);
		assertThat(posicao.getQuantidade()).isEqualByComparingTo("10");
		assertThat(posicao.getPrecoMedioPonderado()).isEqualByComparingTo("100");
		assertThat(acao.getCotacaoAtual()).isEqualByComparingTo("49");

		service.comprar(1L, new CompraAcaoRequest(2L, new BigDecimal("10"), new BigDecimal("150")), 7L);
		assertThat(posicao.getQuantidade()).isEqualByComparingTo("20");
		assertThat(posicao.getPrecoMedioPonderado()).isEqualByComparingTo("125");

		var vendaParcial = service.vender(1L, new VendaAcaoRequest(2L, new BigDecimal("5"), new BigDecimal("160")), 7L);
		assertThat(posicao.getQuantidade()).isEqualByComparingTo("15");
		assertThat(posicao.getPrecoMedioPonderado()).isEqualByComparingTo("125");
		assertThat(vendaParcial.lucroPrejuizoRealizadoCarteira()).isEqualByComparingTo("175");
		assertThat(acao.getCotacaoAtual()).isEqualByComparingTo("49");

		service.comprar(1L, new CompraAcaoRequest(2L, new BigDecimal("10"), new BigDecimal("200")), 7L);
		assertThat(posicao.getQuantidade()).isEqualByComparingTo("25");
		assertThat(posicao.getPrecoMedioPonderado()).isEqualByComparingTo("155");

		service.vender(1L, new VendaAcaoRequest(2L, new BigDecimal("25"), new BigDecimal("155")), 7L);
		assertThat(carteira.getPosicoes()).isEmpty();
		assertThat(carteira.getLucroPrejuizoRealizado()).isEqualByComparingTo("175");
	}

	@Test
	void vendaComPrejuizoMantemPrecoMedioDaPosicaoRemanescente() {
		PosicaoCarteira posicao = posicao("20", "125");
		carteira.getPosicoes().add(posicao);

		var resposta = service.vender(1L, new VendaAcaoRequest(2L, new BigDecimal("5"), new BigDecimal("100")), 7L);

		assertThat(posicao.getQuantidade()).isEqualByComparingTo("15");
		assertThat(posicao.getPrecoMedioPonderado()).isEqualByComparingTo("125");
		assertThat(resposta.lucroPrejuizoRealizadoCarteira()).isEqualByComparingTo("-125");
	}

	@Test
	void rejeitaPrecoDeOperacaoNuloZeroOuNegativo() {
		assertThatThrownBy(() -> service.comprar(1L,
				new CompraAcaoRequest(2L, new BigDecimal("1"), null), 7L))
				.isInstanceOf(RegraNegocioException.class);
		assertThatThrownBy(() -> service.comprar(1L,
				new CompraAcaoRequest(2L, new BigDecimal("1"), BigDecimal.ZERO), 7L))
				.isInstanceOf(RegraNegocioException.class);

		PosicaoCarteira posicao = posicao("1", "100");
		carteira.getPosicoes().add(posicao);
		assertThatThrownBy(() -> service.vender(1L,
				new VendaAcaoRequest(2L, new BigDecimal("1"), new BigDecimal("-1")), 7L))
				.isInstanceOf(RegraNegocioException.class);
	}

	private PosicaoCarteira posicao(String quantidade, String precoMedio) {
		PosicaoCarteira posicao = new PosicaoCarteira();
		posicao.setCarteira(carteira);
		posicao.setAcao(acao);
		posicao.setQuantidade(new BigDecimal(quantidade));
		posicao.setPrecoMedioPonderado(new BigDecimal(precoMedio));
		return posicao;
	}

}
