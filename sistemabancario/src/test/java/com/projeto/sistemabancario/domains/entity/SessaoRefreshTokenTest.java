package com.projeto.sistemabancario.domains.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.projeto.sistemabancario.config.security.RefreshProperties;
import com.projeto.sistemabancario.domains.enums.MotivoRevogacao;

class SessaoRefreshTokenTest {
	private static final Instant INICIO = Instant.parse("2026-01-01T08:00:00Z");
	private static final Duration IDLE = Duration.ofMinutes(30);
	private static final Duration ABSOLUTA = Duration.ofHours(8);
	private final Usuario titular = new Usuario();
	private final SessaoRefreshToken sessao = new SessaoRefreshToken(titular, UUID.randomUUID(),
			"0".repeat(64), INICIO, INICIO.plus(ABSOLUTA));

	@Test
	void inatividadeComecaNoLimiteExato() {
		var limite = INICIO.plus(IDLE);
		assertThat(sessao.impedimento(limite.minusNanos(1), IDLE)).isNull();
		assertThat(sessao.impedimento(limite, IDLE)).isEqualTo(MotivoRevogacao.INATIVIDADE);
		assertThat(sessao.impedimento(limite.plusNanos(1), IDLE)).isEqualTo(MotivoRevogacao.INATIVIDADE);
	}

	@Test
	void limiteAbsolutoPrevaleceMesmoComInatividadeMaior() {
		var limite = INICIO.plus(ABSOLUTA);
		assertThat(sessao.impedimento(limite.minusNanos(1), Duration.ofDays(1))).isNull();
		assertThat(sessao.impedimento(limite, Duration.ofDays(1))).isEqualTo(MotivoRevogacao.EXPIRACAO_ABSOLUTA);
	}

	@Test
	void contaInativaEReplayImpedemRenovacao() {
		titular.setAtivo(false);
		assertThat(sessao.impedimento(INICIO, IDLE)).isEqualTo(MotivoRevogacao.USUARIO_INATIVO);
		titular.setAtivo(true);
		sessao.consumir(INICIO);
		assertThat(sessao.impedimento(INICIO, IDLE)).isEqualTo(MotivoRevogacao.REUTILIZACAO);
	}

	@Test
	void geracaoJaRevogadaPreservaOMotivoMesmoSeTiverSidoConsumida() {
		sessao.consumir(INICIO);
		// A revogação em lote é hidratada pelo JPA ao recarregar a geração.
		ReflectionTestUtils.setField(sessao, "revogadaEm", INICIO.plusSeconds(1));
		ReflectionTestUtils.setField(sessao, "motivoRevogacao", MotivoRevogacao.LOGOUT);
		assertThat(sessao.impedimento(INICIO.plus(ABSOLUTA), IDLE)).isEqualTo(MotivoRevogacao.LOGOUT);
	}

	@Test
	void duracoesDoRefreshDevemSerPositivas() {
		for (Duration invalida : new Duration[]{null, Duration.ZERO, Duration.ofSeconds(-1)}) {
			assertThatThrownBy(() -> new RefreshProperties(invalida, ABSOLUTA)).isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> new RefreshProperties(IDLE, invalida)).isInstanceOf(IllegalArgumentException.class);
		}
	}
}
