package com.projeto.sistemabancario.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import com.projeto.sistemabancario.domains.entity.Acao;
import com.projeto.sistemabancario.domains.entity.HistoricoCotacao;
import com.projeto.sistemabancario.domains.enums.Mercado;

@DataJpaTest
class HistoricoCotacaoRepositoryTest {

	@Autowired
	private HistoricoCotacaoRepository historicoCotacaoRepository;

	@Autowired
	private AcaoRepository acaoRepository;

	@Test
	void findByAcaoIdOrderByDataHoraAsc() {
		Acao acao = new Acao();
		acao.setTicker("TSTJPA");
		acao.setNomeEmpresa("Teste");
		acao.setMercado(Mercado.BRASIL);
		acao.setMoeda("BRL");

		HistoricoCotacao h1 = new HistoricoCotacao();
		h1.setAcao(acao);
		h1.setValor(new BigDecimal("10.00"));
		h1.setDataHora(LocalDateTime.of(2024, 1, 2, 12, 0));
		acao.getHistoricoCotacoes().add(h1);

		HistoricoCotacao h2 = new HistoricoCotacao();
		h2.setAcao(acao);
		h2.setValor(new BigDecimal("11.00"));
		h2.setDataHora(LocalDateTime.of(2024, 1, 1, 12, 0));
		acao.getHistoricoCotacoes().add(h2);

		acao = acaoRepository.saveAndFlush(acao);

		var page = historicoCotacaoRepository.findByAcao_IdOrderByDataHoraAsc(acao.getId(), PageRequest.of(0, 10));

		assertThat(page.getContent()).hasSize(2);
		assertThat(page.getContent().get(0).getDataHora()).isEqualTo(LocalDateTime.of(2024, 1, 1, 12, 0));
		assertThat(page.getContent().get(1).getDataHora()).isEqualTo(LocalDateTime.of(2024, 1, 2, 12, 0));
	}
}
