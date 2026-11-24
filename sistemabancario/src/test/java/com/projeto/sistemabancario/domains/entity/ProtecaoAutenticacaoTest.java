package com.projeto.sistemabancario.domains.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;

import com.projeto.sistemabancario.config.security.ProtecaoLoginProperties;
import com.projeto.sistemabancario.domains.enums.TipoEventoSeguranca;

class ProtecaoAutenticacaoTest {
	private static final Instant INICIO = Instant.parse("2026-01-01T12:00:00Z");
	private static final Duration JANELA = Duration.ofMinutes(15);
	private static final Duration BLOQUEIO = Duration.ofMinutes(5);
	private static final ProtecaoLoginProperties POLITICA = new ProtecaoLoginProperties(3, JANELA, BLOQUEIO);
	private final ProtecaoAutenticacao protecao = new ProtecaoAutenticacao();

	@Test
	void ativaExatamenteNoLimiarENaoEstendeBloqueio() {
		assertThat(protecao.registrarFalha(INICIO, POLITICA)).isFalse();
		assertThat(protecao.getFalhasConsecutivas()).isOne();
		assertThat(protecao.registrarFalha(INICIO.plusSeconds(1), POLITICA)).isFalse();
		assertThat(protecao.getFalhasConsecutivas()).isEqualTo(2);
		Instant terceira = INICIO.plusSeconds(2);
		assertThat(protecao.registrarFalha(terceira, POLITICA)).isTrue();
		Instant fim = terceira.plus(BLOQUEIO);
		assertThat(protecao.estaBloqueada(fim.minusNanos(1))).isTrue();
		assertThat(protecao.registrarFalha(fim.minusNanos(1), POLITICA)).isFalse();
		assertThat(protecao.getFalhasConsecutivas()).isEqualTo(3);
		assertThat(protecao.estaBloqueada(fim)).isFalse();
		assertThat(protecao.registrarFalha(fim, POLITICA)).isFalse();
		assertThat(protecao.getFalhasConsecutivas()).isOne();
	}

	@Test
	void janelaTerminaNoLimiteMesmoSeHouveFalhaRecente() {
		protecao.registrarFalha(INICIO, POLITICA);
		protecao.registrarFalha(INICIO.plus(JANELA).minusNanos(1), POLITICA);
		assertThat(protecao.registrarFalha(INICIO.plus(JANELA), POLITICA)).isFalse();
		assertThat(protecao.getFalhasConsecutivas()).isOne();
	}

	@Test
	void sucessoReiniciaContagem() {
		protecao.registrarFalha(INICIO, POLITICA);
		protecao.limpar();
		assertThat(protecao.getFalhasConsecutivas()).isZero();
		protecao.registrarFalha(INICIO.plusSeconds(10), POLITICA);
		assertThat(protecao.getFalhasConsecutivas()).isOne();
	}

	@Test
	void rejeitaConfiguracaoInvalida() {
		assertThatThrownBy(() -> new ProtecaoLoginProperties(0, JANELA, BLOQUEIO))
				.isInstanceOf(IllegalArgumentException.class);
		for (Duration invalida : new Duration[] {null, Duration.ZERO, Duration.ofSeconds(-1)}) {
			assertThatThrownBy(() -> new ProtecaoLoginProperties(1, invalida, BLOQUEIO))
					.isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> new ProtecaoLoginProperties(1, JANELA, invalida))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	void eventoRecusaHashInvalidoEDescartaOrigemComQuebraDeLinha() {
		assertThatThrownBy(() -> new EventoSeguranca(TipoEventoSeguranca.LOGIN_FALHA, INICIO, null, null,
				"f".repeat(63) + "\n", null, null)).isInstanceOf(IllegalArgumentException.class);
		var evento = new EventoSeguranca(TipoEventoSeguranca.LOGIN_FALHA, INICIO, null, null,
				"f".repeat(64), "127.0.0.1\r\nLOGIN_SUCESSO", null);
		assertThat(evento.getEnderecoOrigem()).isNull();
	}

	@Test
	void configuracaoExternaInvalidaImpedeInicializacao() {
		var contexto = new ApplicationContextRunner().withUserConfiguration(Configuracao.class);
		for (String propriedade : new String[] {"max-failed-attempts=0", "failure-window=PT0S", "lock-duration=-PT1S"}) {
			contexto.withPropertyValues("app.security.login." + propriedade)
					.run(aplicacao -> assertThat(aplicacao).hasFailed());
		}
		contexto.withPropertyValues("app.security.login.max-failed-attempts=2",
				"app.security.login.failure-window=PT3M", "app.security.login.lock-duration=PT1M")
				.run(aplicacao -> assertThat(aplicacao.getBean(ProtecaoLoginProperties.class))
						.isEqualTo(new ProtecaoLoginProperties(2, Duration.ofMinutes(3), Duration.ofMinutes(1))));
	}

	@TestConfiguration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ProtecaoLoginProperties.class)
	static class Configuracao {
	}
}
