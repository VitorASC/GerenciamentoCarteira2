package com.projeto.sistemabancario.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import com.projeto.sistemabancario.AmbienteDeTeste;
import com.projeto.sistemabancario.domains.enums.PerfilAcesso;
import com.projeto.sistemabancario.exception.ResourceNotFoundException;
import com.projeto.sistemabancario.repository.UsuarioRepository;
import com.projeto.sistemabancario.service.AcaoService;
import com.projeto.sistemabancario.service.CorretoraService;
import com.projeto.sistemabancario.service.UsuarioService;

@AmbienteDeTeste
@SpringBootTest
class MethodSecurityIntegrationTest {

	@Autowired
	private AcaoService acaoService;
	@Autowired
	private CorretoraService corretoraService;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private ApplicationContext context;
	@Autowired
	private UsuarioRepository usuarioRepository;

	@Test
	@WithMockUser(roles = "PADRAO")
	void servicosAdministrativosNegamPadraoMesmoSemPassarPeloHttp() {
		assertThatThrownBy(() -> acaoService.cadastrar(null)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> acaoService.atualizarCotacao(1L)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> acaoService.excluir(1L)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> corretoraService.cadastrar(null)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> corretoraService.excluir(1L)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> usuarioService.listar(Pageable.unpaged())).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> usuarioService.alterarPerfilAcesso(1L, PerfilAcesso.ADMIN))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void servicosAdministrativosPermitemQueAdminChegueAoCasoDeUso() {
		assertThatThrownBy(() -> acaoService.excluir(Long.MAX_VALUE)).isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(() -> corretoraService.excluir(Long.MAX_VALUE)).isInstanceOf(ResourceNotFoundException.class);
		assertThat(usuarioService.listar(Pageable.unpaged())).isNotNull();
	}

	@Test
	void bootstrapDesabilitadoNaoRegistraRunner() {
		assertThat(context.getBeansOfType(AdminBootstrap.class)).isEmpty();
		assertThat(usuarioRepository.findAll()).noneMatch(usuario -> usuario.getPerfilAcesso() == PerfilAcesso.ADMIN);
	}
}
