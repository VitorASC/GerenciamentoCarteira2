package com.projeto.sistemabancario.domains.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.projeto.sistemabancario.domains.enums.TipoTransacao;

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
@Table(name = "transacoes")
public class Transacao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "carteira_id", nullable = false)
	private CarteiraInvestimento carteira;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "acao_id", nullable = false)
	private Acao acao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private TipoTransacao tipo;

	@Column(nullable = false, precision = 19, scale = 8)
	private BigDecimal quantidade;

	@Column(nullable = false, precision = 19, scale = 6)
	private BigDecimal precoUnitario;

	@Column(nullable = false)
	private LocalDateTime dataHora;

	public Transacao() {
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

	public TipoTransacao getTipo() {
		return tipo;
	}

	public void setTipo(TipoTransacao tipo) {
		this.tipo = tipo;
	}

	public BigDecimal getQuantidade() {
		return quantidade;
	}

	public void setQuantidade(BigDecimal quantidade) {
		this.quantidade = quantidade;
	}

	public BigDecimal getPrecoUnitario() {
		return precoUnitario;
	}

	public void setPrecoUnitario(BigDecimal precoUnitario) {
		this.precoUnitario = precoUnitario;
	}

	public LocalDateTime getDataHora() {
		return dataHora;
	}

	public void setDataHora(LocalDateTime dataHora) {
		this.dataHora = dataHora;
	}
}
