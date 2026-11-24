package com.projeto.sistemabancario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;

import com.projeto.sistemabancario.domains.entity.Usuario;
import com.projeto.sistemabancario.domains.enums.PerfilAcesso;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Usuario u where u.id = :id")
	Optional<Usuario> bloquearPorId(Long id);

	Optional<Usuario> findByCpf(String cpf);

	Optional<Usuario> findByEmailIgnoreCase(String email);

	@Transactional
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Usuario u where lower(u.email) = lower(:email)")
	Optional<Usuario> bloquearPorEmail(String email);

	boolean existsByCpf(String cpf);

	boolean existsByEmailIgnoreCase(String email);
	boolean existsByPerfilAcessoAndAtivoTrue(PerfilAcesso perfilAcesso);
	boolean existsByPerfilAcessoAndAtivoTrueAndIdNot(PerfilAcesso perfilAcesso, Long id);

	// A linha compartilhada serializa as mudanças administrativas mesmo quando ainda não há usuários.
	@Query(value = "SELECT id FROM controle_perfis_acesso WHERE id = 1 FOR UPDATE", nativeQuery = true)
	int bloquearAlteracoesAdministrativas();
}
