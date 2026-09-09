package com.projeto.sistemabancario.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.sistemabancario.domains.entity.Transacao;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

	boolean existsByAcao_Id(Long acaoId);
}
