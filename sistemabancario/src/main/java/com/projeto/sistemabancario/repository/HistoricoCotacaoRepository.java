package com.projeto.sistemabancario.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.sistemabancario.domains.entity.HistoricoCotacao;

public interface HistoricoCotacaoRepository extends JpaRepository<HistoricoCotacao, Long> {

	Page<HistoricoCotacao> findByAcao_IdOrderByDataHoraAsc(Long acaoId, Pageable pageable);
}
