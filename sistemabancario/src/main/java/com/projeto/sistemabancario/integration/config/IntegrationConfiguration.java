package com.projeto.sistemabancario.integration.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableCaching
@EnableConfigurationProperties(IntegrationProperties.class)
public class IntegrationConfiguration {

	@Bean
	public RestClient.Builder integrationRestClientBuilder() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(8));
		factory.setReadTimeout(Duration.ofSeconds(25));
		return RestClient.builder().requestFactory(factory);
	}
}
