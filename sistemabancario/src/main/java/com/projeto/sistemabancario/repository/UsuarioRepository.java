package com.projeto.sistemabancario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.sistemabancario.domains.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByCpf(String cpf);

	Optional<Usuario> findByEmailIgnoreCase(String email);

	boolean existsByCpf(String cpf);

	boolean existsByEmailIgnoreCase(String email);
}
