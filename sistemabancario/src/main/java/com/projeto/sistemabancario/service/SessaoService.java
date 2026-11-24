package com.projeto.sistemabancario.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.sistemabancario.config.security.JwtProperties;
import com.projeto.sistemabancario.config.security.JwtService;
import com.projeto.sistemabancario.config.security.RefreshProperties;
import com.projeto.sistemabancario.domains.entity.SessaoRefreshToken;
import com.projeto.sistemabancario.domains.entity.Usuario;
import com.projeto.sistemabancario.domains.enums.MotivoRevogacao;
import com.projeto.sistemabancario.dto.response.LoginResponse;
import com.projeto.sistemabancario.repository.SessaoRefreshTokenRepository;
import com.projeto.sistemabancario.repository.UsuarioRepository;

@Service
@Transactional
public class SessaoService {

	private final SessaoRefreshTokenRepository sessoes;
	private final UsuarioRepository usuarios;
	private final RefreshProperties refresh;
	private final JwtProperties jwt;
	private final JwtService jwtService;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	public SessaoService(SessaoRefreshTokenRepository sessoes, UsuarioRepository usuarios,
			RefreshProperties refresh, JwtProperties jwt, JwtService jwtService, Clock clock) {
		this.sessoes = sessoes;
		this.usuarios = usuarios;
		this.refresh = refresh;
		this.jwt = jwt;
		this.jwtService = jwtService;
		this.clock = clock;
	}

	public LoginResponse iniciar(Long usuarioId) {
		Usuario usuario = usuarios.bloquearPorId(usuarioId)
				.filter(u -> u.getEstadoUsuario().isContaHabilitada())
				.orElseThrow(() -> new BadCredentialsException("Conta indisponível"));
		Instant agora = clock.instant();
		return emitir(usuario, UUID.randomUUID(), agora, agora.plus(refresh.absoluteExpiration()));
	}

	public Optional<LoginResponse> renovar(String token) {
		return bloquear(token).flatMap(sessao -> {
			Instant agora = clock.instant();
			MotivoRevogacao impedimento = sessao.impedimento(agora, refresh.idleTimeout());
			if (impedimento != null) {
				sessoes.revogarFamilia(sessao.getFamiliaId(), agora, impedimento);
				return Optional.empty();
			}
			sessao.consumir(agora);
			return Optional.of(emitir(sessao.getUsuario(), sessao.getFamiliaId(), agora, sessao.getExpiraEm()));
		});
	}

	public void encerrar(String token) {
		bloquear(token).ifPresent(sessao ->
				sessoes.revogarFamilia(sessao.getFamiliaId(), clock.instant(), MotivoRevogacao.LOGOUT));
	}

	public void encerrarTodas(Long usuarioId) {
		usuarios.bloquearPorId(usuarioId).ifPresent(usuario ->
				sessoes.revogarDoUsuario(usuario.getId(), clock.instant(), MotivoRevogacao.LOGOUT_GLOBAL));
	}

	private Optional<SessaoRefreshToken> bloquear(String token) {
		if (token == null || token.length() != 43) {
			return Optional.empty();
		}
		String hash = hash(token);
		// Todas as operações bloqueiam primeiro o titular para serializar também o logout global.
		return sessoes.buscarTitular(hash).flatMap(usuarios::bloquearPorId)
				.flatMap(usuario -> sessoes.findByTokenHash(hash));
	}

	private LoginResponse emitir(Usuario usuario, UUID familia, Instant agora, Instant limite) {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		sessoes.save(new SessaoRefreshToken(usuario, familia, hash(token), agora, limite));
		String access = jwtService.gerarToken(usuario.getId(), usuario.getEmail(), true, usuario.getPerfilAcesso(), familia);
		return new LoginResponse("Bearer", access, token, jwt.getExpirationMs(), usuario.getPerfilAcesso());
	}

	private static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 indisponível", ex);
		}
	}
}
