package com.projeto.sistemabancario.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.sistemabancario.dto.request.UsuarioAtualizacaoRequest;
import com.projeto.sistemabancario.dto.request.UsuarioCadastroRequest;
import com.projeto.sistemabancario.dto.response.UsuarioResponse;
import com.projeto.sistemabancario.exception.DuplicateResourceException;
import com.projeto.sistemabancario.exception.RegraNegocioException;
import com.projeto.sistemabancario.exception.ResourceNotFoundException;
import com.projeto.sistemabancario.domains.entity.Usuario;
import com.projeto.sistemabancario.domains.usuario.state.EstadoUsuarioAtivo;
import com.projeto.sistemabancario.domains.usuario.state.EstadoUsuarioInativo;
import com.projeto.sistemabancario.repository.UsuarioRepository;
import com.projeto.sistemabancario.util.Documentos;

@Service
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UsuarioResponse cadastrar(UsuarioCadastroRequest request) {
		String cpf = Documentos.apenasDigitos(request.cpf());
		if (cpf.length() != 11) {
			throw new RegraNegocioException("CPF deve conter 11 dígitos.");
		}
		if (usuarioRepository.existsByCpf(cpf)) {
			throw new DuplicateResourceException("Já existe usuário com este CPF.");
		}
		if (usuarioRepository.existsByEmailIgnoreCase(request.email().trim())) {
			throw new DuplicateResourceException("Já existe usuário com este e-mail.");
		}

		Usuario u = new Usuario();
		u.setNomeCompleto(request.nomeCompleto().trim());
		u.setCpf(cpf);
		u.setEmail(request.email().trim().toLowerCase());
		u.setSenhaHash(passwordEncoder.encode(request.senha()));
		u.setDataNascimento(request.dataNascimento());
		u.setPerfilInvestidor(request.perfilInvestidor());
		u.setDataCriacao(LocalDateTime.now());
		u.setEstadoUsuario(EstadoUsuarioAtivo.INSTANCE);

		return toResponse(usuarioRepository.save(u));
	}

	@Transactional(readOnly = true)
	public Page<UsuarioResponse> listar(Pageable pageable) {
		throw new AccessDeniedException("Listagem de usuários não permitida.");
	}

	@Transactional(readOnly = true)
	public UsuarioResponse buscarPorId(Long id, Long usuarioAutenticadoId) {
		if (!id.equals(usuarioAutenticadoId)) {
			throw new AccessDeniedException("Você só pode consultar o próprio cadastro.");
		}
		return buscarPorIdInterno(id);
	}

	@Transactional(readOnly = true)
	public UsuarioResponse buscarPorCpf(String cpfBruto, Long usuarioAutenticadoId) {
		String cpf = Documentos.apenasDigitos(cpfBruto);
		Usuario alvo = usuarioRepository.findByCpf(cpf)
			.orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado para o CPF informado."));
		if (!alvo.getId().equals(usuarioAutenticadoId)) {
			throw new AccessDeniedException("Você só pode consultar o próprio CPF.");
		}
		return toResponse(alvo);
	}

	@Transactional
	public UsuarioResponse atualizar(Long id, UsuarioAtualizacaoRequest request, Long usuarioAutenticadoId) {
		if (!id.equals(usuarioAutenticadoId)) {
			throw new AccessDeniedException("Você só pode alterar o próprio cadastro.");
		}
		return atualizar(id, request);
	}

	private UsuarioResponse buscarPorIdInterno(Long id) {
		return toResponse(usuarioRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado.")));
	}

	@Transactional
	public UsuarioResponse atualizar(Long id, UsuarioAtualizacaoRequest request) {
		Usuario u = usuarioRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
		if (request.nomeCompleto() != null && !request.nomeCompleto().isBlank()) {
			u.setNomeCompleto(request.nomeCompleto().trim());
		}
		if (request.email() != null && !request.email().isBlank()) {
			String email = request.email().trim().toLowerCase();
			if (!email.equalsIgnoreCase(u.getEmail()) && usuarioRepository.existsByEmailIgnoreCase(email)) {
				throw new DuplicateResourceException("Já existe usuário com este e-mail.");
			}
			u.setEmail(email);
		}
		if (request.perfilInvestidor() != null) {
			u.setPerfilInvestidor(request.perfilInvestidor());
		}
		if (request.ativo() != null) {
			u.setEstadoUsuario(Boolean.TRUE.equals(request.ativo())
					? EstadoUsuarioAtivo.INSTANCE
					: EstadoUsuarioInativo.INSTANCE);
		}
		return toResponse(usuarioRepository.save(u));
	}

	private UsuarioResponse toResponse(Usuario u) {
		return new UsuarioResponse(
				u.getId(),
				u.getNomeCompleto(),
				u.getCpf(),
				u.getEmail(),
				u.getDataNascimento(),
				u.getPerfilInvestidor(),
				u.getDataCriacao(),
				u.getAtivo());
	}
}
