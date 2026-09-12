package com.projeto.sistemabancario.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.sistemabancario.dto.request.CarteiraAtualizacaoRequest;
import com.projeto.sistemabancario.dto.request.CarteiraCadastroRequest;
import com.projeto.sistemabancario.dto.response.CarteiraResponse;
import com.projeto.sistemabancario.dto.response.PosicaoCarteiraResponse;
import com.projeto.sistemabancario.exception.RegraNegocioException;
import com.projeto.sistemabancario.exception.ResourceNotFoundException;
import com.projeto.sistemabancario.domains.entity.CarteiraInvestimento;
import com.projeto.sistemabancario.domains.entity.Corretora;
import com.projeto.sistemabancario.domains.entity.PosicaoCarteira;
import com.projeto.sistemabancario.domains.entity.Usuario;
import com.projeto.sistemabancario.repository.CarteiraInvestimentoRepository;
import com.projeto.sistemabancario.repository.CorretoraRepository;
import com.projeto.sistemabancario.repository.PosicaoCarteiraRepository;
import com.projeto.sistemabancario.repository.TransacaoRepository;
import com.projeto.sistemabancario.repository.UsuarioRepository;

@Service
public class CarteiraService {

	private static final MathContext MC = new MathContext(18, RoundingMode.HALF_UP);

	private final CarteiraInvestimentoRepository carteiraInvestimentoRepository;
	private final UsuarioRepository usuarioRepository;
	private final CorretoraRepository corretoraRepository;
	private final PosicaoCarteiraRepository posicaoCarteiraRepository;
	private final TransacaoRepository transacaoRepository;

	public CarteiraService(CarteiraInvestimentoRepository carteiraInvestimentoRepository,
			UsuarioRepository usuarioRepository, CorretoraRepository corretoraRepository,
			PosicaoCarteiraRepository posicaoCarteiraRepository, TransacaoRepository transacaoRepository) {
		this.carteiraInvestimentoRepository = carteiraInvestimentoRepository;
		this.usuarioRepository = usuarioRepository;
		this.corretoraRepository = corretoraRepository;
		this.posicaoCarteiraRepository = posicaoCarteiraRepository;
		this.transacaoRepository = transacaoRepository;
	}

