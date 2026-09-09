(function () {
	const TOKEN_KEY = "sistemabancario.jwt";
	const THEME_KEY = "finfef.theme";

	const PANEL_META = {
		corretoras: {
			title: "Corretoras",
			sub: "Cadastro e consulta com Brasil API e valida├º├úo CVM.",
		},
		acoes: {
			title: "A├º├Áes e cota├º├Áes",
			sub: "Ativos, hist├│rico e gr├ífico de cota├º├Áes.",
		},
		carteiras: {
			title: "Carteiras",
			sub: "Carteiras, compra, venda e indicadores.",
		},
		conta: {
			title: "Conta",
			sub: "Sess├úo JWT e usu├írios.",
		},
	};

	function stripDigits(s) {
		return String(s || "").replace(/\D/g, "");
	}

	function setStatus(el, text, isError) {
		el.textContent = text || "";
		el.classList.toggle("error", Boolean(isError));
	}

	function getToken() {
		return sessionStorage.getItem(TOKEN_KEY) || "";
	}

	function authHeaders() {
		const t = getToken();
		const h = { Accept: "application/json" };
		if (t) {
			h.Authorization = "Bearer " + t;
		}
		return h;
	}

	function parseJwtPayload(token) {
		if (!token) {
			return null;
		}
		try {
			const part = token.split(".")[1];
			const b64 = part.replace(/-/g, "+").replace(/_/g, "/");
			const json = decodeURIComponent(
				atob(b64)
					.split("")
					.map(function (c) {
						return "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2);
					})
					.join("")
			);
			return JSON.parse(json);
		} catch (e) {
			return null;
		}
	}

	function refreshJwtSummary() {
		const el = document.getElementById("jwt-summary");
		if (!el) {
			return;
		}
		const pl = parseJwtPayload(getToken());
		if (!pl) {
			el.textContent = "ÔÇö";
			return;
		}
		const uid = pl.sub;
		const email = pl.email != null ? pl.email : "ÔÇö";
		el.innerHTML =
			"<strong>ID (subject)</strong>: " +
			uid +
			" ┬À <strong>e-mail</strong>: " +
			email +
			" ┬À exp: " +
			(pl.exp ? new Date(pl.exp * 1000).toLocaleString() : "ÔÇö");
		prefillUserIds(uid);
	}

	function prefillUserIds(usuarioIdStr) {
		const id = usuarioIdStr ? String(usuarioIdStr) : "";
		const u = document.getElementById("usuario-update-id");
		if (u && !u.value) {
			u.value = id;
		}
		const c = document.getElementById("carteira-usuario-id");
		if (c && !c.value) {
			c.value = id;
		}
		const l = document.getElementById("carteira-list-usuario-id");
		if (l && !l.value) {
			l.value = id;
		}
	}

	function initTheme() {
		let t = localStorage.getItem(THEME_KEY) || "dark";
		if (t !== "light" && t !== "dark") {
			t = "dark";
		}
		document.documentElement.setAttribute("data-theme", t);
	}

	function toggleTheme() {
		const cur = document.documentElement.getAttribute("data-theme") || "dark";
		const next = cur === "light" ? "dark" : "light";
		document.documentElement.setAttribute("data-theme", next);
		localStorage.setItem(THEME_KEY, next);
	}

	function showPanel(name) {
		document.querySelectorAll(".panel").forEach(function (p) {
			const show = p.id === "panel-" + name;
			p.hidden = !show;
			p.classList.toggle("is-hidden", !show);
			p.classList.toggle("panel--active", show);
		});
		document.querySelectorAll(".sidebar-link").forEach(function (b) {
			const pan = b.getAttribute("data-panel");
			b.classList.toggle("sidebar-link--active", pan === name);
		});
		const meta = PANEL_META[name];
		if (meta) {
			document.getElementById("app-main-title").textContent = meta.title;
			document.getElementById("app-main-sub").textContent = meta.sub;
		}
		document.body.classList.remove("sidebar-open");
		const bd = document.getElementById("sidebar-backdrop");
		if (bd) {
			bd.classList.add("is-hidden");
		}
		const hm = document.getElementById("btn-menu");
		if (hm) {
			hm.setAttribute("aria-expanded", "false");
		}
	}

	function showLoginScreen() {
		document.getElementById("screen-app").classList.add("is-hidden");
		document.getElementById("screen-app").setAttribute("aria-hidden", "true");
		document.getElementById("screen-login").classList.remove("is-hidden");
		const jwtEl = document.getElementById("jwt-summary");
		if (jwtEl) {
			jwtEl.textContent = "ÔÇö";
		}
		document.body.classList.remove("sidebar-open");
		const tf = document.querySelector(".theme-float");
		if (tf) {
			tf.classList.remove("is-hidden");
		}
	}

	function enterApp() {
		document.getElementById("screen-login").classList.add("is-hidden");
		const app = document.getElementById("screen-app");
		app.classList.remove("is-hidden");
		app.setAttribute("aria-hidden", "false");
		const tf = document.querySelector(".theme-float");
		if (tf) {
			tf.classList.add("is-hidden");
		}
		showPanel("corretoras");
		refreshJwtSummary();
	}

	function logout() {
		sessionStorage.removeItem(TOKEN_KEY);
		showLoginScreen();
		const ls = document.getElementById("login-status");
		if (ls) {
			ls.textContent = "Sess├úo encerrada.";
			ls.classList.remove("error");
		}
	}

	function bindLoginTabs() {
		function activate(tab) {
			const loginTab = document.getElementById("tab-login");
			const regTab = document.getElementById("tab-register");
			const loginPan = document.getElementById("login-panel-login");
			const regPan = document.getElementById("login-panel-register");
			if (tab === "login") {
				loginTab.classList.add("login-tab--active");
				regTab.classList.remove("login-tab--active");
				loginTab.setAttribute("aria-selected", "true");
				regTab.setAttribute("aria-selected", "false");
				loginPan.hidden = false;
				regPan.hidden = true;
			} else {
				regTab.classList.add("login-tab--active");
				loginTab.classList.remove("login-tab--active");
				regTab.setAttribute("aria-selected", "true");
				loginTab.setAttribute("aria-selected", "false");
				regPan.hidden = false;
				loginPan.hidden = true;
			}
		}
		document.getElementById("tab-login").addEventListener("click", function () {
			activate("login");
		});
		document.getElementById("tab-register").addEventListener("click", function () {
			activate("register");
		});
	}

	function bindSidebar() {
		document.querySelectorAll(".sidebar-link").forEach(function (btn) {
			btn.addEventListener("click", function () {
				const name = btn.getAttribute("data-panel");
				if (name) {
					showPanel(name);
				}
			});
		});
	}

	function bindMobileMenu() {
		const menu = document.getElementById("btn-menu");
		const backdrop = document.getElementById("sidebar-backdrop");
		if (!menu) {
			return;
		}
		menu.addEventListener("click", function () {
			const open = !document.body.classList.contains("sidebar-open");
			document.body.classList.toggle("sidebar-open", open);
			menu.setAttribute("aria-expanded", open ? "true" : "false");
			if (backdrop) {
				backdrop.classList.toggle("is-hidden", !open);
			}
		});
		if (backdrop) {
			backdrop.addEventListener("click", function () {
				document.body.classList.remove("sidebar-open");
				menu.setAttribute("aria-expanded", "false");
				backdrop.classList.add("is-hidden");
			});
		}
	}

	function boot() {
		initTheme();
		bindLoginTabs();
		bindSidebar();
		bindMobileMenu();
		document.querySelectorAll(".js-theme-toggle").forEach(function (btn) {
			btn.addEventListener("click", toggleTheme);
		});
		if (getToken()) {
			enterApp();
		} else {
			showLoginScreen();
		}
	}

	async function api(method, path, body) {
		const opts = {
			method: method,
			headers: { ...authHeaders() },
		};
		if (body !== undefined && body !== null && method !== "GET" && method !== "HEAD") {
			opts.headers["Content-Type"] = "application/json";
			opts.body = JSON.stringify(body);
		}
		const res = await fetch(path, opts);
		if (res.status === 204) {
			return null;
		}
		const text = await res.text();
		let data = null;
		try {
			data = text ? JSON.parse(text) : null;
		} catch (_) {
			data = text;
		}
		if (!res.ok) {
			const msg =
				data && typeof data === "object" && data.mensagem != null
					? data.mensagem
					: "HTTP " + res.status;
			throw new Error(msg);
		}
		return data;
	}

	function showJson(preId, data) {
		const pre = document.getElementById(preId);
		if (!pre) {
			return;
		}
		pre.hidden = false;
		pre.textContent = JSON.stringify(data, null, 2);
	}

	function hideTable(tableId) {
		const w = document.getElementById(tableId);
		if (w) {
			w.hidden = true;
			w.innerHTML = "";
		}
	}

	function renderPageTable(containerId, columns, rows, rowMapper) {
		const w = document.getElementById(containerId);
		if (!w) {
			return;
		}
		w.hidden = false;
		const thead =
			"<thead><tr>" +
			columns
				.map(function (c) {
					return "<th>" + c + "</th>";
				})
				.join("") +
			"</tr></thead>";
		const body = rows
			.map(function (r) {
				return (
					"<tr>" +
					rowMapper(r)
						.map(function (cell) {
							return "<td>" + cell + "</td>";
						})
						.join("") +
					"</tr>"
				);
			})
			.join("");
		w.innerHTML = "<table>" + thead + "<tbody>" + body + "</tbody></table>";
	}

	function esc(s) {
		if (s == null || s === "") {
			return "ÔÇö";
		}
		const t = String(s);
		return t
			.replace(/&/g, "&amp;")
			.replace(/</g, "&lt;")
			.replace(/>/g, "&gt;")
			.replace(/"/g, "&quot;");
	}

	/* ---- Login / logout / register ---- */
	document.getElementById("form-login").addEventListener("submit", async function (e) {
		e.preventDefault();
		const loginStatus = document.getElementById("login-status");
		setStatus(loginStatus, "EntrandoÔÇª", false);
		const fd = new FormData(e.target);
		try {
			const data = await api("POST", "/auth/login", {
				email: String(fd.get("email") || "").trim(),
				senha: String(fd.get("senha") || ""),
			});
			sessionStorage.setItem(TOKEN_KEY, data.accessToken || "");
			setStatus(loginStatus, "Login realizado.", false);
			enterApp();
		} catch (err) {
			setStatus(loginStatus, err.message, true);
		}
	});

	document.getElementById("btn-logout").addEventListener("click", function () {
		logout();
	});

	document.getElementById("form-register").addEventListener("submit", async function (e) {
		e.preventDefault();
		const st = document.getElementById("register-status");
		setStatus(st, "EnviandoÔÇª", false);
		const fd = new FormData(e.target);
		const body = {
			nomeCompleto: String(fd.get("nomeCompleto") || "").trim(),
			cpf: stripDigits(fd.get("cpf")),
			email: String(fd.get("email") || "").trim(),
			senha: String(fd.get("senha") || ""),
			perfilInvestidor: String(fd.get("perfilInvestidor") || "MODERADO"),
		};
		const dn = fd.get("dataNascimento");
		if (dn) {
			body.dataNascimento = String(dn);
		}
		try {
			const data = await api("POST", "/usuarios", body);
			setStatus(st, "Cadastro criado. ID " + data.id + ". Voc├¬ pode entrar na aba Entrar.", false);
			showJson("register-json-out", data);
			const regPre = document.getElementById("register-json-out");
			if (regPre) {
				regPre.hidden = false;
			}
		} catch (err) {
			setStatus(st, err.message, true);
		}
	});

	boot();

	/* ---- Usu├írios ---- */
	const usuariosStatus = document.getElementById("usuarios-status");

	document.getElementById("btn-usuarios-listar").addEventListener("click", async function () {
		if (!getToken()) {
			setStatus(usuariosStatus, "Fa├ºa login.", true);
			return;
		}
		setStatus(usuariosStatus, "CarregandoÔÇª", false);
		try {
			const page = await api("GET", "/usuarios?size=50");
			const rows = page.content || [];
			setStatus(usuariosStatus, rows.length + " usu├írios (p├ígina).", false);
			renderPageTable(
				"usuarios-out",
				["ID", "Nome", "E-mail", "CPF", "Perfil", "Ativo"],
				rows,
				function (u) {
					return [
						esc(u.id),
						esc(u.nomeCompleto),
						esc(u.email),
						esc(u.cpf),
						esc(u.perfilInvestidor),
						esc(u.ativo),
					];
				}
			);
			document.getElementById("usuarios-json").hidden = true;
		} catch (err) {
			setStatus(usuariosStatus, err.message, true);
		}
	});

	document.getElementById("form-usuario-buscar-id").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(usuariosStatus, "Fa├ºa login.", true);
			return;
		}
		const id = new FormData(e.target).get("id");
		setStatus(usuariosStatus, "BuscandoÔÇª", false);
		try {
			const data = await api("GET", "/usuarios/" + encodeURIComponent(id));
			setStatus(usuariosStatus, "OK.", false);
			hideTable("usuarios-out");
			showJson("usuarios-json", data);
		} catch (err) {
			setStatus(usuariosStatus, err.message, true);
		}
	});

	document.getElementById("form-usuario-buscar-cpf").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(usuariosStatus, "Fa├ºa login.", true);
			return;
		}
		const cpf = stripDigits(new FormData(e.target).get("cpf"));
		if (cpf.length !== 11) {
			setStatus(usuariosStatus, "CPF deve ter 11 d├¡gitos.", true);
			return;
		}
		setStatus(usuariosStatus, "BuscandoÔÇª", false);
		try {
			const data = await api("GET", "/usuarios/cpf/" + encodeURIComponent(cpf));
			setStatus(usuariosStatus, "OK.", false);
			hideTable("usuarios-out");
			showJson("usuarios-json", data);
		} catch (err) {
			setStatus(usuariosStatus, err.message, true);
		}
	});

	document.getElementById("form-usuario-atualizar").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(usuariosStatus, "Fa├ºa login.", true);
			return;
		}
		const fd = new FormData(e.target);
		const id = fd.get("id");
		const body = {};
		const nome = String(fd.get("nomeCompleto") || "").trim();
		const email = String(fd.get("email") || "").trim();
		const perfil = String(fd.get("perfilInvestidor") || "").trim();
		const ativoOpt = String(fd.get("ativoOpt") || "").trim();
		if (nome) {
			body.nomeCompleto = nome;
		}
		if (email) {
			body.email = email;
		}
		if (perfil) {
			body.perfilInvestidor = perfil;
		}
		if (ativoOpt === "true") {
			body.ativo = true;
		}
		if (ativoOpt === "false") {
			body.ativo = false;
		}
		setStatus(usuariosStatus, "AtualizandoÔÇª", false);
		try {
			const data = await api("PUT", "/usuarios/" + encodeURIComponent(id), body);
			setStatus(usuariosStatus, "Usu├írio atualizado.", false);
			hideTable("usuarios-out");
			showJson("usuarios-json", data);
		} catch (err) {
			setStatus(usuariosStatus, err.message, true);
		}
	});

	/* ---- Corretoras ---- */
	const corretorasStatus = document.getElementById("corretoras-status");

	document.getElementById("btn-corretoras-listar").addEventListener("click", async function () {
		if (!getToken()) {
			setStatus(corretorasStatus, "Fa├ºa login.", true);
			return;
		}
		setStatus(corretorasStatus, "CarregandoÔÇª", false);
		try {
			const page = await api("GET", "/corretoras?size=50");
			const rows = page.content || [];
			setStatus(corretorasStatus, rows.length + " corretoras.", false);
			renderPageTable(
				"corretoras-out",
				["ID", "CNPJ", "Raz├úo social", "Cidade", "UF", "CVM"],
				rows,
				function (c) {
					return [
						esc(c.id),
						esc(c.cnpj),
						esc(c.razaoSocial),
						esc(c.cidade),
						esc(c.uf),
						esc(c.validadaNaCvm),
					];
				}
			);
			document.getElementById("corretoras-json").hidden = true;
		} catch (err) {
			setStatus(corretorasStatus, err.message, true);
		}
	});

	document.getElementById("form-corretora-cadastro").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(corretorasStatus, "Fa├ºa login.", true);
			return;
		}
		const fd = new FormData(e.target);
		const body = {
			cnpj: String(fd.get("cnpj") || "").trim(),
			cep: String(fd.get("cep") || "").trim(),
		};
		const num = String(fd.get("numero") || "").trim();
		const comp = String(fd.get("complemento") || "").trim();
		if (num) {
			body.numero = num;
		}
		if (comp) {
			body.complemento = comp;
		}
		setStatus(corretorasStatus, "CadastrandoÔÇª", false);
		try {
			const data = await api("POST", "/corretoras", body);
			setStatus(corretorasStatus, "Corretora criada (ID " + data.id + ").", false);
			hideTable("corretoras-out");
			showJson("corretoras-json", data);
		} catch (err) {
			setStatus(corretorasStatus, err.message, true);
		}
	});

	document.getElementById("form-corretora-buscar-id").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(corretorasStatus, "Fa├ºa login.", true);
			return;
		}
		const id = new FormData(e.target).get("id");
		setStatus(corretorasStatus, "BuscandoÔÇª", false);
		try {
			const data = await api("GET", "/corretoras/" + encodeURIComponent(id));
			setStatus(corretorasStatus, "OK.", false);
			hideTable("corretoras-out");
			showJson("corretoras-json", data);
		} catch (err) {
			setStatus(corretorasStatus, err.message, true);
		}
	});

	document.getElementById("form-corretora-buscar-cnpj").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(corretorasStatus, "Fa├ºa login.", true);
			return;
		}
		const cnpj = stripDigits(new FormData(e.target).get("cnpj"));
		setStatus(corretorasStatus, "BuscandoÔÇª", false);
		try {
			const data = await api("GET", "/corretoras/cnpj/" + encodeURIComponent(cnpj));
			setStatus(corretorasStatus, "OK.", false);
			hideTable("corretoras-out");
			showJson("corretoras-json", data);
		} catch (err) {
			setStatus(corretorasStatus, err.message, true);
		}
	});

	document.getElementById("form-corretora-excluir").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(corretorasStatus, "Fa├ºa login.", true);
			return;
		}
		const id = new FormData(e.target).get("id");
		if (!window.confirm("Excluir definitivamente a corretora ID " + id + "?")) {
			return;
		}
		setStatus(corretorasStatus, "ExcluindoÔÇª", false);
		try {
			await api("DELETE", "/corretoras/" + encodeURIComponent(id));
			setStatus(corretorasStatus, "Corretora ID " + id + " exclu├¡da com sucesso.", false);
			hideTable("corretoras-out");
			document.getElementById("corretoras-json").hidden = true;
			e.target.reset();
		} catch (err) {
			setStatus(corretorasStatus, err.message, true);
		}
	});

	/* ---- A├º├Áes + gr├ífico ---- */
	const acoesStatus = document.getElementById("acoes-status");
	const chartStatus = document.getElementById("chart-status");
	const canvas = document.getElementById("chart-cotacoes");
	let chart;

	document.getElementById("btn-acoes-listar").addEventListener("click", async function () {
		if (!getToken()) {
			setStatus(acoesStatus, "Fa├ºa login.", true);
			return;
		}
		setStatus(acoesStatus, "CarregandoÔÇª", false);
		try {
			const page = await api("GET", "/acoes?size=50");
			const rows = page.content || [];
			setStatus(acoesStatus, rows.length + " a├º├Áes.", false);
			renderPageTable(
				"acoes-out",
				["ID", "Ticker", "Empresa", "Mercado", "Cota├º├úo", "Corretora"],
				rows,
				function (a) {
					return [
						esc(a.id),
						esc(a.ticker),
						esc(a.nomeEmpresa),
						esc(a.mercado),
						esc(a.cotacaoAtual),
						esc(a.corretoraId),
					];
				}
			);
			document.getElementById("acoes-json").hidden = true;
		} catch (err) {
			setStatus(acoesStatus, err.message, true);
		}
	});

	document.getElementById("form-acao-cadastro").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(acoesStatus, "Fa├ºa login.", true);
			return;
		}
		const fd = new FormData(e.target);
		const body = {
			ticker: String(fd.get("ticker") || "").trim().toUpperCase(),
			mercado: String(fd.get("mercado") || "BRASIL"),
		};
		const cid = fd.get("corretoraId");
		if (cid && String(cid).trim() !== "") {
			body.corretoraId = Number(cid);
		}
		setStatus(acoesStatus, "CadastrandoÔÇª", false);
		try {
			const data = await api("POST", "/acoes", body);
			setStatus(acoesStatus, "A├º├úo criada (ID " + data.id + ").", false);
			hideTable("acoes-out");
			showJson("acoes-json", data);
		} catch (err) {
			setStatus(acoesStatus, err.message, true);
		}
	});

	document.getElementById("form-acao-buscar-id").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(acoesStatus, "Fa├ºa login.", true);
			return;
		}
		const id = new FormData(e.target).get("id");
		setStatus(acoesStatus, "BuscandoÔÇª", false);
		try {
			const data = await api("GET", "/acoes/" + encodeURIComponent(id));
			setStatus(acoesStatus, "OK.", false);
			hideTable("acoes-out");
			showJson("acoes-json", data);
		} catch (err) {
			setStatus(acoesStatus, err.message, true);
		}
	});

	document.getElementById("form-acao-buscar-ticker").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(acoesStatus, "Fa├ºa login.", true);
			return;
		}
		const t = String(new FormData(e.target).get("ticker") || "").trim();
		setStatus(acoesStatus, "BuscandoÔÇª", false);
		try {
			const data = await api("GET", "/acoes/ticker/" + encodeURIComponent(t));
			setStatus(acoesStatus, "OK.", false);
			hideTable("acoes-out");
			showJson("acoes-json", data);
		} catch (err) {
			setStatus(acoesStatus, err.message, true);
		}
	});

	document.getElementById("form-acao-atualizar-cotacao").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(acoesStatus, "Fa├ºa login.", true);
			return;
		}
		const id = new FormData(e.target).get("id");
		setStatus(acoesStatus, "Atualizando cota├º├úoÔÇª", false);
		try {
			const data = await api("PUT", "/acoes/" + encodeURIComponent(id) + "/atualizar-cotacao");
			setStatus(acoesStatus, "Cota├º├úo atualizada.", false);
			hideTable("acoes-out");
			showJson("acoes-json", data);
		} catch (err) {
			setStatus(acoesStatus, err.message, true);
		}
	});

	document.getElementById("form-acao-excluir").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(acoesStatus, "Fa├ºa login.", true);
			return;
		}
		const id = new FormData(e.target).get("id");
		if (!window.confirm("Excluir definitivamente a a├º├úo ID " + id + "?")) {
			return;
		}
		setStatus(acoesStatus, "ExcluindoÔÇª", false);
		try {
			await api("DELETE", "/acoes/" + encodeURIComponent(id));
			setStatus(acoesStatus, "A├º├úo ID " + id + " exclu├¡da com sucesso.", false);
			hideTable("acoes-out");
			document.getElementById("acoes-json").hidden = true;
			e.target.reset();
		} catch (err) {
			setStatus(acoesStatus, err.message, true);
		}
	});

	document.getElementById("form-chart").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(chartStatus, "Fa├ºa login antes de carregar o gr├ífico.", true);
			return;
		}
		const acaoId = new FormData(e.target).get("acaoId");
		setStatus(chartStatus, "CarregandoÔÇª", false);
		try {
			const page = await api("GET", "/acoes/" + encodeURIComponent(acaoId) + "/historico-cotacoes?size=500");
			const rows = page.content || [];
			if (rows.length === 0) {
				setStatus(chartStatus, "Nenhum ponto de hist├│rico para esta a├º├úo.", false);
				if (chart) {
					chart.destroy();
					chart = null;
				}
				return;
			}
			const labels = rows.map(function (r) {
				return r.dataHora;
			});
			const values = rows.map(function (r) {
				return Number(r.valor);
			});
			if (chart) {
				chart.destroy();
			}
			chart = new Chart(canvas.getContext("2d"), {
				type: "line",
				data: {
					labels: labels,
					datasets: [
						{
							label: "Cota├º├úo",
							data: values,
							borderColor: "#60a5fa",
							backgroundColor: "rgba(96,165,250,0.15)",
							fill: true,
							tension: 0.2,
							pointRadius: 0,
						},
					],
				},
				options: {
					responsive: true,
					maintainAspectRatio: false,
					scales: {
						x: {
							ticks: { maxRotation: 45, minRotation: 0, color: "#9aa5b1" },
							grid: { color: "#2d3848" },
						},
						y: {
							ticks: { color: "#9aa5b1" },
							grid: { color: "#2d3848" },
						},
					},
					plugins: {
						legend: { labels: { color: "#e8eaed" } },
					},
				},
			});
			setStatus(chartStatus, rows.length + " pontos carregados.", false);
		} catch (err) {
			setStatus(chartStatus, err.message, true);
		}
	});

	/* ---- Carteiras ---- */
	const carteirasStatus = document.getElementById("carteiras-status");

	document.getElementById("btn-carteiras-listar").addEventListener("click", async function () {
		if (!getToken()) {
			setStatus(carteirasStatus, "Fa├ºa login.", true);
			return;
		}
		setStatus(carteirasStatus, "CarregandoÔÇª", false);
		try {
			const page = await api("GET", "/carteiras?size=50");
			const rows = page.content || [];
			const rowsComMedia = await Promise.all(
				rows.map(function (c) {
					return api(
						"GET",
						"/carteiras/" + encodeURIComponent(c.id) + "/indicadores/media-carteira"
					)
						.then(function (ind) {
							return Object.assign({}, c, {
								mediaValorMercadoPorTitulo: ind.mediaValorMercadoPorTitulo,
							});
						})
						.catch(function () {
							return Object.assign({}, c, { mediaValorMercadoPorTitulo: null });
						});
				})
			);
			setStatus(carteirasStatus, rows.length + " carteiras.", false);
			renderPageTable(
				"carteiras-out",
				[
					"ID",
					"Nome",
					"Saldo",
					"Rentab. %",
					"Corretora",
					"Posi├º├Áes",
					"M├®dia das a├º├Áes",
				],
				rowsComMedia,
				function (c) {
					const n = c.posicoes ? c.posicoes.length : 0;
					return [
						esc(c.id),
						esc(c.nomeDaCarteira),
						esc(c.saldoTotal),
						esc(c.rentabilidadeAcumulada),
						esc(c.corretoraId),
						esc(n),
						esc(c.mediaValorMercadoPorTitulo),
					];
				}
			);
			document.getElementById("carteiras-json").hidden = true;
		} catch (err) {
			setStatus(carteirasStatus, err.message, true);
		}
	});

	document.getElementById("form-carteira-cadastro").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(carteirasStatus, "Fa├ºa login.", true);
			return;
		}
		const fd = new FormData(e.target);
		const body = {
			usuarioId: Number(fd.get("usuarioId")),
			nomeDaCarteira: String(fd.get("nomeDaCarteira") || "").trim(),
		};
		const corId = fd.get("corretoraId");
		if (corId && String(corId).trim() !== "") {
			body.corretoraId = Number(corId);
		}
		const saldo = String(fd.get("saldoInicial") || "").trim();
		if (saldo !== "") {
			body.saldoInicial = saldo;
		}
		setStatus(carteirasStatus, "Criando carteiraÔÇª", false);
		try {
			const data = await api("POST", "/carteiras", body);
			setStatus(carteirasStatus, "Carteira criada (ID " + data.id + ").", false);
			hideTable("carteiras-out");
			showJson("carteiras-json", data);
		} catch (err) {
			setStatus(carteirasStatus, err.message, true);
		}
	});

	document.getElementById("form-carteira-buscar-id").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(carteirasStatus, "Fa├ºa login.", true);
			return;
		}
		const id = new FormData(e.target).get("id");
		setStatus(carteirasStatus, "BuscandoÔÇª", false);
		try {
			const data = await api("GET", "/carteiras/" + encodeURIComponent(id));
			setStatus(carteirasStatus, "OK.", false);
			hideTable("carteiras-out");
			showJson("carteiras-json", data);
		} catch (err) {
			setStatus(carteirasStatus, err.message, true);
		}
	});

	document.getElementById("form-carteira-por-usuario").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(carteirasStatus, "Fa├ºa login.", true);
			return;
		}
		const uid = new FormData(e.target).get("usuarioId");
		setStatus(carteirasStatus, "CarregandoÔÇª", false);
		try {
			const list = await api("GET", "/carteiras/usuario/" + encodeURIComponent(uid));
			const n = Array.isArray(list) ? list.length : 0;
			setStatus(carteirasStatus, n + " carteiras.", false);
			showJson("carteiras-json", list);
			document.getElementById("carteiras-json").hidden = false;
			hideTable("carteiras-out");
		} catch (err) {
			setStatus(carteirasStatus, err.message, true);
		}
	});

	document.getElementById("form-carteira-atualizar").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(carteirasStatus, "Fa├ºa login.", true);
			return;
		}
		const fd = new FormData(e.target);
		const id = fd.get("id");
		const body = {};
		const nome = String(fd.get("nomeDaCarteira") || "").trim();
		const corId = fd.get("corretoraId");
		if (nome) {
			body.nomeDaCarteira = nome;
		}
		if (corId && String(corId).trim() !== "") {
			body.corretoraId = Number(corId);
		}
		setStatus(carteirasStatus, "AtualizandoÔÇª", false);
		try {
			const data = await api("PUT", "/carteiras/" + encodeURIComponent(id), body);
			setStatus(carteirasStatus, "Carteira atualizada.", false);
			hideTable("carteiras-out");
			showJson("carteiras-json", data);
		} catch (err) {
			setStatus(carteirasStatus, err.message, true);
		}
	});

	const formCompra = document.getElementById("form-compra");
	const compraAcaoId = document.getElementById("compra-acao-id");
	const compraQuantidade = document.getElementById("compra-quantidade");
	const compraPrecoDisplay = document.getElementById("compra-preco-display");
	const compraCustoTotal = document.getElementById("compra-custo-total");
	let compraPrecoNum = null;

	function atualizarCompraCustoEstimado() {
		const qStr = String(compraQuantidade.value || "").trim().replace(",", ".");
		const q = parseFloat(qStr);
		if (compraPrecoNum == null || !Number.isFinite(q) || q <= 0) {
			compraCustoTotal.textContent = "ÔÇö";
			return;
		}
		const total = q * compraPrecoNum;
		compraCustoTotal.textContent = total.toLocaleString("pt-BR", {
			minimumFractionDigits: 2,
			maximumFractionDigits: 8,
		});
	}

	async function carregarCotacaoCompra() {
		if (!getToken()) {
			return;
		}
		const id = String(compraAcaoId.value || "").trim();
		if (!id) {
			compraPrecoDisplay.value = "";
			compraPrecoNum = null;
			atualizarCompraCustoEstimado();
			return;
		}
		try {
			const data = await api("GET", "/acoes/" + encodeURIComponent(id));
			if (data.cotacaoAtual == null || data.cotacaoAtual === "") {
				compraPrecoDisplay.value = "(sem cota├º├úo no cadastro ÔÇö estimativa indispon├¡vel)";
				compraPrecoNum = null;
			} else {
				compraPrecoNum = Number(data.cotacaoAtual);
				compraPrecoDisplay.value =
					typeof data.cotacaoAtual === "number"
						? String(data.cotacaoAtual)
						: String(data.cotacaoAtual).trim();
			}
		} catch (err) {
			compraPrecoDisplay.value = "";
			compraPrecoNum = null;
			setStatus(carteirasStatus, err.message, true);
		}
		atualizarCompraCustoEstimado();
	}

	compraAcaoId.addEventListener("change", function () {
		carregarCotacaoCompra();
	});
	compraAcaoId.addEventListener("blur", function () {
		carregarCotacaoCompra();
	});
	compraQuantidade.addEventListener("input", atualizarCompraCustoEstimado);

	formCompra.addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(carteirasStatus, "Fa├ºa login.", true);
			return;
		}
		const fd = new FormData(e.target);
		const cid = fd.get("carteiraId");
		await carregarCotacaoCompra();
		const body = {
			acaoId: Number(fd.get("acaoId")),
			quantidade: String(fd.get("quantidade") || "").trim(),
		};
		setStatus(carteirasStatus, "Registrando compra (cota├º├úo ao vivo na API)ÔÇª", false);
		try {
			const data = await api("POST", "/carteiras/" + encodeURIComponent(cid) + "/compras", body);
			setStatus(carteirasStatus, "Compra registrada ao pre├ºo da cota├º├úo atual da API.", false);
			hideTable("carteiras-out");
			showJson("carteiras-json", data);
		} catch (err) {
			setStatus(carteirasStatus, err.message, true);
		}
	});

	document.getElementById("form-venda").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(carteirasStatus, "Fa├ºa login.", true);
			return;
		}
		const fd = new FormData(e.target);
		const cid = fd.get("carteiraId");
		const body = {
			acaoId: Number(fd.get("acaoId")),
			quantidade: String(fd.get("quantidade") || "").trim(),
		};
		setStatus(carteirasStatus, "Registrando venda (cota├º├úo ao vivo na API)ÔÇª", false);
		try {
			const data = await api("POST", "/carteiras/" + encodeURIComponent(cid) + "/vendas", body);
			setStatus(carteirasStatus, "Venda registrada ao pre├ºo da cota├º├úo atual da API.", false);
			hideTable("carteiras-out");
			showJson("carteiras-json", data);
		} catch (err) {
			setStatus(carteirasStatus, err.message, true);
		}
	});

	document.getElementById("form-indicador-ticker").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(carteirasStatus, "Fa├ºa login.", true);
			return;
		}
		const fd = new FormData(e.target);
		const cid = fd.get("carteiraId");
		const ticker = String(fd.get("ticker") || "").trim();
		if (!ticker) {
			setStatus(carteirasStatus, "Informe o ticker.", true);
			return;
		}
		setStatus(carteirasStatus, "Calculando indicador do tickerÔÇª", false);
		try {
			const data = await api(
				"GET",
				"/carteiras/" + encodeURIComponent(cid) + "/indicadores/ticker/" + encodeURIComponent(ticker)
			);
			setStatus(carteirasStatus, "Indicador do ticker (pre├ºo m├®dio e mercado).", false);
			hideTable("carteiras-out");
			showJson("carteiras-json", data);
		} catch (err) {
			setStatus(carteirasStatus, err.message, true);
		}
	});

	document.getElementById("form-indicador-media-carteira").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(carteirasStatus, "Fa├ºa login.", true);
			return;
		}
		const fd = new FormData(e.target);
		const cid = fd.get("carteiraId");
		setStatus(carteirasStatus, "Calculando indicadores da carteiraÔÇª", false);
		try {
			const data = await api("GET", "/carteiras/" + encodeURIComponent(cid) + "/indicadores/media-carteira");
			setStatus(carteirasStatus, "M├®dias e totais da carteira (cota├º├úo ao vivo).", false);
			hideTable("carteiras-out");
			showJson("carteiras-json", data);
		} catch (err) {
			setStatus(carteirasStatus, err.message, true);
		}
	});

	document.getElementById("form-carteira-excluir").addEventListener("submit", async function (e) {
		e.preventDefault();
		if (!getToken()) {
			setStatus(carteirasStatus, "Fa├ºa login.", true);
			return;
		}
		const id = new FormData(e.target).get("id");
		if (!window.confirm("Excluir definitivamente a carteira ID " + id + "? O saldo deve estar zerado e sem a├º├Áes em posi├º├úo.")) {
			return;
		}
		setStatus(carteirasStatus, "Excluindo carteiraÔÇª", false);
		try {
			await api("DELETE", "/carteiras/" + encodeURIComponent(id));
			setStatus(carteirasStatus, "Carteira ID " + id + " exclu├¡da com sucesso.", false);
			hideTable("carteiras-out");
			document.getElementById("carteiras-json").hidden = true;
			e.target.reset();
		} catch (err) {
			setStatus(carteirasStatus, err.message, true);
		}
	});
})();
