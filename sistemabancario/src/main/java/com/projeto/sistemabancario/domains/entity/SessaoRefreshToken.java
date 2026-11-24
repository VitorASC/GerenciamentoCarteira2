package com.projeto.sistemabancario.domains.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.projeto.sistemabancario.domains.enums.MotivoRevogacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sessoes_refresh_token")
public class SessaoRefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, updatable = false)
	private Usuario usuario;

	@Column(nullable = false, updatable = false)
	private UUID familiaId;

	@Column(nullable = false, unique = true, length = 64, updatable = false)
	private String tokenHash;

	@Column(nullable = false, updatable = false)
	private Instant criadaEm;

	@Column(nullable = false)
	private Instant ultimaAtividadeEm;

	@Column(nullable = false, updatable = false)
	private Instant expiraEm;

	private Instant utilizadaEm;
	private Instant revogadaEm;

	@Enumerated(EnumType.STRING)
	@Column(length = 24)
	private MotivoRevogacao motivoRevogacao;

	protected SessaoRefreshToken() {
	}

	public SessaoRefreshToken(Usuario usuario, UUID familiaId, String tokenHash, Instant agora, Instant expiraEm) {
		this.usuario = usuario;
		this.familiaId = familiaId;
		this.tokenHash = tokenHash;
		this.criadaEm = agora;
		this.ultimaAtividadeEm = agora;
		this.expiraEm = expiraEm;
	}

	public MotivoRevogacao impedimento(Instant agora, Duration inatividade) {
		if (revogadaEm != null) {
			return motivoRevogacao;
		}
		if (utilizadaEm != null) {
			return MotivoRevogacao.REUTILIZACAO;
		}
		if (!agora.isBefore(expiraEm)) {
			return MotivoRevogacao.EXPIRACAO_ABSOLUTA;
		}
		if (!agora.isBefore(ultimaAtividadeEm.plus(inatividade))) {
			return MotivoRevogacao.INATIVIDADE;
		}
		if (!usuario.getEstadoUsuario().isContaHabilitada()) {
			return MotivoRevogacao.USUARIO_INATIVO;
		}
		return null;
	}

	public void consumir(Instant agora) {
		utilizadaEm = agora;
		ultimaAtividadeEm = agora;
	}

	public UUID getFamiliaId() {
		return familiaId;
	}

	public Instant getExpiraEm() {
		return expiraEm;
	}

	public Usuario getUsuario() {
		return usuario;
	}
}
