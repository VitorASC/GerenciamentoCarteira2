package com.projeto.sistemabancario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.sistemabancario.domains.entity.CarteiraInvestimento;

public interface CarteiraInvestimentoRepository extends JpaRepository<CarteiraInvestimento, Long> {

	List<CarteiraInvestimento> findByUsuario_Id(Long usuarioId);

	Page<CarteiraInvestimento> findByUsuario_Id(Long usuarioId, Pageable pageable);

	Optional<CarteiraInvestimento> findByIdAndUsuario_Id(Long id, Long usuarioId);

	boolean existsByCorretora_Id(Long corretoraId);
}
