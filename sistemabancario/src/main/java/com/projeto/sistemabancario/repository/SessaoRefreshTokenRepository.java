package com.projeto.sistemabancario.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.projeto.sistemabancario.domains.entity.SessaoRefreshToken;
import com.projeto.sistemabancario.domains.enums.MotivoRevogacao;

import jakarta.persistence.LockModeType;

public interface SessaoRefreshTokenRepository extends JpaRepository<SessaoRefreshToken, Long> {

	@Query("select s.usuario.id from SessaoRefreshToken s where s.tokenHash = :hash")
	Optional<Long> buscarTitular(String hash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<SessaoRefreshToken> findByTokenHash(String tokenHash);

	@Modifying
	@Query("""
			update SessaoRefreshToken s set s.revogadaEm = :agora, s.motivoRevogacao = :motivo
			where s.familiaId = :familia and s.revogadaEm is null
			""")
	void revogarFamilia(UUID familia, Instant agora, MotivoRevogacao motivo);

	@Modifying
	@Query("""
			update SessaoRefreshToken s set s.revogadaEm = :agora, s.motivoRevogacao = :motivo
			where s.usuario.id = :usuarioId and s.revogadaEm is null
			""")
	void revogarDoUsuario(Long usuarioId, Instant agora, MotivoRevogacao motivo);
}
