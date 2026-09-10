package com.projeto.sistemabancario.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import com.projeto.sistemabancario.dto.request.CompraAcaoRequest;
import com.projeto.sistemabancario.dto.request.VendaAcaoRequest;
import com.projeto.sistemabancario.dto.response.OperacaoCarteiraResponse;
import com.projeto.sistemabancario.exception.RegraNegocioException;
import com.projeto.sistemabancario.exception.ResourceNotFoundException;
import com.projeto.sistemabancario.integration.cotacao.CotacaoConsultationPort;
import com.projeto.sistemabancario.integration.dto.CotacaoConsultaResult;
import com.projeto.sistemabancario.domains.entity.Acao;
import com.projeto.sistemabancario.domains.entity.CarteiraInvestimento;
import com.projeto.sistemabancario.domains.entity.PosicaoCarteira;
import com.projeto.sistemabancario.domains.entity.Transacao;
import com.projeto.sistemabancario.domains.enums.TipoTransacao;
import com.projeto.sistemabancario.repository.AcaoRepository;
import com.projeto.sistemabancario.repository.CarteiraInvestimentoRepository;
import com.projeto.sistemabancario.repository.PosicaoCarteiraRepository;

@Service
public class CarteiraTradingService {

	private static final MathContext MC = new MathContext(18, RoundingMode.HALF_UP);

	private final CarteiraInvestimentoRepository carteiraInvestimentoRepository;
	private final AcaoRepository acaoRepository;
	private final PosicaoCarteiraRepository posicaoCarteiraRepository;
	private final CotacaoConsultationPort cotacaoConsultationPort;

	public CarteiraTradingService(CarteiraInvestimentoRepository carteiraInvestimentoRepository,
			AcaoRepository acaoRepository, PosicaoCarteiraRepository posicaoCarteiraRepository,
			CotacaoConsultationPort cotacaoConsultationPort) {
		this.carteiraInvestimentoRepository = carteiraInvestimentoRepository;
		this.acaoRepository = acaoRepository;
		this.posicaoCarteiraRepository = posicaoCarteiraRepository;
		this.cotacaoConsultationPort = cotacaoConsultationPort;
	}

	@Transactional
	public OperacaoCarteiraResponse comprar(Long carteiraId, CompraAcaoRequest request, Long usuarioAutenticadoId) {
		CarteiraInvestimento carteira = obterCarteiraDoUsuario(carteiraId, usuarioAutenticadoId);
		Acao acao = acaoRepository.findById(request.acaoId())
			.orElseThrow(() -> new ResourceNotFoundException("Ação não encontrada."));

		CotacaoConsultaResult cotacaoMercado = cotacaoConsultationPort.buscar(acao.getMercado(), acao.getTicker())
			.orElseThrow(() -> new RegraNegocioException(
					"Não foi possível obter a cotação atual na API externa para registrar a compra. Verifique o ticker, o mercado e as chaves BRAPI / Alpha Vantage."));

		BigDecimal quantidade = request.quantidade().setScale(8, RoundingMode.HALF_UP);
		BigDecimal precoUnitario = cotacaoMercado.preco().setScale(6, RoundingMode.HALF_UP);
		LocalDateTime dataHoraCotacao = LocalDateTime.ofInstant(cotacaoMercado.dataHoraReferencia(), ZoneId.systemDefault());
		acao.setCotacaoAtual(precoUnitario);
		acao.setDataHoraCotacao(dataHoraCotacao);
		if (cotacaoMercado.nomeEmpresa() != null && !cotacaoMercado.nomeEmpresa().isBlank()) {
			acao.setNomeEmpresa(cotacaoMercado.nomeEmpresa());
		}
		acaoRepository.save(acao);

		BigDecimal custo = quantidade.multiply(precoUnitario, MC);

		if (carteira.getSaldoTotal().compareTo(custo) < 0) {
			throw new RegraNegocioException("Saldo insuficiente para esta compra.");
		}

		PosicaoCarteira posicao = posicaoCarteiraRepository.findByCarteiraIdAndAcaoId(carteiraId, acao.getId())
			.orElseGet(() -> novaPosicao(carteira, acao));

		BigDecimal qAnt = posicao.getQuantidade();
		BigDecimal pmAnt = posicao.getPrecoMedioPonderado();
		BigDecimal qNova = qAnt.add(quantidade, MC);

		BigDecimal pmNova;
		if (qAnt.compareTo(BigDecimal.ZERO) == 0) {
			pmNova = precoUnitario;
		}
		else {
			BigDecimal valorAntigo = qAnt.multiply(pmAnt, MC);
			BigDecimal valorCompra = quantidade.multiply(precoUnitario, MC);
			pmNova = valorAntigo.add(valorCompra, MC).divide(qNova, 6, RoundingMode.HALF_UP);
		}
		posicao.setQuantidade(qNova);
		posicao.setPrecoMedioPonderado(pmNova);

		carteira.setSaldoTotal(carteira.getSaldoTotal().subtract(custo).setScale(2, RoundingMode.HALF_UP));

		Transacao transacao = registrarTransacao(carteira, acao, TipoTransacao.COMPRA, quantidade, precoUnitario);

		carteiraInvestimentoRepository.saveAndFlush(carteira);
		return toOperacaoResponse(transacao, carteira);
	}

