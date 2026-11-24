package com.projeto.sistemabancario.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentosTest {

	@ParameterizedTest
	@ValueSource(strings = {"11222333000181", "11.222.333/0001-81"})
	void aceitaCnpjValidoComOuSemMascara(String cnpj) {
		assertThat(Documentos.cnpjValido(cnpj)).isTrue();
	}

	@Test
	void rejeitaPrimeiroDigitoVerificadorIncorreto() {
		assertThat(Documentos.cnpjValido("11222333000191")).isFalse();
	}

	@Test
	void rejeitaSegundoDigitoVerificadorIncorreto() {
		assertThat(Documentos.cnpjValido("11222333000180")).isFalse();
	}

	@Test
	void rejeitaSequenciaDeDigitosRepetidos() {
		assertThat(Documentos.cnpjValido("11111111111111")).isFalse();
	}

	@Test
	void rejeitaTamanhoInvalidoECaracterNaoFormatador() {
		assertThat(Documentos.cnpjValido("123")).isFalse();
		assertThat(Documentos.cnpjValido("11.222.333/0001-8A")).isFalse();
	}

	@Test
	void rejeitaNuloEVazio() {
		assertThat(Documentos.cnpjValido(null)).isFalse();
		assertThat(Documentos.cnpjValido("")).isFalse();
		assertThat(Documentos.cnpjValido("   ")).isFalse();
	}
}
