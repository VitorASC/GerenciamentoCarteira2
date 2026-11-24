package com.projeto.sistemabancario.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
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
import com.projeto.sistemabancario.domains.enums.TipoEventoSeguranca;
import com.projeto.sistemabancario.util.HashSha256;
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
	private final AuditoriaSegurancaService auditoria;
	private final SecureRandom random = new SecureRandom();

	public SessaoService(SessaoRefreshTokenRepository sessoes, UsuarioRepository usuarios,
			RefreshProperties refresh, JwtProperties jwt, JwtService jwtService, Clock clock, AuditoriaSegurancaService auditoria) {
		this.auditoria = auditoria;
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
		var encontrada = bloquear(token);
		if (encontrada.isEmpty()) {
			auditoria.registrar(TipoEventoSeguranca.REFRESH_FALHA, null, null, null, null);
			return Optional.empty();
		}
		var sessao = encontrada.get();
		Long titular = sessao.getUsuario().getId();
		Instant agora = clock.instant();
		MotivoRevogacao impedimento = sessao.impedimento(agora, refresh.idleTimeout());
		if (impedimento != null) {
			sessoes.revogarFamilia(sessao.getFamiliaId(), agora, impedimento);
			auditoria.registrar(TipoEventoSeguranca.REFRESH_FALHA, null, titular, null, impedimento);
			return Optional.empty();
		}
		sessao.consumir(agora);
		var par = emitir(sessao.getUsuario(), sessao.getFamiliaId(), agora, sessao.getExpiraEm());
		auditoria.registrar(TipoEventoSeguranca.REFRESH_SUCESSO, titular, titular, null, null);
		return Optional.of(par);
	}

	public void encerrar(String token) {
		bloquear(token).ifPresent(sessao -> {
			if (sessoes.revogarFamilia(sessao.getFamiliaId(), clock.instant(), MotivoRevogacao.LOGOUT) > 0) {
				Long titular = sessao.getUsuario().getId();
				auditoria.registrar(TipoEventoSeguranca.LOGOUT, titular, titular, null, null);
			}
		});
	}

	public void encerrarTodas(Long usuarioId) {
		usuarios.bloquearPorId(usuarioId).ifPresent(usuario -> {
			sessoes.revogarDoUsuario(usuario.getId(), clock.instant(), MotivoRevogacao.LOGOUT_GLOBAL);
			auditoria.registrar(TipoEventoSeguranca.LOGOUT_GLOBAL, usuarioId, usuarioId, null, null);
		});
	}

	private Optional<SessaoRefreshToken> bloquear(String token) {
		if (token == null || token.length() != 43) {
			return Optional.empty();
		}
		String hash = HashSha256.calcular(token);
		// Todas as operações bloqueiam primeiro o titular para serializar também o logout global.
		return sessoes.buscarTitular(hash).flatMap(usuarios::bloquearPorId)
				.flatMap(usuario -> sessoes.findByTokenHash(hash));
	}

	private LoginResponse emitir(Usuario usuario, UUID familia, Instant agora, Instant limite) {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		sessoes.save(new SessaoRefreshToken(usuario, familia, HashSha256.calcular(token), agora, limite));
		String access = jwtService.gerarToken(usuario.getId(), usuario.getEmail(), true, usuario.getPerfilAcesso(), familia);
		return new LoginResponse("Bearer", access, token, jwt.getExpirationMs(), usuario.getPerfilAcesso());
	}
}
