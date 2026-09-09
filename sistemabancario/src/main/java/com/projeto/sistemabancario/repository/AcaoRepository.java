package com.projeto.sistemabancario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.sistemabancario.domains.entity.Acao;

public interface AcaoRepository extends JpaRepository<Acao, Long> {

	Optional<Acao> findByTickerIgnoreCase(String ticker);

	boolean existsByTickerIgnoreCase(String ticker);

	boolean existsByCorretora_Id(Long corretoraId);
}
