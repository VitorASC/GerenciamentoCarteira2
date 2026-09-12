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

import com.projeto.sistemabancario.domains.entity.CarteiraInvestimento;
import com.projeto.sistemabancario.domains.entity.Usuario;
import com.projeto.sistemabancario.dto.request.CarteiraAtualizacaoRequest;
import com.projeto.sistemabancario.exception.RegraNegocioException;
import com.projeto.sistemabancario.repository.CarteiraInvestimentoRepository;
import com.projeto.sistemabancario.repository.CorretoraRepository;
import com.projeto.sistemabancario.repository.PosicaoCarteiraRepository;
import com.projeto.sistemabancario.repository.TransacaoRepository;
import com.projeto.sistemabancario.repository.UsuarioRepository;

class CarteiraServiceTest {

	private CarteiraInvestimentoRepository carteiras;
	private TransacaoRepository transacoes;
	private CarteiraService service;
	private CarteiraInvestimento carteira;

	@BeforeEach
	void setUp() {
		carteiras = mock(CarteiraInvestimentoRepository.class);
		transacoes = mock(TransacaoRepository.class);
		service = new CarteiraService(carteiras, mock(UsuarioRepository.class), mock(CorretoraRepository.class),
				mock(PosicaoCarteiraRepository.class), transacoes);

		Usuario usuario = new Usuario();
		ReflectionTestUtils.setField(usuario, "id", 7L);
		carteira = new CarteiraInvestimento();
		ReflectionTestUtils.setField(carteira, "id", 1L);
		carteira.setUsuario(usuario);
		carteira.setNomeDaCarteira("Principal");
		carteira.setSaldoTotal(new BigDecimal("100.00"));
		carteira.setLucroPrejuizoRealizado(BigDecimal.ZERO);

		when(carteiras.findById(1L)).thenReturn(Optional.of(carteira));
		when(carteiras.save(carteira)).thenReturn(carteira);
	}

	@Test
	void alteraSaldoInicialEnquantoCarteiraNaoPossuiOperacoes() {
		var resposta = service.atualizar(1L,
				new CarteiraAtualizacaoRequest(null, null, new BigDecimal("10000")), 7L);

		assertThat(carteira.getSaldoTotal()).isEqualByComparingTo("10000.00");
		assertThat(resposta.saldoTotal()).isEqualByComparingTo("10000.00");
	}

	@Test
	void impedeAlteracaoDeSaldoInicialDepoisDaPrimeiraOperacao() {
		when(transacoes.existsByCarteira_Id(1L)).thenReturn(true);

		assertThatThrownBy(() -> service.atualizar(1L,
				new CarteiraAtualizacaoRequest(null, null, new BigDecimal("10000")), 7L))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("primeira operação");
		assertThat(carteira.getSaldoTotal()).isEqualByComparingTo("100.00");
	}

	@Test
	void impedeSaldoInicialNegativo() {
		assertThatThrownBy(() -> service.atualizar(1L,
				new CarteiraAtualizacaoRequest(null, null, new BigDecimal("-0.01")), 7L))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("não pode ser negativo");
	}
}
