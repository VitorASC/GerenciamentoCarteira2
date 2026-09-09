package com.projeto.sistemabancario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.projeto.sistemabancario.config.security.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class SistemabancarioApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemabancarioApplication.class, args);
	}

}
