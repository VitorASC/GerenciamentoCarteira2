package com.projeto.sistemabancario.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import liquibase.integration.spring.SpringLiquibase;

import com.projeto.sistemabancario.AmbienteDeTeste;
import com.projeto.sistemabancario.web.CenarioSessaoHttp;

@AmbienteDeTeste
@SpringBootTest
@AutoConfigureMockMvc
@Import(PosicaoCarteiraPostgreSqlIT.BancoDeTeste.class)
class SessaoRefreshPostgreSqlIT extends CenarioSessaoHttp {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private SpringLiquibase liquibase;

	@Test
	void segundaExecucaoPreservaOsChangesetsNoPostgreSql() throws Exception {
		var historico = "select id, dateexecuted, md5sum from databasechangelog order by orderexecuted";
		var antes = jdbc.queryForList(historico);
		assertThat(antes).extracting(linha -> linha.get("id")).containsOnlyOnce("004-01-sessoes-refresh-token");
		liquibase.afterPropertiesSet();
		assertThat(jdbc.queryForList(historico)).isEqualTo(antes);
		assertThat(jdbc.queryForObject("select locked from databasechangeloglock where id = 1", Boolean.class)).isFalse();
	}

	@Test
	void duasRenovacoesSimultaneasProduzemUmParEReplayEncerraACadeia() throws Exception {
		var inicial = login(cadastrar("30000000016"));
		var respostas = simultaneas(() -> renovar(inicial), () -> renovar(inicial));
		assertThat(respostas).extracting(r -> r.andReturn().getResponse().getStatus())
				.containsExactlyInAnyOrder(200, 401);
		for (var resposta : respostas) {
			if (resposta.andReturn().getResponse().getStatus() == 200) {
				verificarRecusa(renovar(par(resposta)));
			}
			else {
				verificarRecusa(resposta);
			}
		}
		assertThat(jdbc.queryForObject("""
				select count(*) from sessoes_refresh_token
				where motivo_revogacao = 'REUTILIZACAO' and revogada_em is not null
				""", Integer.class)).isEqualTo(2);
	}

	@Test
	void logoutGlobalConcorrenteNaoDeixaGeracaoAtiva() throws Exception {
		String email = cadastrar("30000000017");
		var inicial = login(email);
		var respostas = simultaneas(() -> renovar(inicial), () -> mvc.perform(post("/auth/logout-global")
				.header("Authorization", "Bearer " + inicial.get("accessToken").asText())));
		var tentativa = respostas.get(0);
		assertThat(respostas.get(1).andReturn().getResponse().getStatus()).isEqualTo(204);
		if (tentativa.andReturn().getResponse().getStatus() == 200) {
			verificarRecusa(renovar(par(tentativa)));
		}
		else {
			verificarRecusa(tentativa);
		}
		assertThat(jdbc.queryForObject("""
				select count(*) from sessoes_refresh_token s join usuarios u on u.id = s.usuario_id
				where u.email = ? and s.revogada_em is null
				""", Integer.class, email)).isZero();
	}

	private List<ResultActions> simultaneas(Callable<ResultActions> a, Callable<ResultActions> b) throws Exception {
		var largada = new CyclicBarrier(2);
		var executor = Executors.newFixedThreadPool(2);
		try {
			var primeira = executor.submit(() -> {
				largada.await(10, TimeUnit.SECONDS);
				return a.call();
			});
			var segunda = executor.submit(() -> {
				largada.await(10, TimeUnit.SECONDS);
				return b.call();
			});
			return List.of(primeira.get(30, TimeUnit.SECONDS), segunda.get(30, TimeUnit.SECONDS));
		}
		finally {
			executor.shutdownNow();
		}
	}
}
