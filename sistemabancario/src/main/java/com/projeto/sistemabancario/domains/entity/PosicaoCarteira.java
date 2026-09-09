package com.projeto.sistemabancario.domains.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "posicoes_carteira", uniqueConstraints = @UniqueConstraint(name = "uk_posicao_carteira_acao", columnNames = {
		"carteira_id", "acao_id"
}))
public class PosicaoCarteira {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "carteira_id", nullable = false)
	private CarteiraInvestimento carteira;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "acao_id", nullable = false)
	private Acao acao;

	@Column(nullable = false, precision = 19, scale = 8)
	private BigDecimal quantidade = BigDecimal.ZERO;

	@Column(nullable = false, precision = 19, scale = 6)
	private BigDecimal precoMedioPonderado = BigDecimal.ZERO;

	public PosicaoCarteira() {
	}

	public Long getId() {
		return id;
	}

	public CarteiraInvestimento getCarteira() {
		return carteira;
	}

	public void setCarteira(CarteiraInvestimento carteira) {
		this.carteira = carteira;
	}

	public Acao getAcao() {
		return acao;
	}

	public void setAcao(Acao acao) {
		this.acao = acao;
	}

	public BigDecimal getQuantidade() {
		return quantidade;
	}

	public void setQuantidade(BigDecimal quantidade) {
		this.quantidade = quantidade;
	}

	public BigDecimal getPrecoMedioPonderado() {
		return precoMedioPonderado;
	}

	public void setPrecoMedioPonderado(BigDecimal precoMedioPonderado) {
		this.precoMedioPonderado = precoMedioPonderado;
	}
}
