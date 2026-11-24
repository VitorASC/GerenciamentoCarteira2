package com.projeto.sistemabancario.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.projeto.sistemabancario.domains.entity.Usuario;
import com.projeto.sistemabancario.domains.enums.PerfilAcesso;
import com.projeto.sistemabancario.domains.enums.PerfilInvestidor;
import com.projeto.sistemabancario.repository.UsuarioRepository;
import com.projeto.sistemabancario.service.UsuarioService;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:test_admin_bootstrap",
		"app.admin.bootstrap.enabled=true",
		"app.admin.bootstrap.email=bootstrap@teste.local",
		"app.admin.bootstrap.cpf=98765432109",
		"app.admin.bootstrap.password=SenhaApenasDeTeste123"
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AdminBootstrapIntegrationTest {

	@Autowired
	private AdminBootstrap bootstrap;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private MockMvc mockMvc;

	@Test
	void criaAdminAtivoComBcryptESegundaExecucaoPreservaConta(CapturedOutput saida) throws Exception {
		var admin = usuarioRepository.findByEmailIgnoreCase("bootstrap@teste.local").orElseThrow();
		assertThat(admin.getPerfilAcesso()).isEqualTo(PerfilAcesso.ADMIN);
		assertThat(admin.getAtivo()).isTrue();
		assertThat(admin.getSenhaHash()).startsWith("$2");
		assertThat(passwordEncoder.matches("SenhaApenasDeTeste123", admin.getSenhaHash())).isTrue();
		assertThat(usuarioRepository.count()).isEqualTo(1);

		bootstrap.run(new DefaultApplicationArguments());

		var administradorAtual = usuarioRepository.findByEmailIgnoreCase("bootstrap@teste.local").orElseThrow();
		assertThat(usuarioRepository.count()).isEqualTo(1);
		assertThat(administradorAtual.getId()).isEqualTo(admin.getId());
		assertThat(administradorAtual.getSenhaHash()).isEqualTo(admin.getSenhaHash());

		String resposta = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"email":"bootstrap@teste.local","senha":"SenhaApenasDeTeste123"}
				"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.perfilAcesso").value("ADMIN"))
				.andReturn().getResponse().getContentAsString();
		assertThat(resposta).doesNotContain("SenhaApenasDeTeste123", admin.getSenhaHash(), "\"senha\"");
		assertThat(saida.getAll()).doesNotContain("SenhaApenasDeTeste123", admin.getSenhaHash());
	}

	@Test
	void rebaixamentosConcorrentesPreservamUmAdminAtivo() throws Exception {
		Usuario primeiro = usuarioRepository.findByEmailIgnoreCase("bootstrap@teste.local").orElseThrow();
		Usuario segundo = new Usuario();
		segundo.setNomeCompleto("Segundo admin de teste");
		segundo.setEmail("segundo-admin@teste.local");
		segundo.setCpf("98765432108");
		segundo.setSenhaHash(primeiro.getSenhaHash());
		segundo.setPerfilInvestidor(PerfilInvestidor.MODERADO);
		segundo.setPerfilAcesso(PerfilAcesso.ADMIN);
		segundo.setDataCriacao(LocalDateTime.now());
		usuarioRepository.save(segundo);

		var executor = Executors.newFixedThreadPool(2);
		var iniciar = new CountDownLatch(1);
		try {
			var rebaixarSegundo = executor.submit(() -> rebaixar(primeiro, segundo, iniciar));
			var rebaixarPrimeiro = executor.submit(() -> rebaixar(segundo, primeiro, iniciar));
			iniciar.countDown();

			assertThat(List.of(rebaixarSegundo.get(10, TimeUnit.SECONDS), rebaixarPrimeiro.get(10, TimeUnit.SECONDS)))
					.containsExactlyInAnyOrder(true, false);
			assertThat(usuarioRepository.findAll()).filteredOn(u -> u.getPerfilAcesso() == PerfilAcesso.ADMIN && u.getAtivo())
					.hasSize(1);
		}
		finally {
			executor.shutdownNow();
			executor.awaitTermination(10, TimeUnit.SECONDS);
			primeiro.setPerfilAcesso(PerfilAcesso.ADMIN);
			usuarioRepository.save(primeiro);
			usuarioRepository.delete(segundo);
		}
	}

	private boolean rebaixar(Usuario autor, Usuario alvo, CountDownLatch iniciar) throws InterruptedException {
		var principal = UsuarioPrincipal.fromJwt(autor.getId(), autor.getEmail(), true, PerfilAcesso.ADMIN);
		var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
		try {
			iniciar.await();
			usuarioService.alterarPerfilAcesso(alvo.getId(), PerfilAcesso.PADRAO);
			return true;
		}
		catch (AccessDeniedException ex) {
			return false;
		}
		finally {
			SecurityContextHolder.clearContext();
		}
	}
}