	@Transactional
	public OperacaoCarteiraResponse vender(Long carteiraId, VendaAcaoRequest request, Long usuarioAutenticadoId) {
		CarteiraInvestimento carteira = obterCarteiraDoUsuario(carteiraId, usuarioAutenticadoId);
		Acao acao = acaoRepository.findById(request.acaoId())
			.orElseThrow(() -> new ResourceNotFoundException("Ação não encontrada."));

		BigDecimal quantidade = request.quantidade().setScale(8, RoundingMode.HALF_UP);

		PosicaoCarteira posicao = posicaoCarteiraRepository.findByCarteiraIdAndAcaoId(carteiraId, acao.getId())
			.orElseThrow(() -> new RegraNegocioException("Não há posição nesta ação na carteira."));

		if (posicao.getQuantidade().compareTo(quantidade) < 0) {
			throw new RegraNegocioException("Quantidade à venda maior que a posição atual.");
		}

		CotacaoConsultaResult cotacaoMercado = cotacaoConsultationPort.buscar(acao.getMercado(), acao.getTicker())
			.orElseThrow(() -> new RegraNegocioException(
					"Não foi possível obter a cotação atual na API externa para registrar a venda. Verifique o ticker, o mercado e as chaves BRAPI / Alpha Vantage."));

		BigDecimal precoVenda = cotacaoMercado.preco().setScale(6, RoundingMode.HALF_UP);
		LocalDateTime dataHoraVenda = LocalDateTime.ofInstant(cotacaoMercado.dataHoraReferencia(), ZoneId.systemDefault());
		acao.setCotacaoAtual(precoVenda);
		acao.setDataHoraCotacao(dataHoraVenda);
		if (cotacaoMercado.nomeEmpresa() != null && !cotacaoMercado.nomeEmpresa().isBlank()) {
			acao.setNomeEmpresa(cotacaoMercado.nomeEmpresa());
		}
		acaoRepository.save(acao);

		BigDecimal pm = posicao.getPrecoMedioPonderado();
		BigDecimal qRestante = posicao.getQuantidade().subtract(quantidade, MC);

		BigDecimal liquido = quantidade.multiply(precoVenda, MC);
		BigDecimal lucroRealizado = quantidade.multiply(precoVenda.subtract(pm, MC), MC);

		carteira.setSaldoTotal(carteira.getSaldoTotal().add(liquido).setScale(2, RoundingMode.HALF_UP));
		carteira.setLucroPrejuizoRealizado(
				carteira.getLucroPrejuizoRealizado().add(lucroRealizado).setScale(6, RoundingMode.HALF_UP));

		if (qRestante.compareTo(BigDecimal.ZERO) <= 0) {
			carteira.getPosicoes().remove(posicao);
		}
		else {
			posicao.setQuantidade(qRestante.setScale(8, RoundingMode.HALF_UP));
		}

		Transacao transacao = registrarTransacao(carteira, acao, TipoTransacao.VENDA, quantidade, precoVenda);

		carteiraInvestimentoRepository.saveAndFlush(carteira);
		return toOperacaoResponse(transacao, carteira);
	}

	private CarteiraInvestimento obterCarteiraDoUsuario(Long carteiraId, Long usuarioAutenticadoId) {
		CarteiraInvestimento carteira = carteiraInvestimentoRepository.findById(carteiraId)
			.orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada."));
		if (!carteira.getUsuario().getId().equals(usuarioAutenticadoId)) {
			throw new AccessDeniedException("Você não tem permissão para operar nesta carteira.");
		}
		return carteira;
	}

	private static PosicaoCarteira novaPosicao(CarteiraInvestimento carteira, Acao acao) {
		PosicaoCarteira p = new PosicaoCarteira();
		p.setCarteira(carteira);
		p.setAcao(acao);
		p.setQuantidade(BigDecimal.ZERO);
		p.setPrecoMedioPonderado(BigDecimal.ZERO);
		carteira.getPosicoes().add(p);
		return p;
	}

	private static Transacao registrarTransacao(CarteiraInvestimento carteira, Acao acao, TipoTransacao tipo,
			BigDecimal quantidade, BigDecimal precoUnitario) {
		Transacao t = new Transacao();
		t.setCarteira(carteira);
		t.setAcao(acao);
		t.setTipo(tipo);
		t.setQuantidade(quantidade);
		t.setPrecoUnitario(precoUnitario);
		t.setDataHora(LocalDateTime.now());
		carteira.getTransacoes().add(t);
		return t;
	}

	private static OperacaoCarteiraResponse toOperacaoResponse(Transacao t, CarteiraInvestimento carteira) {
		return new OperacaoCarteiraResponse(
				t.getId(),
				t.getTipo(),
				t.getAcao().getId(),
				t.getAcao().getTicker(),
				t.getQuantidade(),
				t.getPrecoUnitario(),
				t.getDataHora(),
				carteira.getSaldoTotal(),
				carteira.getLucroPrejuizoRealizado());
	}
}
