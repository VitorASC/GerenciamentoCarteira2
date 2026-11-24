package com.projeto.sistemabancario.config.security;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.sistemabancario.domains.entity.Usuario;
import com.projeto.sistemabancario.domains.enums.PerfilAcesso;
import com.projeto.sistemabancario.domains.enums.PerfilInvestidor;
import com.projeto.sistemabancario.dto.request.UsuarioCadastroRequest;
import com.projeto.sistemabancario.repository.UsuarioRepository;

import jakarta.validation.Validator;

@Component
@ConditionalOnProperty(name = "app.admin.bootstrap.enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final Validator validator;
	private final String email;
	private final String cpf;
	private final String senha;

	public AdminBootstrap(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, Validator validator,
			@Value("${app.admin.bootstrap.email}") String email, @Value("${app.admin.bootstrap.cpf}") String cpf,
			@Value("${app.admin.bootstrap.password}") String senha) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.validator = validator;
		this.email = email.trim().toLowerCase(Locale.ROOT);
		this.cpf = cpf.trim();
		this.senha = senha;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		usuarioRepository.bloquearAlteracoesAdministrativas();
		if (usuarioRepository.existsByPerfilAcessoAndAtivoTrue(PerfilAcesso.ADMIN)) {
			return;
		}

		var cadastro = new UsuarioCadastroRequest("Administrador GestãoAtiva", cpf, email, senha, null,
				PerfilInvestidor.MODERADO);
		if (!validator.validate(cadastro).isEmpty() || !cpf.matches("[0-9]{11}")
				|| senha.length() < 12 || senha.getBytes(StandardCharsets.UTF_8).length > 72 || email.length() > 255) {
			throw new IllegalStateException("Configuração inválida do bootstrap administrativo");
		}
		if (usuarioRepository.existsByCpf(cpf) || usuarioRepository.existsByEmailIgnoreCase(email)) {
			throw new IllegalStateException("O e-mail ou CPF do bootstrap administrativo já está cadastrado");
		}

		Usuario usuario = new Usuario();
		usuario.setNomeCompleto(cadastro.nomeCompleto());
		usuario.setCpf(cadastro.cpf());
		usuario.setEmail(cadastro.email());
		usuario.setSenhaHash(passwordEncoder.encode(cadastro.senha()));
		usuario.setPerfilInvestidor(cadastro.perfilInvestidor());
		usuario.setPerfilAcesso(PerfilAcesso.ADMIN);
		usuario.setDataCriacao(LocalDateTime.now());
		usuarioRepository.save(usuario);
	}
}
