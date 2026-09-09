package com.projeto.sistemabancario.integration.cvm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CvmCorretoraCsvParserTest {

	@Test
	void parseFiltraCorretorasENormalizaCnpj() throws IOException {
		String csv = """
				TP_PARTIC;CNPJ;DENOM_SOCIAL;DENOM_COMERC;DT_REG;DT_CANCEL;MOTIVO_CANCEL;SIT;DT_INI_SIT
				CORRETORAS;76.621.457/0001-85;X;Y;;;;EM FUNCIONAMENTO NORMAL;
				BANCOS COMERCIAIS;03.532.415/0001-02;Banco;;;;
				CORRETORAS;10.000.000/0001-01;Z;W;;;;CANCELADA;
				""";
		try (BufferedReader reader = new BufferedReader(new StringReader(csv))) {
			Map<String, String> map = CvmCorretoraCsvParser.parseCorretorasSituacao(reader);
			assertThat(map).containsEntry("76621457000185", "EM FUNCIONAMENTO NORMAL");
			assertThat(map).containsEntry("10000000000101", "CANCELADA");
			assertThat(map).doesNotContainKey("035324150000102");
		}
	}
}
