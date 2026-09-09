package com.projeto.sistemabancario.integration.cep;

import java.util.Optional;

import com.projeto.sistemabancario.integration.dto.CepConsultaResult;

public interface CepConsultationPort {

	Optional<CepConsultaResult> consultar(String cepSomenteDigitos);
}
