package com.projeto.sistemabancario.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

	@GetMapping("/")
	public String home() {
		return "forward:/index.html";
	}

	/** Mesma SPA que {@code /}; mantém compatibilidade com links antigos. */
	@GetMapping("/dashboard")
	public String dashboard() {
		return "forward:/index.html";
	}
}
