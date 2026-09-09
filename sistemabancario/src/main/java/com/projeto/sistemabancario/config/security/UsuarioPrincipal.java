package com.projeto.sistemabancario.config.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.projeto.sistemabancario.domains.entity.Usuario;

public class UsuarioPrincipal implements UserDetails {

	private static final long serialVersionUID = 1L;

	private final Long usuarioId;
	private final String email;
	private final String senhaHash;
	private final boolean ativo;

	public UsuarioPrincipal(Long usuarioId, String email, String senhaHash, boolean ativo) {
		this.usuarioId = usuarioId;
		this.email = email;
		this.senhaHash = senhaHash;
		this.ativo = ativo;
	}

	public static UsuarioPrincipal fromEntity(Usuario u) {
		return new UsuarioPrincipal(u.getId(), u.getEmail(), u.getSenhaHash(), u.getEstadoUsuario().isContaHabilitada());
	}

	public static UsuarioPrincipal fromJwt(Long usuarioId, String email, boolean ativo) {
		return new UsuarioPrincipal(usuarioId, email, "", ativo);
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}

	@Override
	public String getPassword() {
		return senhaHash != null ? senhaHash : "";
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return ativo;
	}
}
