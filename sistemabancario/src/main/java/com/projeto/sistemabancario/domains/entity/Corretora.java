package com.projeto.sistemabancario.domains.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "corretoras", uniqueConstraints = @UniqueConstraint(name = "uk_corretora_cnpj", columnNames = "cnpj"))
public class Corretora {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 14)
	private String cnpj;

	@Column(nullable = false, length = 1000)
	private String razaoSocial;

	@Column(length = 1000)
	private String nomeFantasia;

	@Column(length = 512)
	private String email;

	@Column(length = 128)
	private String telefone;

	@Column(length = 8)
	private String cep;

	@Column(length = 1000)
	private String logradouro;

	@Column(length = 64)
	private String numero;

	@Column(length = 1000)
	private String complemento;

	@Column(length = 512)
	private String bairro;

	@Column(length = 512)
	private String cidade;

	@Column(length = 2)
	private String uf;

	@Column(length = 512)
	private String situacaoCadastral;

	@Column(nullable = false)
	private Boolean validadaNaCvm = Boolean.FALSE;

	@Column(nullable = false)
	private LocalDateTime dataCadastro;

	@OneToMany(mappedBy = "corretora", fetch = FetchType.LAZY)
	private List<Acao> acoes = new ArrayList<>();

	@OneToMany(mappedBy = "corretora", fetch = FetchType.LAZY)
	private List<CarteiraInvestimento> carteiras = new ArrayList<>();

	public Corretora() {
	}

	public Long getId() {
		return id;
	}

	public String getCnpj() {
		return cnpj;
	}

	public void setCnpj(String cnpj) {
		this.cnpj = cnpj;
	}

	public String getRazaoSocial() {
		return razaoSocial;
	}

	public void setRazaoSocial(String razaoSocial) {
		this.razaoSocial = razaoSocial;
	}

	public String getNomeFantasia() {
		return nomeFantasia;
	}

	public void setNomeFantasia(String nomeFantasia) {
		this.nomeFantasia = nomeFantasia;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public String getCep() {
		return cep;
	}

	public void setCep(String cep) {
		this.cep = cep;
	}

	public String getLogradouro() {
		return logradouro;
	}

	public void setLogradouro(String logradouro) {
		this.logradouro = logradouro;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public String getComplemento() {
		return complemento;
	}

	public void setComplemento(String complemento) {
		this.complemento = complemento;
	}

	public String getBairro() {
		return bairro;
	}

	public void setBairro(String bairro) {
		this.bairro = bairro;
	}

	public String getCidade() {
		return cidade;
	}

	public void setCidade(String cidade) {
		this.cidade = cidade;
	}

	public String getUf() {
		return uf;
	}

	public void setUf(String uf) {
		this.uf = uf;
	}

	public String getSituacaoCadastral() {
		return situacaoCadastral;
	}

	public void setSituacaoCadastral(String situacaoCadastral) {
		this.situacaoCadastral = situacaoCadastral;
	}

	public Boolean getValidadaNaCvm() {
		return validadaNaCvm;
	}

	public void setValidadaNaCvm(Boolean validadaNaCvm) {
		this.validadaNaCvm = validadaNaCvm;
	}

	public LocalDateTime getDataCadastro() {
		return dataCadastro;
	}

	public void setDataCadastro(LocalDateTime dataCadastro) {
		this.dataCadastro = dataCadastro;
	}

	public List<Acao> getAcoes() {
		return acoes;
	}

	public List<CarteiraInvestimento> getCarteiras() {
		return carteiras;
	}
}
