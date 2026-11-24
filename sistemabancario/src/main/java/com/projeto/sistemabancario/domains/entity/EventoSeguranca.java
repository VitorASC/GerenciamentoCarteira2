package com.projeto.sistemabancario.domains.entity;

import java.time.Instant;
import java.util.regex.Pattern;

import com.projeto.sistemabancario.domains.enums.MotivoRevogacao;
import com.projeto.sistemabancario.domains.enums.TipoEventoSeguranca;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "eventos_seguranca")
public class EventoSeguranca {

	private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
	private static final Pattern ORIGEM = Pattern.compile("[0-9a-fA-F:.]{1,45}");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private TipoEventoSeguranca tipo;
	@Column(nullable = false)
	private Instant ocorridoEm;
	private Long atorUsuarioId;
	private Long alvoUsuarioId;
	@Column(length = 64)
	private String identificadorHash;
	@Column(length = 45)
	private String enderecoOrigem;
	@Enumerated(EnumType.STRING)
	@Column(length = 24)
	private MotivoRevogacao motivoRevogacao;

	protected EventoSeguranca() {
	}

	public EventoSeguranca(TipoEventoSeguranca tipo, Instant ocorridoEm, Long ator, Long alvo,
			String identificadorHash, String origem, MotivoRevogacao motivo) {
		if (identificadorHash != null && !HASH.matcher(identificadorHash).matches()) {
			throw new IllegalArgumentException("Formato de identificador inválido");
		}
		this.tipo = tipo;
		this.ocorridoEm = ocorridoEm;
		this.atorUsuarioId = ator;
		this.alvoUsuarioId = alvo;
		this.identificadorHash = identificadorHash;
		this.enderecoOrigem = origem != null && ORIGEM.matcher(origem).matches() ? origem : null;
		this.motivoRevogacao = motivo;
	}

	public Long getId() { return id; }
	public TipoEventoSeguranca getTipo() { return tipo; }
	public Instant getOcorridoEm() { return ocorridoEm; }
	public Long getAtorUsuarioId() { return atorUsuarioId; }
	public Long getAlvoUsuarioId() { return alvoUsuarioId; }
	public String getEnderecoOrigem() { return enderecoOrigem; }
	public MotivoRevogacao getMotivoRevogacao() { return motivoRevogacao; }
}
