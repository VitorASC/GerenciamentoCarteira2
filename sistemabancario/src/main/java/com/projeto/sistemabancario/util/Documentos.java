package com.projeto.sistemabancario.util;

public final class Documentos {

	private static final int[] PESOS_PRIMEIRO_DV = { 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
	private static final int[] PESOS_SEGUNDO_DV = { 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };

	private Documentos() {
	}

	public static String apenasDigitos(String texto) {
		if (texto == null) {
			return "";
		}
		return texto.replaceAll("\\D", "");
	}

	public static boolean cnpjValido(String texto) {
		if (texto == null || texto.isBlank() || !texto.matches("[0-9./\\-\\s]+")) {
			return false;
		}

		String cnpj = apenasDigitos(texto);
		if (cnpj.length() != 14 || todosDigitosIguais(cnpj)) {
			return false;
		}

		int primeiroDv = calcularDigito(cnpj, PESOS_PRIMEIRO_DV);
		if (cnpj.charAt(12) - '0' != primeiroDv) {
			return false;
		}

		int segundoDv = calcularDigito(cnpj, PESOS_SEGUNDO_DV);
		return cnpj.charAt(13) - '0' == segundoDv;
	}

	private static int calcularDigito(String cnpj, int[] pesos) {
		int soma = 0;
		for (int i = 0; i < pesos.length; i++) {
			soma += (cnpj.charAt(i) - '0') * pesos[i];
		}
		int resto = soma % 11;
		return resto < 2 ? 0 : 11 - resto;
	}

	private static boolean todosDigitosIguais(String cnpj) {
		char primeiro = cnpj.charAt(0);
		for (int i = 1; i < cnpj.length(); i++) {
			if (cnpj.charAt(i) != primeiro) {
				return false;
			}
		}
		return true;
	}
}
