package com.projeto.sistemabancario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.projeto.sistemabancario.config.security.JwtProperties;
import com.projeto.sistemabancario.config.security.ProtecaoLoginProperties;
import com.projeto.sistemabancario.config.security.RefreshProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, RefreshProperties.class, ProtecaoLoginProperties.class})
public class SistemabancarioApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemabancarioApplication.class, args);
	}

}
