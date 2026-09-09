package com.projeto.sistemabancario.integration.cotacao;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record AlphaVantageQuoteResponse(
		@JsonProperty("Global Quote") Map<String, String> globalQuote,
		@JsonProperty("Note") String note,
		@JsonProperty("Information") String information,
		@JsonProperty("Error Message") String errorMessage) {
}
