package com.projeto.sistemabancario.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.projeto.sistemabancario.AmbienteDeTeste;

import liquibase.integration.spring.SpringLiquibase;

@AmbienteDeTeste
@SpringBootTest
class LiquibaseMigrationIntegrationTest {

	private static final Set<String> DOMAIN_TABLES = Set.of(
			"USUARIOS",
			"CORRETORAS",
			"ACOES",
			"CARTEIRAS_INVESTIMENTO",
			"POSICOES_CARTEIRA",
			"TRANSACOES",
			"HISTORICO_COTACOES",
			"CONTROLE_PERFIS_ACESSO");

	private static final Set<String> CHANGESET_IDS = Set.of(
			"001-01-create-usuarios",
			"001-02-create-corretoras",
			"001-03-create-acoes",
			"001-04-create-carteiras-investimento",
			"001-05-create-posicoes-carteira",
			"001-06-create-transacoes",
			"001-07-create-historico-cotacoes",
			"001-08-add-unique-constraints",
			"001-09-add-foreign-keys",
			"002-01-add-perfil-acesso-usuarios",
			"003-01-controle-perfis-acesso");

	@Autowired
	private DataSource dataSource;

	@Autowired
	private SpringLiquibase springLiquibase;

	@Test
	void criaSchemaRegistraChangesetsELiberaLock() {
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);

		Set<String> tabelas = Set.copyOf(jdbc.queryForList(
				"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
				String.class));
		assertThat(tabelas).containsAll(DOMAIN_TABLES);
		assertThat(tabelas).contains("DATABASECHANGELOG", "DATABASECHANGELOGLOCK");

		Set<String> changesets = Set.copyOf(jdbc.queryForList(
				"SELECT ID FROM DATABASECHANGELOG",
				String.class));
		assertThat(changesets).isEqualTo(CHANGESET_IDS);
		assertThat(jdbc.queryForList("SELECT ID FROM CONTROLE_PERFIS_ACESSO", Integer.class)).containsExactly(1);
		assertThat(jdbc.queryForObject(
				"SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'USUARIOS' AND COLUMN_NAME = 'PERFIL_ACESSO'",
				String.class)).isEqualTo("NO");

		Boolean locked = jdbc.queryForObject(
				"SELECT LOCKED FROM DATABASECHANGELOGLOCK WHERE ID = 1",
				Boolean.class);
		assertThat(locked).isFalse();
	}

	@Test
	void segundaExecucaoNoMesmoBancoNaoReaplicaChangesets() throws Exception {
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		Integer registrosAntes = jdbc.queryForObject("SELECT COUNT(*) FROM DATABASECHANGELOG", Integer.class);
		Integer tabelasAntes = jdbc.queryForObject(
				"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
				Integer.class);

		springLiquibase.afterPropertiesSet();

		Integer registrosDepois = jdbc.queryForObject("SELECT COUNT(*) FROM DATABASECHANGELOG", Integer.class);
		Integer tabelasDepois = jdbc.queryForObject(
				"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
				Integer.class);
		Boolean locked = jdbc.queryForObject(
				"SELECT LOCKED FROM DATABASECHANGELOGLOCK WHERE ID = 1",
				Boolean.class);

		assertThat(registrosDepois).isEqualTo(registrosAntes).isEqualTo(CHANGESET_IDS.size());
		assertThat(tabelasDepois).isEqualTo(tabelasAntes);
		assertThat(locked).isFalse();
	}
}
