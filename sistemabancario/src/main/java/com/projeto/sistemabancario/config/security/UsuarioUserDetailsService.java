package com.projeto.sistemabancario.config.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.projeto.sistemabancario.repository.UsuarioRepository;

@Service
public class UsuarioUserDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	public UsuarioUserDetailsService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return usuarioRepository.findByEmailIgnoreCase(username.trim())
			.map(UsuarioPrincipal::fromEntity)
			.orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
	}
}
