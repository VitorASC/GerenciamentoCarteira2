package com.projeto.sistemabancario.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.sistemabancario.dto.request.AcaoCadastroRequest;
import com.projeto.sistemabancario.dto.response.AcaoResponse;
import com.projeto.sistemabancario.dto.response.HistoricoCotacaoResponse;
import com.projeto.sistemabancario.exception.DuplicateResourceException;
import com.projeto.sistemabancario.exception.RegraNegocioException;
import com.projeto.sistemabancario.exception.ResourceNotFoundException;
import com.projeto.sistemabancario.integration.cotacao.CotacaoConsultationPort;
import com.projeto.sistemabancario.domains.entity.Acao;
import com.projeto.sistemabancario.domains.entity.Corretora;
import com.projeto.sistemabancario.domains.entity.HistoricoCotacao;
import com.projeto.sistemabancario.repository.AcaoRepository;
import com.projeto.sistemabancario.repository.CorretoraRepository;
import com.projeto.sistemabancario.repository.HistoricoCotacaoRepository;
import com.projeto.sistemabancario.repository.PosicaoCarteiraRepository;
import com.projeto.sistemabancario.repository.TransacaoRepository;

@Service
public class AcaoService {

	private final AcaoRepository acaoRepository;
	private final CorretoraRepository corretoraRepository;
	private final CotacaoConsultationPort cotacaoConsultationPort;
	private final HistoricoCotacaoRepository historicoCotacaoRepository;
	private final PosicaoCarteiraRepository posicaoCarteiraRepository;
	private final TransacaoRepository transacaoRepository;

	public AcaoService(AcaoRepository acaoRepository, CorretoraRepository corretoraRepository,
			CotacaoConsultationPort cotacaoConsultationPort, HistoricoCotacaoRepository historicoCotacaoRepository,
			PosicaoCarteiraRepository posicaoCarteiraRepository, TransacaoRepository transacaoRepository) {
		this.acaoRepository = acaoRepository;
		this.corretoraRepository = corretoraRepository;
		this.cotacaoConsultationPort = cotacaoConsultationPort;
		this.historicoCotacaoRepository = historicoCotacaoRepository;
		this.posicaoCarteiraRepository = posicaoCarteiraRepository;
		this.transacaoRepository = transacaoRepository;
	}

	@Transactional
	public AcaoResponse cadastrar(AcaoCadastroRequest request) {
		String ticker = request.ticker().trim().toUpperCase();
		if (acaoRepository.existsByTickerIgnoreCase(ticker)) {
			throw new DuplicateResourceException("Já existe ação cadastrada com este ticker.");
		}

		var cotacao = cotacaoConsultationPort.buscar(request.mercado(), ticker).orElseThrow(
				() -> new RegraNegocioException("Ticker não encontrado ou indisponível na API de cotação do mercado informado."));

		LocalDateTime dataHora = LocalDateTime.ofInstant(cotacao.dataHoraReferencia(), ZoneId.systemDefault());

		Acao acao = new Acao();
		acao.setTicker(ticker);
		acao.setNomeEmpresa(cotacao.nomeEmpresa());
		acao.setMercado(request.mercado());
		acao.setMoeda(cotacao.moeda());
		acao.setCotacaoAtual(cotacao.preco());
		acao.setDataHoraCotacao(dataHora);

		if (request.corretoraId() != null) {
			Corretora corretora = corretoraRepository.findById(request.corretoraId())
				.orElseThrow(() -> new ResourceNotFoundException("Corretora informada não existe."));
			acao.setCorretora(corretora);
		}

		registrarHistorico(acao, cotacao.preco(), dataHora);

		return toResponse(acaoRepository.save(acao));
	}

	@Transactional(readOnly = true)
	public Page<AcaoResponse> listar(Pageable pageable) {
		return acaoRepository.findAll(pageable).map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public AcaoResponse buscarPorId(Long id) {
		Acao acao = acaoRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Ação não encontrada."));
		return toResponse(acao);
	}

	@Transactional(readOnly = true)
	public AcaoResponse buscarPorTicker(String tickerBruto) {
		String ticker = tickerBruto.trim().toUpperCase();
		Acao acao = acaoRepository.findByTickerIgnoreCase(ticker)
			.orElseThrow(() -> new ResourceNotFoundException("Ação não encontrada para o ticker informado."));
		return toResponse(acao);
	}

	@Transactional
	public AcaoResponse atualizarCotacao(Long id) {
		Acao acao = acaoRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Ação não encontrada."));

		var cotacao = cotacaoConsultationPort.buscar(acao.getMercado(), acao.getTicker()).orElseThrow(
				() -> new RegraNegocioException("Não foi possível obter cotação atualizada nas APIs externas."));

		LocalDateTime dataHora = LocalDateTime.ofInstant(cotacao.dataHoraReferencia(), ZoneId.systemDefault());
		acao.setCotacaoAtual(cotacao.preco());
		acao.setDataHoraCotacao(dataHora);
		if (cotacao.nomeEmpresa() != null && !cotacao.nomeEmpresa().isBlank()) {
			acao.setNomeEmpresa(cotacao.nomeEmpresa());
		}

		registrarHistorico(acao, cotacao.preco(), dataHora);

		return toResponse(acaoRepository.save(acao));
	}

	@Transactional
	public void excluir(Long id) {
		Acao acao = acaoRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Ação não encontrada."));

		if (posicaoCarteiraRepository.existsByAcao_Id(id)) {
			throw new RegraNegocioException(
					"Não é possível excluir esta ação: existem posições em carteiras vinculadas a ela.");
		}
		if (transacaoRepository.existsByAcao_Id(id)) {
			throw new RegraNegocioException(
					"Não é possível excluir esta ação: existem transações (compras/vendas) registradas para ela.");
		}

		acaoRepository.delete(acao);
	}

	@Transactional(readOnly = true)
	public Page<HistoricoCotacaoResponse> listarHistoricoCotacoes(Long acaoId, Pageable pageable) {
		if (!acaoRepository.existsById(acaoId)) {
			throw new ResourceNotFoundException("Ação não encontrada.");
		}
		return historicoCotacaoRepository.findByAcao_IdOrderByDataHoraAsc(acaoId, pageable)
			.map(h -> new HistoricoCotacaoResponse(h.getId(), h.getValor(), h.getDataHora()));
	}

	private void registrarHistorico(Acao acao, BigDecimal valor, LocalDateTime dataHora) {
		HistoricoCotacao linha = new HistoricoCotacao();
		linha.setAcao(acao);
		linha.setValor(valor);
		linha.setDataHora(dataHora);
		acao.getHistoricoCotacoes().add(linha);
	}

	private AcaoResponse toResponse(Acao acao) {
		Long corretoraId = acao.getCorretora() != null ? acao.getCorretora().getId() : null;
		return new AcaoResponse(
				acao.getId(),
				acao.getTicker(),
				acao.getNomeEmpresa(),
				acao.getMercado(),
				acao.getMoeda(),
				acao.getCotacaoAtual(),
				acao.getDataHoraCotacao(),
				corretoraId);
	}
}
