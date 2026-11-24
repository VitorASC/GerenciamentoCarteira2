package com.projeto.sistemabancario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.projeto.sistemabancario.domains.entity.EventoSeguranca;

public interface EventoSegurancaRepository extends JpaRepository<EventoSeguranca, Long>,
		JpaSpecificationExecutor<EventoSeguranca> {
}
