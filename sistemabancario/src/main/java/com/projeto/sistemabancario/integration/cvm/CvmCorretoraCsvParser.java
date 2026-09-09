package com.projeto.sistemabancario.integration.cvm;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.projeto.sistemabancario.integration.exception.ExternalIntegrationException;

final class CvmCorretoraCsvParser {

	private static final String TIPO_CORRETORAS = "CORRETORAS";

	private CvmCorretoraCsvParser() {
	}

	static Map<String, String> parseCorretorasSituacao(BufferedReader reader) throws IOException {
		Map<String, String> destino = new HashMap<>();
		String headerLine = reader.readLine();
		if (headerLine == null) {
			return Map.of();
		}
		String[] headers = headerLine.split(";");
		int idxTipo = indexOfIgnoreCase(headers, "TP_PARTIC");
		int idxCnpj = indexOfIgnoreCase(headers, "CNPJ");
		int idxSit = indexOfIgnoreCase(headers, "SIT");
		if (idxTipo < 0 || idxCnpj < 0 || idxSit < 0) {
			throw new ExternalIntegrationException(
					"Cabeçalho do CSV da CVM não contém colunas esperadas (TP_PARTIC, CNPJ, SIT)");
		}
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.isBlank()) {
				continue;
			}
			String[] cols = line.split(";", -1);
			if (cols.length <= Math.max(Math.max(idxTipo, idxCnpj), idxSit)) {
				continue;
			}
			String tipo = safeCol(cols, idxTipo);
			if (!TIPO_CORRETORAS.equalsIgnoreCase(trimCad(tipo))) {
				continue;
			}
			String cnpj = normalizeDigits(safeCol(cols, idxCnpj));
			if (cnpj.length() != 14) {
				continue;
			}
			String sit = safeCol(cols, idxSit);
			destino.put(cnpj, sit);
		}
		return Map.copyOf(destino);
	}

	private static int indexOfIgnoreCase(String[] headers, String name) {
		for (int i = 0; i < headers.length; i++) {
			if (name.equalsIgnoreCase(trimCad(headers[i]))) {
				return i;
			}
		}
		return -1;
	}

	private static String safeCol(String[] cols, int idx) {
		return idx < cols.length ? cols[idx] : "";
	}

	private static String trimCad(String s) {
		return s == null ? "" : s.trim();
	}

	private static String normalizeDigits(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replaceAll("\\D", "");
	}
}
