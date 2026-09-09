package com.projeto.sistemabancario.domains.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "carteiras_investimento")
public class CarteiraInvestimento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(nullable = false)
	private String nomeDaCarteira;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal saldoTotal = BigDecimal.ZERO;

	@Column(precision = 19, scale = 6)
	private BigDecimal rentabilidadeAcumulada = BigDecimal.ZERO;

	@Column(nullable = false)
	private LocalDateTime dataCriacao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "corretora_id")
	private Corretora corretora;

	@OneToMany(mappedBy = "carteira", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PosicaoCarteira> posicoes = new ArrayList<>();

	@OneToMany(mappedBy = "carteira", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Transacao> transacoes = new ArrayList<>();

	public CarteiraInvestimento() {
	}

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public String getNomeDaCarteira() {
		return nomeDaCarteira;
	}

	public void setNomeDaCarteira(String nomeDaCarteira) {
		this.nomeDaCarteira = nomeDaCarteira;
	}

	public BigDecimal getSaldoTotal() {
		return saldoTotal;
	}

	public void setSaldoTotal(BigDecimal saldoTotal) {
		this.saldoTotal = saldoTotal;
	}

	public BigDecimal getRentabilidadeAcumulada() {
		return rentabilidadeAcumulada;
	}

	public void setRentabilidadeAcumulada(BigDecimal rentabilidadeAcumulada) {
		this.rentabilidadeAcumulada = rentabilidadeAcumulada;
	}

	public LocalDateTime getDataCriacao() {
		return dataCriacao;
	}

	public void setDataCriacao(LocalDateTime dataCriacao) {
		this.dataCriacao = dataCriacao;
	}

	public Corretora getCorretora() {
		return corretora;
	}

	public void setCorretora(Corretora corretora) {
		this.corretora = corretora;
	}

	public List<PosicaoCarteira> getPosicoes() {
		return posicoes;
	}

	public List<Transacao> getTransacoes() {
		return transacoes;
	}
}
