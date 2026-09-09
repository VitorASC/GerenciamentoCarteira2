package com.projeto.sistemabancario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.sistemabancario.domains.entity.Corretora;

public interface CorretoraRepository extends JpaRepository<Corretora, Long> {

	Optional<Corretora> findByCnpj(String cnpj);

	boolean existsByCnpj(String cnpj);
}
