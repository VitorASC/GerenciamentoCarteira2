package com.projeto.sistemabancario.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentosTest {

	@Test
	void aceitaCnpjValidoSemMascara() {
		assertThat(Documentos.cnpjValido("11222333000181")).isTrue();
	}

	@Test
	void aceitaMesmoCnpjValidoFormatado() {
		assertThat(Documentos.cnpjValido("11.222.333/0001-81")).isTrue();
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
