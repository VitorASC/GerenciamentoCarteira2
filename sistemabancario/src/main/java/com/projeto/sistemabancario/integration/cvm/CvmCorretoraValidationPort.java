package com.projeto.sistemabancario.integration.cvm;

public interface CvmCorretoraValidationPort {

	CvmCorretoraValidationResult validar(String cnpjSomenteDigitos);

}
