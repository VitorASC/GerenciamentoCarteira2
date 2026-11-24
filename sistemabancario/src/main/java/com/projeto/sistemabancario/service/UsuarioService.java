package com.projeto.sistemabancario.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.sistemabancario.config.security.UsuarioPrincipal;
import com.projeto.sistemabancario.domains.enums.PerfilAcesso;
import com.projeto.sistemabancario.domains.enums.TipoEventoSeguranca;
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
	private final AuditoriaSegurancaService auditoria;

	public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, AuditoriaSegurancaService auditoria) {
		this.auditoria = auditoria;
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
		u.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
		u.setSenhaHash(passwordEncoder.encode(request.senha()));
		u.setDataNascimento(request.dataNascimento());
		u.setPerfilInvestidor(request.perfilInvestidor());
		u.setPerfilAcesso(PerfilAcesso.PADRAO);
		u.setDataCriacao(LocalDateTime.now());
		u.setEstadoUsuario(EstadoUsuarioAtivo.INSTANCE);

		return toResponse(usuarioRepository.save(u));
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasRole('ADMIN')")
	public Page<UsuarioResponse> listar(Pageable pageable) {
		return usuarioRepository.findAll(pageable).map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public UsuarioResponse buscarPorId(Long id, Long usuarioAutenticadoId, boolean admin) {
		if (!admin && !id.equals(usuarioAutenticadoId)) {
			throw new AccessDeniedException("Você só pode consultar o próprio cadastro.");
		}
		return toResponse(buscarUsuario(id));
	}

	@Transactional(readOnly = true)
	public UsuarioResponse buscarPorCpf(String cpfBruto, Long usuarioAutenticadoId, boolean admin) {
		String cpf = Documentos.apenasDigitos(cpfBruto);
		Usuario alvo = usuarioRepository.findByCpf(cpf)
			.orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado para o CPF informado."));
		if (!admin && !alvo.getId().equals(usuarioAutenticadoId)) {
			throw new AccessDeniedException("Você só pode consultar o próprio CPF.");
		}
		return toResponse(alvo);
	}

	@Transactional
	public UsuarioResponse atualizar(Long id, UsuarioAtualizacaoRequest request, Long usuarioAutenticadoId,
			boolean admin) {
		boolean proprioCadastro = id.equals(usuarioAutenticadoId);
		if (!admin && !proprioCadastro) {
			throw new AccessDeniedException("Você só pode alterar o próprio cadastro.");
		}

		usuarioRepository.bloquearAlteracoesAdministrativas();
		Usuario autor = buscarUsuario(usuarioAutenticadoId);
		if (!autor.getEstadoUsuario().isContaHabilitada()
				|| (!proprioCadastro && autor.getPerfilAcesso() != PerfilAcesso.ADMIN)) {
			throw new AccessDeniedException("Você não tem permissão para alterar este cadastro.");
		}
		Usuario u = proprioCadastro ? autor : buscarUsuario(id);
		if (request.nomeCompleto() != null && !request.nomeCompleto().isBlank()) {
			u.setNomeCompleto(request.nomeCompleto().trim());
		}
		if (request.email() != null && !request.email().isBlank()) {
			String email = request.email().trim().toLowerCase(Locale.ROOT);
			if (!email.equalsIgnoreCase(u.getEmail())) {
				if (usuarioRepository.existsByEmailIgnoreCase(email)) {
					throw new DuplicateResourceException("Já existe usuário com este e-mail.");
				}
				auditoria.registrar(TipoEventoSeguranca.EMAIL_ALTERADO, autor.getId(), id, null, null);
			}
			u.setEmail(email);
		}
		if (request.perfilInvestidor() != null) {
			u.setPerfilInvestidor(request.perfilInvestidor());
		}
		if (request.ativo() != null) {
			if (Boolean.FALSE.equals(request.ativo())) {
				preservarAdministradorAtivo(u);
			}
			if (!request.ativo().equals(u.getAtivo())) {
				var tipo = request.ativo() ? TipoEventoSeguranca.USUARIO_ATIVADO : TipoEventoSeguranca.USUARIO_INATIVADO;
				auditoria.registrar(tipo, autor.getId(), id, null, null);
			}
			u.setEstadoUsuario(Boolean.TRUE.equals(request.ativo())
					? EstadoUsuarioAtivo.INSTANCE
					: EstadoUsuarioInativo.INSTANCE);
		}
		return toResponse(usuarioRepository.save(u));
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public UsuarioResponse alterarPerfilAcesso(Long id, PerfilAcesso perfilAcesso) {
		var principal = (UsuarioPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (id.equals(principal.getUsuarioId())) {
			throw new AccessDeniedException("Você só pode alterar o perfil de acesso de outro usuário.");
		}
		usuarioRepository.bloquearAlteracoesAdministrativas();
		Usuario autor = buscarUsuario(principal.getUsuarioId());
		if (autor.getPerfilAcesso() != PerfilAcesso.ADMIN || !autor.getEstadoUsuario().isContaHabilitada()) {
			throw new AccessDeniedException("Você não tem permissão para alterar perfis de acesso.");
		}
		Usuario alvo = buscarUsuario(id);
		if (perfilAcesso == PerfilAcesso.PADRAO) {
			preservarAdministradorAtivo(alvo);
		}
		if (alvo.getPerfilAcesso() != perfilAcesso) {
			alvo.setPerfilAcesso(perfilAcesso);
			auditoria.registrar(TipoEventoSeguranca.PERFIL_ALTERADO, autor.getId(), id, null, null);
		}
		return toResponse(usuarioRepository.save(alvo));
	}

	private void preservarAdministradorAtivo(Usuario usuario) {
		if (usuario.getPerfilAcesso() == PerfilAcesso.ADMIN && usuario.getEstadoUsuario().isContaHabilitada()
				&& !usuarioRepository.existsByPerfilAcessoAndAtivoTrueAndIdNot(PerfilAcesso.ADMIN, usuario.getId())) {
			throw new RegraNegocioException("Deve permanecer pelo menos um administrador ativo.");
		}
	}

	private Usuario buscarUsuario(Long id) {
		return usuarioRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
	}

	private UsuarioResponse toResponse(Usuario u) {
		return new UsuarioResponse(
				u.getId(),
				u.getNomeCompleto(),
				u.getCpf(),
				u.getEmail(),
				u.getDataNascimento(),
				u.getPerfilInvestidor(),
				u.getPerfilAcesso(),
				u.getDataCriacao(),
				u.getAtivo());
	}
}
