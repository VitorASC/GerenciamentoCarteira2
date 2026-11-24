package com.projeto.sistemabancario.repository;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.projeto.sistemabancario.domains.entity.ProtecaoAutenticacao;

import jakarta.persistence.LockModeType;
import jakarta.persistence.EntityManager;

@Repository
public class ProtecaoAutenticacaoRepository {
	private final JdbcTemplate jdbc;
	private final EntityManager entidades;
	private final String inserirSeAusente;

	public ProtecaoAutenticacaoRepository(JdbcTemplate jdbc, EntityManager entidades) {
		this.jdbc = jdbc;
		this.entidades = entidades;
		this.inserirSeAusente = jdbc.execute((ConnectionCallback<String>) conexao ->
				switch (conexao.getMetaData().getDatabaseProductName()) {
					case "PostgreSQL" -> """
							insert into protecao_autenticacao(chave_login) values (?)
							on conflict (chave_login) do nothing
							""";
					case "H2" -> "merge into protecao_autenticacao(chave_login) key(chave_login) values (?)";
					case "MySQL" -> """
							insert into protecao_autenticacao(chave_login) values (?)
							on duplicate key update chave_login = chave_login
							""";
					default -> throw new IllegalStateException("Banco sem suporte à proteção de login");
				});
	}

	public ProtecaoAutenticacao bloquear(String chave) {
		// JDBC e JPA compartilham a transação. A criação nunca substitui o contador existente.
		jdbc.update(inserirSeAusente, chave);
		return entidades.find(ProtecaoAutenticacao.class, chave, LockModeType.PESSIMISTIC_WRITE);
	}
}
