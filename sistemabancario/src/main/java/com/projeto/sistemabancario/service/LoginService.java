package com.projeto.sistemabancario.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.sistemabancario.config.security.ProtecaoLoginProperties;
import com.projeto.sistemabancario.config.security.UsuarioPrincipal;
import com.projeto.sistemabancario.domains.enums.TipoEventoSeguranca;
import com.projeto.sistemabancario.dto.request.LoginRequest;
import com.projeto.sistemabancario.dto.response.LoginResponse;
import com.projeto.sistemabancario.repository.ProtecaoAutenticacaoRepository;
import com.projeto.sistemabancario.util.HashSha256;

@Service
public class LoginService {
	private final ProtecaoAutenticacaoRepository protecoes;
	private final ProtecaoLoginProperties politica;
	private final AuthenticationManager autenticacao;
	private final SessaoService sessoes;
	private final AuditoriaSegurancaService auditoria;
	private final Clock clock;

	public LoginService(ProtecaoAutenticacaoRepository protecoes, ProtecaoLoginProperties politica,
			AuthenticationManager autenticacao, SessaoService sessoes, AuditoriaSegurancaService auditoria, Clock clock) {
		this.protecoes = protecoes;
		this.politica = politica;
		this.autenticacao = autenticacao;
		this.sessoes = sessoes;
		this.auditoria = auditoria;
		this.clock = clock;
	}

	@Transactional
	public Optional<LoginResponse> tentar(LoginRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		String chave = HashSha256.calcular(email);
		var protecao = protecoes.bloquear(chave);
		Instant agora = clock.instant();
		if (protecao.estaBloqueada(agora)) {
			auditoria.registrar(TipoEventoSeguranca.LOGIN_RECUSADO_BLOQUEIO, null, null, chave, null);
			return Optional.empty();
		}

		UsuarioPrincipal principal;
		try {
			var credenciais = UsernamePasswordAuthenticationToken.unauthenticated(email, request.senha());
			principal = (UsuarioPrincipal) autenticacao.authenticate(credenciais).getPrincipal();
		}
		catch (BadCredentialsException | AccountStatusException ex) {
			boolean ativou = protecao.registrarFalha(agora, politica);
			auditoria.registrar(TipoEventoSeguranca.LOGIN_FALHA, null, null, chave, null);
			if (ativou) {
				auditoria.registrar(TipoEventoSeguranca.BLOQUEIO_TEMPORARIO_ATIVADO, null, null, chave, null);
			}
			return Optional.empty();
		}

		Long titular = principal.getUsuarioId();
		var par = sessoes.iniciar(titular);
		protecao.limpar();
		auditoria.registrar(TipoEventoSeguranca.LOGIN_SUCESSO, titular, titular, chave, null);
		return Optional.of(par);
	}
}
