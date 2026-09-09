package com.projeto.sistemabancario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projeto.sistemabancario.domains.entity.PosicaoCarteira;

public interface PosicaoCarteiraRepository extends JpaRepository<PosicaoCarteira, Long> {

	Optional<PosicaoCarteira> findByCarteiraIdAndAcaoId(Long carteiraId, Long acaoId);

	@Query("SELECT p FROM PosicaoCarteira p JOIN FETCH p.acao WHERE p.carteira.id = :carteiraId")
	List<PosicaoCarteira> findAllByCarteiraIdWithAcao(@Param("carteiraId") Long carteiraId);

	boolean existsByAcao_Id(Long acaoId);

	@Query("SELECT COALESCE(SUM(p.quantidade), 0) FROM PosicaoCarteira p WHERE p.carteira.id = :carteiraId")
	java.math.BigDecimal somarQuantidadesPorCarteira(@Param("carteiraId") Long carteiraId);
}
