package com.projeto.sistemabancario.config.security;

import java.time.Clock;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.DispatcherType;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
	private final Environment environment;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
			JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint, JwtAccessDeniedHandler jwtAccessDeniedHandler,
			Environment environment) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
		this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
		this.environment = environment;
	}

	@Bean
	public static Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable);
		http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.cors(c -> c.configurationSource(corsConfigurationSource()));
		http.exceptionHandling(ex -> ex
				.authenticationEntryPoint(jwtAuthenticationEntryPoint)
				.accessDeniedHandler(jwtAccessDeniedHandler));
		http.authorizeHttpRequests(auth -> {
			auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
			auth.requestMatchers(HttpMethod.POST, "/usuarios", "/auth/login", "/auth/refresh", "/auth/logout").permitAll();
			auth.requestMatchers(HttpMethod.POST, "/auth/logout-global").authenticated();
			if (environment.acceptsProfiles(Profiles.of("dev"))) {
				auth.requestMatchers("/h2-console", "/h2-console/**").permitAll();
			}
			String[] frontend = {"/", "/dashboard", "/index.html", "/assets/**", "/favicon.ico"};
			auth.requestMatchers(HttpMethod.GET, frontend).permitAll();
			auth.requestMatchers(HttpMethod.HEAD, frontend).permitAll();

			auth.requestMatchers(HttpMethod.GET, "/usuarios").hasRole("ADMIN");
			auth.requestMatchers(HttpMethod.HEAD, "/usuarios").hasRole("ADMIN");
			auth.requestMatchers(HttpMethod.PUT, "/usuarios/{id:[0-9]+}/perfil-acesso",
					"/acoes/{id:[0-9]+}/atualizar-cotacao").hasRole("ADMIN");
			auth.requestMatchers(HttpMethod.POST, "/acoes", "/corretoras").hasRole("ADMIN");
			auth.requestMatchers(HttpMethod.DELETE, "/acoes/{id:[0-9]+}", "/corretoras/{id:[0-9]+}").hasRole("ADMIN");

			String[] consultas = {
					"/actuator", "/actuator/health",
					"/usuarios/{id:[0-9]+}", "/usuarios/cpf/{cpf}",
					"/acoes", "/acoes/{id:[0-9]+}", "/acoes/ticker/{ticker}", "/acoes/{id:[0-9]+}/historico-cotacoes",
					"/corretoras", "/corretoras/{id:[0-9]+}", "/corretoras/cnpj/{cnpj}",
					"/carteiras", "/carteiras/{id:[0-9]+}", "/carteiras/usuario/{id:[0-9]+}",
					"/carteiras/{id:[0-9]+}/indicadores/ticker/{ticker}",
					"/carteiras/{id:[0-9]+}/indicadores/media-carteira"
			};
			auth.requestMatchers(HttpMethod.GET, consultas).hasAnyRole("PADRAO", "ADMIN");
			auth.requestMatchers(HttpMethod.HEAD, consultas).hasAnyRole("PADRAO", "ADMIN");
			auth.requestMatchers(HttpMethod.PUT, "/usuarios/{id:[0-9]+}", "/carteiras/{id:[0-9]+}")
				.hasAnyRole("PADRAO", "ADMIN");
			auth.requestMatchers(HttpMethod.POST, "/carteiras", "/carteiras/{id:[0-9]+}/compras",
					"/carteiras/{id:[0-9]+}/vendas").hasAnyRole("PADRAO", "ADMIN");
			auth.requestMatchers(HttpMethod.DELETE, "/carteiras/{id:[0-9]+}").hasAnyRole("PADRAO", "ADMIN");
			auth.anyRequest().denyAll();
		});
		if (environment.acceptsProfiles(Profiles.of("dev"))) {
			http.headers(h -> h.frameOptions(FrameOptionsConfig::sameOrigin));
		}
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
		var registration = new FilterRegistrationBean<>(jwtAuthenticationFilter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOriginPattern("*");
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		config.setAllowCredentials(false);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}
