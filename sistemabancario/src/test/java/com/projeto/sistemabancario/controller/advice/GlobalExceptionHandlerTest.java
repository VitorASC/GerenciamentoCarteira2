package com.projeto.sistemabancario.controller.advice;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

	@ParameterizedTest
	@ValueSource(strings = {"23505", "23505\r\nESTADO_INJETADO"})
	void registraSomenteSqlStateValidoEPreservaRespostaDeDuplicidade(String estado, CapturedOutput saida) {
		var causa = new SQLException("duplicate key\r\nDETALHE_DO_DRIVER", estado);
		var erro = new DataIntegrityViolationException("DETALHE_DA_OPERACAO", causa);
		var resposta = new GlobalExceptionHandler().integridadeDados(erro, new MockHttpServletRequest());

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resposta.getBody().mensagem()).isEqualTo("Registro duplicado: já existe um cadastro com estes dados.");
		assertThat(saida.getAll()).contains("categoria=integridade sqlState=" + (estado.equals("23505") ? estado : "indisponivel"))
				.doesNotContain("ESTADO_INJETADO", "DETALHE_DO_DRIVER", "DETALHE_DA_OPERACAO", "duplicate key");
	}
}