	@Transactional
	public CarteiraResponse cadastrar(CarteiraCadastroRequest request, Long usuarioAutenticadoId) {
		if (!request.usuarioId().equals(usuarioAutenticadoId)) {
			throw new AccessDeniedException("Você só pode criar carteiras para o próprio usuário.");
		}
		Usuario usuario = usuarioRepository.findById(request.usuarioId())
			.orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

		BigDecimal saldoInicial = request.saldoInicial() != null ? request.saldoInicial() : BigDecimal.ZERO;
		if (saldoInicial.compareTo(BigDecimal.ZERO) < 0) {
			throw new RegraNegocioException("Saldo inicial não pode ser negativo.");
		}

		CarteiraInvestimento c = new CarteiraInvestimento();
		c.setUsuario(usuario);
		c.setNomeDaCarteira(request.nomeDaCarteira().trim());
		c.setSaldoTotal(saldoInicial.setScale(2, RoundingMode.HALF_UP));
		c.setLucroPrejuizoRealizado(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
		c.setDataCriacao(LocalDateTime.now());

		if (request.corretoraId() != null) {
			Corretora corretora = corretoraRepository.findById(request.corretoraId())
				.orElseThrow(() -> new ResourceNotFoundException("Corretora informada não existe."));
			c.setCorretora(corretora);
		}

		return toDetail(carteiraInvestimentoRepository.save(c));
	}

	@Transactional(readOnly = true)
	public Page<CarteiraResponse> listar(Pageable pageable, Long usuarioAutenticadoId) {
		return carteiraInvestimentoRepository.findByUsuario_Id(usuarioAutenticadoId, pageable).map(this::toSummary);
	}

	@Transactional(readOnly = true)
	public CarteiraResponse buscarPorId(Long id, Long usuarioAutenticadoId) {
		CarteiraInvestimento c = carteiraInvestimentoRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada."));
		garantirDono(c, usuarioAutenticadoId);
		return toDetail(c);
	}

	@Transactional(readOnly = true)
	public List<CarteiraResponse> listarPorUsuario(Long usuarioId, Long usuarioAutenticadoId) {
		if (!usuarioId.equals(usuarioAutenticadoId)) {
			throw new AccessDeniedException("Você só pode listar as próprias carteiras.");
		}
		return carteiraInvestimentoRepository.findByUsuario_Id(usuarioId).stream().map(this::toSummary).toList();
	}

	@Transactional
	public CarteiraResponse atualizar(Long id, CarteiraAtualizacaoRequest request, Long usuarioAutenticadoId) {
		CarteiraInvestimento c = carteiraInvestimentoRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada."));
		garantirDono(c, usuarioAutenticadoId);
		if (request.nomeDaCarteira() != null && !request.nomeDaCarteira().isBlank()) {
			c.setNomeDaCarteira(request.nomeDaCarteira().trim());
		}
		if (request.corretoraId() != null) {
			Corretora corretora = corretoraRepository.findById(request.corretoraId())
				.orElseThrow(() -> new ResourceNotFoundException("Corretora informada não existe."));
			c.setCorretora(corretora);
		}
		if (request.saldoInicial() != null) {
			if (request.saldoInicial().compareTo(BigDecimal.ZERO) < 0) {
				throw new RegraNegocioException("Saldo inicial não pode ser negativo.");
			}
			if (transacaoRepository.existsByCarteira_Id(id)) {
				throw new RegraNegocioException(
						"O saldo inicial não pode ser alterado após a primeira operação financeira.");
			}
			c.setSaldoTotal(request.saldoInicial().setScale(2, RoundingMode.HALF_UP));
		}
		return toDetail(carteiraInvestimentoRepository.save(c));
	}

	@Transactional
	public void excluir(Long id, Long usuarioAutenticadoId) {
		CarteiraInvestimento c = carteiraInvestimentoRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada."));
		garantirDono(c, usuarioAutenticadoId);

		BigDecimal saldo = c.getSaldoTotal() != null ? c.getSaldoTotal() : BigDecimal.ZERO;
		if (saldo.compareTo(BigDecimal.ZERO) != 0) {
			throw new RegraNegocioException(
					"Não é possível excluir a carteira: o saldo precisa estar zerado antes da exclusão.");
		}

		BigDecimal totalAcoes = posicaoCarteiraRepository.somarQuantidadesPorCarteira(id);
		if (totalAcoes != null && totalAcoes.compareTo(BigDecimal.ZERO) != 0) {
			throw new RegraNegocioException(
					"Não é possível excluir a carteira: existem ações em posição. Venda todas as posições antes da exclusão.");
		}

		carteiraInvestimentoRepository.delete(c);
	}

	private static void garantirDono(CarteiraInvestimento c, Long usuarioAutenticadoId) {
		if (!c.getUsuario().getId().equals(usuarioAutenticadoId)) {
			throw new AccessDeniedException("Você não tem permissão para acessar esta carteira.");
		}
	}

	private CarteiraResponse toSummary(CarteiraInvestimento c) {
		Long corretoraId = c.getCorretora() != null ? c.getCorretora().getId() : null;
		return new CarteiraResponse(
				c.getId(),
				c.getUsuario().getId(),
				c.getNomeDaCarteira(),
				c.getSaldoTotal(),
				c.getLucroPrejuizoRealizado(),
				corretoraId,
				c.getDataCriacao(),
				transacaoRepository.existsByCarteira_Id(c.getId()),
				List.of());
	}

	private CarteiraResponse toDetail(CarteiraInvestimento c) {
		Long corretoraId = c.getCorretora() != null ? c.getCorretora().getId() : null;
		List<PosicaoCarteiraResponse> posicoes = c.getPosicoes().stream().map(this::mapPosicao).toList();
		return new CarteiraResponse(
				c.getId(),
				c.getUsuario().getId(),
				c.getNomeDaCarteira(),
				c.getSaldoTotal(),
				c.getLucroPrejuizoRealizado(),
				corretoraId,
				c.getDataCriacao(),
				transacaoRepository.existsByCarteira_Id(c.getId()),
				posicoes);
	}

	private PosicaoCarteiraResponse mapPosicao(PosicaoCarteira p) {
		var acao = p.getAcao();
		BigDecimal cotacao = acao.getCotacaoAtual() != null ? acao.getCotacaoAtual() : BigDecimal.ZERO;
		BigDecimal q = p.getQuantidade();
		BigDecimal pm = p.getPrecoMedioPonderado();
		BigDecimal valorMercado = q.multiply(cotacao, MC).setScale(2, RoundingMode.HALF_UP);
		BigDecimal custoTotal = q.multiply(pm, MC).setScale(2, RoundingMode.HALF_UP);
		return new PosicaoCarteiraResponse(
				acao.getId(),
				acao.getTicker(),
				acao.getNomeEmpresa(),
				q,
				pm,
				cotacao,
				valorMercado,
				custoTotal);
	}
}
