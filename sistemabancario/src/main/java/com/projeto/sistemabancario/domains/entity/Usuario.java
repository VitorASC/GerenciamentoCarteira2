package com.projeto.sistemabancario.domains.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.projeto.sistemabancario.domains.enums.PerfilInvestidor;
import com.projeto.sistemabancario.domains.usuario.state.EstadoUsuario;
import com.projeto.sistemabancario.domains.usuario.state.EstadoUsuarioAtivo;
import com.projeto.sistemabancario.domains.usuario.state.EstadoUsuarioInativo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
		@UniqueConstraint(name = "uk_usuario_cpf", columnNames = "cpf"),
		@UniqueConstraint(name = "uk_usuario_email", columnNames = "email")
})
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nomeCompleto;

	@Column(nullable = false, length = 11)
	private String cpf;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private String senhaHash;

	private LocalDate dataNascimento;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private PerfilInvestidor perfilInvestidor;

	@Column(nullable = false)
	private LocalDateTime dataCriacao;

	@Column(nullable = false)
	private Boolean ativo = Boolean.TRUE;

	@OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CarteiraInvestimento> carteiras = new ArrayList<>();

	public Usuario() {
	}

	public Long getId() {
		return id;
	}

	public String getNomeCompleto() {
		return nomeCompleto;
	}

	public void setNomeCompleto(String nomeCompleto) {
		this.nomeCompleto = nomeCompleto;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public void setSenhaHash(String senhaHash) {
		this.senhaHash = senhaHash;
	}

	public LocalDate getDataNascimento() {
		return dataNascimento;
	}

	public void setDataNascimento(LocalDate dataNascimento) {
		this.dataNascimento = dataNascimento;
	}

	public PerfilInvestidor getPerfilInvestidor() {
		return perfilInvestidor;
	}

	public void setPerfilInvestidor(PerfilInvestidor perfilInvestidor) {
		this.perfilInvestidor = perfilInvestidor;
	}

	public LocalDateTime getDataCriacao() {
		return dataCriacao;
	}

	public void setDataCriacao(LocalDateTime dataCriacao) {
		this.dataCriacao = dataCriacao;
	}

	public EstadoUsuario getEstadoUsuario() {
		return Boolean.TRUE.equals(ativo) ? EstadoUsuarioAtivo.INSTANCE : EstadoUsuarioInativo.INSTANCE;
	}

	public void setEstadoUsuario(EstadoUsuario estado) {
		this.ativo = estado.isContaHabilitada();
	}

	public Boolean getAtivo() {
		return ativo;
	}

	public void setAtivo(Boolean ativo) {
		this.ativo = ativo;
	}

	public List<CarteiraInvestimento> getCarteiras() {
		return carteiras;
	}
}
