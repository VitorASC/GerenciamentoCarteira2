package com.projeto.sistemabancario.integration.cnpj;

import java.util.Optional;

import com.projeto.sistemabancario.integration.dto.CnpjConsultaResult;

public interface CnpjConsultationPort {

	Optional<CnpjConsultaResult> consultar(String cnpjSomenteDigitos);
}
