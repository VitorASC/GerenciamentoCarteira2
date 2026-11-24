package com.projeto.sistemabancario.domains.entity;

import java.time.Instant;

import com.projeto.sistemabancario.config.security.ProtecaoLoginProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "protecao_autenticacao")
public class ProtecaoAutenticacao {

	@Id
	@Column(length = 64)
	private String chaveLogin;

	@Column(nullable = false)
	private int falhasConsecutivas;
	private Instant janelaIniciadaEm;
	private Instant bloqueadaAte;

	public boolean estaBloqueada(Instant agora) {
		return bloqueadaAte != null && agora.isBefore(bloqueadaAte);
	}

	public boolean registrarFalha(Instant agora, ProtecaoLoginProperties politica) {
		if (estaBloqueada(agora)) {
			return false;
		}
		if (bloqueadaAte != null || janelaIniciadaEm == null
				|| !agora.isBefore(janelaIniciadaEm.plus(politica.failureWindow()))) {
			limpar();
			janelaIniciadaEm = agora;
		}
		falhasConsecutivas++;
		if (falhasConsecutivas >= politica.maxFailedAttempts()) {
			bloqueadaAte = agora.plus(politica.lockDuration());
			return true;
		}
		return false;
	}

	public void limpar() {
		falhasConsecutivas = 0;
		janelaIniciadaEm = null;
		bloqueadaAte = null;
	}

	public int getFalhasConsecutivas() {
		return falhasConsecutivas;
	}
}
