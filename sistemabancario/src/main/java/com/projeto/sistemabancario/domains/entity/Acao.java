package com.projeto.sistemabancario.domains.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.projeto.sistemabancario.domains.enums.Mercado;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "acoes", uniqueConstraints = @UniqueConstraint(name = "uk_acao_ticker", columnNames = "ticker"))
public class Acao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 32)
	private String ticker;

	private String nomeEmpresa;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private Mercado mercado;

	@Column(nullable = false, length = 8)
	private String moeda;

	@Column(precision = 19, scale = 6)
	private BigDecimal cotacaoAtual;

	private LocalDateTime dataHoraCotacao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "corretora_id")
	private Corretora corretora;

	@OneToMany(mappedBy = "acao", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<HistoricoCotacao> historicoCotacoes = new ArrayList<>();

	@OneToMany(mappedBy = "acao", fetch = FetchType.LAZY)
	private List<PosicaoCarteira> posicoes = new ArrayList<>();

	@OneToMany(mappedBy = "acao", fetch = FetchType.LAZY)
	private List<Transacao> transacoes = new ArrayList<>();

	public Acao() {
	}

	public Long getId() {
		return id;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public String getNomeEmpresa() {
		return nomeEmpresa;
	}

	public void setNomeEmpresa(String nomeEmpresa) {
		this.nomeEmpresa = nomeEmpresa;
	}

	public Mercado getMercado() {
		return mercado;
	}

	public void setMercado(Mercado mercado) {
		this.mercado = mercado;
	}

	public String getMoeda() {
		return moeda;
	}

	public void setMoeda(String moeda) {
		this.moeda = moeda;
	}

	public BigDecimal getCotacaoAtual() {
		return cotacaoAtual;
	}

	public void setCotacaoAtual(BigDecimal cotacaoAtual) {
		this.cotacaoAtual = cotacaoAtual;
	}

	public LocalDateTime getDataHoraCotacao() {
		return dataHoraCotacao;
	}

	public void setDataHoraCotacao(LocalDateTime dataHoraCotacao) {
		this.dataHoraCotacao = dataHoraCotacao;
	}

	public Corretora getCorretora() {
		return corretora;
	}

	public void setCorretora(Corretora corretora) {
		this.corretora = corretora;
	}

	public List<HistoricoCotacao> getHistoricoCotacoes() {
		return historicoCotacoes;
	}

	public List<PosicaoCarteira> getPosicoes() {
		return posicoes;
	}

	public List<Transacao> getTransacoes() {
		return transacoes;
	}
}
