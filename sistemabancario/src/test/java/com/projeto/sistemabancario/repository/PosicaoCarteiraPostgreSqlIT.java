package com.projeto.sistemabancario.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.projeto.sistemabancario.AmbienteDeTeste;
import com.projeto.sistemabancario.domains.entity.Acao;
import com.projeto.sistemabancario.domains.entity.CarteiraInvestimento;
import com.projeto.sistemabancario.domains.entity.PosicaoCarteira;
import com.projeto.sistemabancario.domains.entity.Usuario;
import com.projeto.sistemabancario.domains.enums.Mercado;
import com.projeto.sistemabancario.domains.enums.PerfilInvestidor;

@AmbienteDeTeste
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PosicaoCarteiraPostgreSqlIT.BancoDeTeste.class)
class PosicaoCarteiraPostgreSqlIT {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private PosicaoCarteiraRepository posicoes;

	@Test
	void recuperaPosicaoComPrecisaoERejeitaSegundaPosicaoDaMesmaAcaoNaCarteira() {
		CarteiraInvestimento carteira = persistirCarteira();
		Acao acao = new Acao();
		acao.setTicker("TSTPG");
		acao.setNomeEmpresa("Empresa fictícia");
		acao.setMercado(Mercado.BRASIL);
		acao.setMoeda("BRL");
		entityManager.persist(acao);

		PosicaoCarteira original = novaPosicao(carteira, acao);
		BigDecimal quantidade = new BigDecimal("1.12345678");
		BigDecimal precoMedio = new BigDecimal("23.456789");
		original.setQuantidade(quantidade);
		original.setPrecoMedioPonderado(precoMedio);
		posicoes.saveAndFlush(original);
		entityManager.clear();

		PosicaoCarteira recuperada = posicoes.findByCarteiraIdAndAcaoId(carteira.getId(), acao.getId()).orElseThrow();
		assertThat(recuperada.getId()).isEqualTo(original.getId());
		assertThat(recuperada.getQuantidade()).isEqualByComparingTo(quantidade);
		assertThat(recuperada.getPrecoMedioPonderado()).isEqualByComparingTo(precoMedio);

		PosicaoCarteira duplicada = novaPosicao(recuperada.getCarteira(), recuperada.getAcao());
		var falha = assertThrows(DataIntegrityViolationException.class, () -> posicoes.saveAndFlush(duplicada));

		var causa = assertInstanceOf(PSQLException.class, falha.getMostSpecificCause());
		assertThat(causa.getSQLState()).isEqualTo("23505");
		assertThat(causa.getServerErrorMessage().getConstraint()).isEqualTo("uk_posicao_carteira_acao");
	}

	private CarteiraInvestimento persistirCarteira() {
		LocalDateTime criacao = LocalDateTime.of(2026, 1, 1, 12, 0);
		Usuario usuario = new Usuario();
		usuario.setNomeCompleto("Titular fictício");
		usuario.setCpf("00000000000");
		usuario.setEmail("titular@example.test");
		usuario.setSenhaHash("hash-ficticio-de-teste");
		usuario.setPerfilInvestidor(PerfilInvestidor.MODERADO);
		usuario.setDataCriacao(criacao);
		entityManager.persist(usuario);

		CarteiraInvestimento carteira = new CarteiraInvestimento();
		carteira.setUsuario(usuario);
		carteira.setNomeDaCarteira("Carteira de teste PostgreSQL");
		carteira.setDataCriacao(criacao);
		return entityManager.persist(carteira);
	}

	private static PosicaoCarteira novaPosicao(CarteiraInvestimento carteira, Acao acao) {
		PosicaoCarteira posicao = new PosicaoCarteira();
		posicao.setCarteira(carteira);
		posicao.setAcao(acao);
		return posicao;
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class BancoDeTeste {

		@Bean
		@ServiceConnection
		PostgreSQLContainer postgres() {
			return new PostgreSQLContainer("postgres:17-alpine").withPassword(UUID.randomUUID().toString());
		}
	}
}
