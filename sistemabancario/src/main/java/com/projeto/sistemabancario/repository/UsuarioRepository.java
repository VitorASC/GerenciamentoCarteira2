package com.projeto.sistemabancario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.projeto.sistemabancario.domains.entity.Usuario;
import com.projeto.sistemabancario.domains.enums.PerfilAcesso;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByCpf(String cpf);

	Optional<Usuario> findByEmailIgnoreCase(String email);

	boolean existsByCpf(String cpf);

	boolean existsByEmailIgnoreCase(String email);
	boolean existsByPerfilAcessoAndAtivoTrue(PerfilAcesso perfilAcesso);
	boolean existsByPerfilAcessoAndAtivoTrueAndIdNot(PerfilAcesso perfilAcesso, Long id);

	// A linha compartilhada serializa as mudanças administrativas mesmo quando ainda não há usuários.
	@Query(value = "SELECT id FROM controle_perfis_acesso WHERE id = 1 FOR UPDATE", nativeQuery = true)
	int bloquearAlteracoesAdministrativas();
}
