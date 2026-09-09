package com.projeto.sistemabancario.util;

public final class Documentos {

	private Documentos() {
	}

	public static String apenasDigitos(String texto) {
		if (texto == null) {
			return "";
		}
		return texto.replaceAll("\\D", "");
	}
}
