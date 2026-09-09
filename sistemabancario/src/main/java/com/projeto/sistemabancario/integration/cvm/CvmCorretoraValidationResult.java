package com.projeto.sistemabancario.integration.cvm;

public record CvmCorretoraValidationResult(
		boolean encontradoNoCadastroCvmComoCorretora,
		boolean emFuncionamentoNormal,
		String situacaoRegistro) {

	public static CvmCorretoraValidationResult naoEncontrado() {
		return new CvmCorretoraValidationResult(false, false, null);
	}
}
