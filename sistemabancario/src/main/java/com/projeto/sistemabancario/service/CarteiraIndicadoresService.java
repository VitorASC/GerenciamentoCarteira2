package com.projeto.sistemabancario.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.sistemabancario.dto.response.IndicadorMediaCarteiraResponse;
import com.projeto.sistemabancario.dto.response.IndicadorTickerCarteiraResponse;
import com.projeto.sistemabancario.dto.response.PosicaoIndicadorMercadoResponse;
import com.projeto.sistemabancario.exception.RegraNegocioException;
import com.projeto.sistemabancario.exception.ResourceNotFoundException;
import com.projeto.sistemabancario.integration.cotacao.CotacaoConsultationPort;
import com.projeto.sistemabancario.integration.dto.CotacaoConsultaResult;
import com.projeto.sistemabancario.domains.entity.Acao;
import com.projeto.sistemabancario.domains.entity.CarteiraInvestimento;
import com.projeto.sistemabancario.domains.entity.PosicaoCarteira;
import com.projeto.sistemabancario.repository.CarteiraInvestimentoRepository;
import com.projeto.sistemabancario.repository.PosicaoCarteiraRepository;

@Service
public class CarteiraIndicadoresService {

	private static final MathContext MC = new MathContext(18, RoundingMode.HALF_UP);

	private final CarteiraInvestimentoRepository carteiraInvestimentoRepository;
	private final PosicaoCarteiraRepository posicaoCarteiraRepository;
	private final CotacaoConsultationPort cotacaoConsultationPort;

	public CarteiraIndicadoresService(CarteiraInvestimentoRepository carteiraInvestimentoRepository,
			PosicaoCarteiraRepository posicaoCarteiraRepository,
			CotacaoConsultationPort cotacaoConsultationPort) {
		this.carteiraInvestimentoRepository = carteiraInvestimentoRepository;
		this.posicaoCarteiraRepository = posicaoCarteiraRepository;
		this.cotacaoConsultationPort = cotacaoConsultationPort;
	}

	@Transactional(readOnly = true)
	public IndicadorTickerCarteiraResponse indicadorPorTicker(Long carteiraId, String tickerBruto,
			Long usuarioAutenticadoId) {
		String ticker = tickerBruto.trim().toUpperCase();
		CarteiraInvestimento carteira = obterCarteiraDoUsuario(carteiraId, usuarioAutenticadoId);

		List<PosicaoCarteira> linhas = posicaoCarteiraRepository.findAllByCarteiraIdWithAcao(carteiraId);
		PosicaoCarteira posicao = linhas.stream()
			.filter(p -> p.getAcao().getTicker().equalsIgnoreCase(ticker))
			.findFirst()
			.orElseThrow(() -> new RegraNegocioException("Não há posição neste ticker na carteira."));

		return montarIndicadorTicker(carteira.getId(), posicao);
	}

	@Transactional(readOnly = true)
	public IndicadorMediaCarteiraResponse indicadorMediaCarteira(Long carteiraId, Long usuarioAutenticadoId) {
		CarteiraInvestimento carteira = obterCarteiraDoUsuario(carteiraId, usuarioAutenticadoId);

		List<PosicaoCarteira> linhas = posicaoCarteiraRepository.findAllByCarteiraIdWithAcao(carteira.getId());
		List<PosicaoIndicadorMercadoResponse> detalhes = new ArrayList<>();
		BigDecimal custoTotal = BigDecimal.ZERO;
		BigDecimal mercadoTotal = BigDecimal.ZERO;
		BigDecimal quantidadeTotalTitulos = BigDecimal.ZERO;

		for (PosicaoCarteira p : linhas) {
			if (p.getQuantidade().compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			IndicadorTickerCarteiraResponse um = montarIndicadorTicker(carteira.getId(), p);
			detalhes.add(new PosicaoIndicadorMercadoResponse(
					um.ticker(),
					um.quantidade(),
					um.precoMedioPonderado(),
					um.cotacaoMercadoAtual(),
					um.valorCustoTotal(),
					um.valorMercadoAtual()));
			custoTotal = custoTotal.add(um.valorCustoTotal(), MC);
			mercadoTotal = mercadoTotal.add(um.valorMercadoAtual(), MC);
			quantidadeTotalTitulos = quantidadeTotalTitulos.add(um.quantidade(), MC);
		}

		int n = detalhes.size();
		BigDecimal mediaPrecoMedioPonderado = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
		BigDecimal mediaValorMercadoPorTitulo = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
		if (quantidadeTotalTitulos.compareTo(BigDecimal.ZERO) > 0) {
			mediaPrecoMedioPonderado = custoTotal.divide(quantidadeTotalTitulos, 6, RoundingMode.HALF_UP);
			mediaValorMercadoPorTitulo = mercadoTotal.divide(quantidadeTotalTitulos, 6, RoundingMode.HALF_UP);
		}

		BigDecimal rentabilidadeNaoRealizadaPercentual = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
		if (custoTotal.compareTo(BigDecimal.ZERO) > 0) {
			rentabilidadeNaoRealizadaPercentual = mercadoTotal.subtract(custoTotal, MC)
				.divide(custoTotal, 8, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100), MC)
				.setScale(6, RoundingMode.HALF_UP);
		}

		return new IndicadorMediaCarteiraResponse(
				carteira.getId(),
				n,
				quantidadeTotalTitulos.setScale(8, RoundingMode.HALF_UP),
				custoTotal.setScale(2, RoundingMode.HALF_UP),
				mercadoTotal.setScale(2, RoundingMode.HALF_UP),
				mediaPrecoMedioPonderado,
				mediaValorMercadoPorTitulo,
				rentabilidadeNaoRealizadaPercentual,
				detalhes);
	}

	private IndicadorTickerCarteiraResponse montarIndicadorTicker(Long carteiraId, PosicaoCarteira posicao) {
		Acao acao = posicao.getAcao();
		CotacaoConsultaResult cot = cotacaoConsultationPort.buscar(acao.getMercado(), acao.getTicker())
			.orElseThrow(() -> new RegraNegocioException(
					"Não foi possível obter a cotação de mercado para " + acao.getTicker() + " (indicadores)."));

		BigDecimal q = posicao.getQuantidade().setScale(8, RoundingMode.HALF_UP);
		BigDecimal pm = posicao.getPrecoMedioPonderado().setScale(6, RoundingMode.HALF_UP);
		BigDecimal precoMercado = cot.preco().setScale(6, RoundingMode.HALF_UP);
		LocalDateTime dtMercado = LocalDateTime.ofInstant(cot.dataHoraReferencia(), ZoneId.systemDefault());
		BigDecimal valorCusto = q.multiply(pm, MC).setScale(2, RoundingMode.HALF_UP);
		BigDecimal valorMercado = q.multiply(precoMercado, MC).setScale(2, RoundingMode.HALF_UP);

		return new IndicadorTickerCarteiraResponse(
				carteiraId,
				acao.getTicker(),
				q,
				pm,
				precoMercado,
				dtMercado,
				cot.moeda(),
				valorCusto,
				valorMercado);
	}

	private CarteiraInvestimento obterCarteiraDoUsuario(Long carteiraId, Long usuarioAutenticadoId) {
		CarteiraInvestimento carteira = carteiraInvestimentoRepository.findById(carteiraId)
			.orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada."));
		if (!carteira.getUsuario().getId().equals(usuarioAutenticadoId)) {
			throw new AccessDeniedException("Você não tem permissão para acessar esta carteira.");
		}
		return carteira;
	}
}
