import { useCallback, useEffect, useMemo, useState } from "react";
import DataTable from "../components/DataTable";
import JsonOutput from "../components/JsonOutput";
import StatusMessage from "../components/StatusMessage";
import { useAuth } from "../hooks/useAuth";
import { api } from "../services/api";
import { esc } from "../services/format";

const COLUMNS = [
	"ID",
	"Nome",
	"Saldo",
	"Rentab. %",
	"Corretora",
	"Posições",
	"Média das ações",
];

function rowMapper(c) {
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

export default function CarteirasPanel() {
	const { isAuthenticated, usuarioId } = useAuth();
	const [status, setStatus] = useState({ text: "", error: false });
	const [tableRows, setTableRows] = useState(null);
	const [jsonData, setJsonData] = useState(null);

	const [cadastro, setCadastro] = useState({
		usuarioId: "",
		nomeDaCarteira: "",
		corretoraId: "",
		saldoInicial: "",
	});
	const [buscaIdValue, setBuscaIdValue] = useState("");
	const [buscaUsuarioValue, setBuscaUsuarioValue] = useState("");
	const [atualizar, setAtualizar] = useState({ id: "", nomeDaCarteira: "", corretoraId: "" });
	const [compra, setCompra] = useState({ carteiraId: "", acaoId: "", quantidade: "" });
	const [compraPrecoNum, setCompraPrecoNum] = useState(null);
	const [compraPrecoDisplay, setCompraPrecoDisplay] = useState("");
	const [venda, setVenda] = useState({ carteiraId: "", acaoId: "", quantidade: "" });
	const [indicadorTicker, setIndicadorTicker] = useState({ carteiraId: "", ticker: "" });
	const [indicadorMedia, setIndicadorMedia] = useState({ carteiraId: "" });
	const [excluirIdValue, setExcluirIdValue] = useState("");

	useEffect(() => {
		if (!usuarioId) return;
		setCadastro((prev) => (prev.usuarioId ? prev : { ...prev, usuarioId }));
		setBuscaUsuarioValue((prev) => (prev ? prev : usuarioId));
	}, [usuarioId]);

	const requireAuth = useCallback(() => {
		if (!isAuthenticated) {
			setStatus({ text: "Faça login.", error: true });
			return false;
		}
		return true;
	}, [isAuthenticated]);

	const compraCustoTotal = useMemo(() => {
		const qStr = String(compra.quantidade || "").trim().replace(",", ".");
		const q = parseFloat(qStr);
		if (compraPrecoNum == null || !Number.isFinite(q) || q <= 0) {
			return "—";
		}
		const total = q * compraPrecoNum;
		return total.toLocaleString("pt-BR", {
			minimumFractionDigits: 2,
			maximumFractionDigits: 8,
		});
	}, [compra.quantidade, compraPrecoNum]);

	const carregarCotacaoCompra = useCallback(
		async (id) => {
			if (!isAuthenticated) return;
			const trimmed = String(id || "").trim();
			if (!trimmed) {
				setCompraPrecoDisplay("");
				setCompraPrecoNum(null);
				return;
			}
			try {
				const data = await api("GET", "/acoes/" + encodeURIComponent(trimmed));
				if (data.cotacaoAtual == null || data.cotacaoAtual === "") {
					setCompraPrecoDisplay("(sem cotação no cadastro — estimativa indisponível)");
					setCompraPrecoNum(null);
				} else {
					setCompraPrecoNum(Number(data.cotacaoAtual));
					setCompraPrecoDisplay(
						typeof data.cotacaoAtual === "number"
							? String(data.cotacaoAtual)
							: String(data.cotacaoAtual).trim()
					);
				}
			} catch (err) {
				setCompraPrecoDisplay("");
				setCompraPrecoNum(null);
				setStatus({ text: err.message, error: true });
			}
		},
		[isAuthenticated]
	);

	useEffect(() => {
		if (!compra.acaoId) {
			setCompraPrecoDisplay("");
			setCompraPrecoNum(null);
			return;
		}
		const handle = setTimeout(() => {
			carregarCotacaoCompra(compra.acaoId);
		}, 250);
		return () => clearTimeout(handle);
	}, [compra.acaoId, carregarCotacaoCompra]);

	const handleListar = useCallback(async () => {
		if (!requireAuth()) return;
		setStatus({ text: "Carregando…", error: false });
		try {
			const page = await api("GET", "/carteiras?size=50");
			const rows = page.content || [];
			const rowsComMedia = await Promise.all(
				rows.map((c) =>
					api(
						"GET",
						"/carteiras/" + encodeURIComponent(c.id) + "/indicadores/media-carteira"
					)
						.then((ind) => ({
							...c,
							mediaValorMercadoPorTitulo: ind.mediaValorMercadoPorTitulo,
						}))
						.catch(() => ({ ...c, mediaValorMercadoPorTitulo: null }))
				)
			);
			setStatus({ text: rows.length + " carteiras.", error: false });
			setTableRows(rowsComMedia);
			setJsonData(null);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}, [requireAuth]);

	async function handleCadastro(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		const body = {
			usuarioId: Number(cadastro.usuarioId),
			nomeDaCarteira: cadastro.nomeDaCarteira.trim(),
		};
		if (cadastro.corretoraId && String(cadastro.corretoraId).trim() !== "") {
			body.corretoraId = Number(cadastro.corretoraId);
		}
		const saldo = String(cadastro.saldoInicial || "").trim();
		if (saldo !== "") body.saldoInicial = saldo;
		setStatus({ text: "Criando carteira…", error: false });
		try {
			const data = await api("POST", "/carteiras", body);
			setStatus({ text: "Carteira criada (ID " + data.id + ").", error: false });
			setTableRows(null);
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleBuscaId(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		setStatus({ text: "Buscando…", error: false });
		try {
			const data = await api("GET", "/carteiras/" + encodeURIComponent(buscaIdValue));
			setStatus({ text: "OK.", error: false });
			setTableRows(null);
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleBuscaUsuario(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		setStatus({ text: "Carregando…", error: false });
		try {
			const list = await api(
				"GET",
				"/carteiras/usuario/" + encodeURIComponent(buscaUsuarioValue)
			);
			const n = Array.isArray(list) ? list.length : 0;
			setStatus({ text: n + " carteiras.", error: false });
			setTableRows(null);
			setJsonData(list);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleAtualizar(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		const body = {};
		if (atualizar.nomeDaCarteira.trim()) body.nomeDaCarteira = atualizar.nomeDaCarteira.trim();
		if (atualizar.corretoraId && String(atualizar.corretoraId).trim() !== "") {
			body.corretoraId = Number(atualizar.corretoraId);
		}
		setStatus({ text: "Atualizando…", error: false });
		try {
			const data = await api(
				"PUT",
				"/carteiras/" + encodeURIComponent(atualizar.id),
				body
			);
			setStatus({ text: "Carteira atualizada.", error: false });
			setTableRows(null);
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleCompra(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		await carregarCotacaoCompra(compra.acaoId);
		const body = {
			acaoId: Number(compra.acaoId),
			quantidade: String(compra.quantidade || "").trim(),
		};
		setStatus({ text: "Registrando compra (cotação ao vivo na API)…", error: false });
		try {
			const data = await api(
				"POST",
				"/carteiras/" + encodeURIComponent(compra.carteiraId) + "/compras",
				body
			);
			setStatus({
				text: "Compra registrada ao preço da cotação atual da API.",
				error: false,
			});
			setTableRows(null);
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleVenda(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		const body = {
			acaoId: Number(venda.acaoId),
			quantidade: String(venda.quantidade || "").trim(),
		};
		setStatus({ text: "Registrando venda (cotação ao vivo na API)…", error: false });
		try {
			const data = await api(
				"POST",
				"/carteiras/" + encodeURIComponent(venda.carteiraId) + "/vendas",
				body
			);
			setStatus({
				text: "Venda registrada ao preço da cotação atual da API.",
				error: false,
			});
			setTableRows(null);
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleIndicadorTicker(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		const ticker = indicadorTicker.ticker.trim();
		if (!ticker) {
			setStatus({ text: "Informe o ticker.", error: true });
			return;
		}
		setStatus({ text: "Calculando indicador do ticker…", error: false });
		try {
			const data = await api(
				"GET",
				"/carteiras/" +
					encodeURIComponent(indicadorTicker.carteiraId) +
					"/indicadores/ticker/" +
					encodeURIComponent(ticker)
			);
			setStatus({
				text: "Indicador do ticker (preço médio e mercado).",
				error: false,
			});
			setTableRows(null);
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleIndicadorMedia(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		setStatus({ text: "Calculando indicadores da carteira…", error: false });
		try {
			const data = await api(
				"GET",
				"/carteiras/" +
					encodeURIComponent(indicadorMedia.carteiraId) +
					"/indicadores/media-carteira"
			);
			setStatus({
				text: "Médias e totais da carteira (cotação ao vivo).",
				error: false,
			});
			setTableRows(null);
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleExcluir(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		if (
			!window.confirm(
				"Excluir definitivamente a carteira ID " +
					excluirIdValue +
					"? O saldo deve estar zerado e sem ações em posição."
			)
		)
			return;
		setStatus({ text: "Excluindo carteira…", error: false });
		try {
			await api("DELETE", "/carteiras/" + encodeURIComponent(excluirIdValue));
			setStatus({
				text: "Carteira ID " + excluirIdValue + " excluída com sucesso.",
				error: false,
			});
			setTableRows(null);
			setJsonData(null);
			setExcluirIdValue("");
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	return (
		<div className="card section">
			<p className="hint">
				Operações usam o usuário autenticado; use o ID do usuário logado nos formulários.
			</p>
			<div className="toolbar">
				<button type="button" id="btn-carteiras-listar" onClick={handleListar}>
					Minhas carteiras
				</button>
			</div>
			<h3 className="block-title">Nova carteira</h3>
			<form id="form-carteira-cadastro" className="form-grid wide" onSubmit={handleCadastro}>
				<label>
					ID do usuário
					<input
						type="number"
						name="usuarioId"
						min="1"
						required
						id="carteira-usuario-id"
						value={cadastro.usuarioId}
						onChange={(e) => setCadastro({ ...cadastro, usuarioId: e.target.value })}
					/>
				</label>
				<label>
					Nome da carteira
					<input
						type="text"
						name="nomeDaCarteira"
						required
						value={cadastro.nomeDaCarteira}
						onChange={(e) => setCadastro({ ...cadastro, nomeDaCarteira: e.target.value })}
					/>
				</label>
				<label>
					ID da corretora
					<input
						type="number"
						name="corretoraId"
						min="1"
						placeholder="opcional"
						value={cadastro.corretoraId}
						onChange={(e) => setCadastro({ ...cadastro, corretoraId: e.target.value })}
					/>
				</label>
				<label>
					Saldo inicial
					<input
						type="text"
						name="saldoInicial"
						placeholder="ex.: 10000.00"
						value={cadastro.saldoInicial}
						onChange={(e) => setCadastro({ ...cadastro, saldoInicial: e.target.value })}
					/>
				</label>
				<button type="submit">Criar carteira</button>
			</form>
			<form id="form-carteira-buscar-id" className="form-inline" onSubmit={handleBuscaId}>
				<label>
					ID da carteira
					<input
						type="number"
						name="id"
						min="1"
						required
						value={buscaIdValue}
						onChange={(e) => setBuscaIdValue(e.target.value)}
					/>
				</label>
				<button type="submit">Buscar por ID</button>
			</form>
			<form
				id="form-carteira-por-usuario"
				className="form-inline"
				onSubmit={handleBuscaUsuario}
			>
				<label>
					ID do usuário
					<input
						type="number"
						name="usuarioId"
						min="1"
						required
						id="carteira-list-usuario-id"
						value={buscaUsuarioValue}
						onChange={(e) => setBuscaUsuarioValue(e.target.value)}
					/>
				</label>
				<button type="submit">Listar por usuário</button>
			</form>
			<h3 className="block-title">Atualizar carteira</h3>
			<form id="form-carteira-atualizar" className="form-grid wide" onSubmit={handleAtualizar}>
				<label>
					ID da carteira
					<input
						type="number"
						name="id"
						min="1"
						required
						value={atualizar.id}
						onChange={(e) => setAtualizar({ ...atualizar, id: e.target.value })}
					/>
				</label>
				<label>
					Nome
					<input
						type="text"
						name="nomeDaCarteira"
						placeholder="opcional"
						value={atualizar.nomeDaCarteira}
						onChange={(e) =>
							setAtualizar({ ...atualizar, nomeDaCarteira: e.target.value })
						}
					/>
				</label>
				<label>
					ID da corretora
					<input
						type="number"
						name="corretoraId"
						min="1"
						placeholder="opcional"
						value={atualizar.corretoraId}
						onChange={(e) => setAtualizar({ ...atualizar, corretoraId: e.target.value })}
					/>
				</label>
				<button type="submit">Salvar alterações</button>
			</form>
			<h3 className="block-title">Compra</h3>
			<p className="hint">
				Na compra, o servidor usa a <strong>cotação ao vivo</strong>; o valor abaixo é
				estimativa pelo cadastro.
			</p>
			<form id="form-compra" className="form-grid wide" onSubmit={handleCompra}>
				<label>
					ID da carteira
					<input
						type="number"
						name="carteiraId"
						min="1"
						required
						value={compra.carteiraId}
						onChange={(e) => setCompra({ ...compra, carteiraId: e.target.value })}
					/>
				</label>
				<label>
					ID da ação
					<input
						type="number"
						name="acaoId"
						min="1"
						required
						id="compra-acao-id"
						value={compra.acaoId}
						onChange={(e) => setCompra({ ...compra, acaoId: e.target.value })}
						onBlur={() => carregarCotacaoCompra(compra.acaoId)}
					/>
				</label>
				<label>
					Quantidade
					<input
						type="text"
						name="quantidade"
						required
						placeholder="ex.: 10"
						id="compra-quantidade"
						value={compra.quantidade}
						onChange={(e) => setCompra({ ...compra, quantidade: e.target.value })}
					/>
				</label>
				<label>
					Cotação no cadastro (estimativa)
					<input
						type="text"
						id="compra-preco-display"
						readOnly
						tabIndex={-1}
						placeholder="Atualiza ao informar o ID da ação"
						aria-live="polite"
						value={compraPrecoDisplay}
					/>
				</label>
				<p className="hint" id="compra-custo-par">
					Custo estimado (cadastro): <strong id="compra-custo-total">{compraCustoTotal}</strong>
				</p>
				<button type="submit">Registrar compra</button>
			</form>
			<h3 className="block-title">Venda</h3>
			<p className="hint">
				O crédito no saldo usa a <strong>cotação ao vivo</strong> no momento da venda.
			</p>
			<form id="form-venda" className="form-grid wide" onSubmit={handleVenda}>
				<label>
					ID da carteira
					<input
						type="number"
						name="carteiraId"
						min="1"
						required
						value={venda.carteiraId}
						onChange={(e) => setVenda({ ...venda, carteiraId: e.target.value })}
					/>
				</label>
				<label>
					ID da ação
					<input
						type="number"
						name="acaoId"
						min="1"
						required
						value={venda.acaoId}
						onChange={(e) => setVenda({ ...venda, acaoId: e.target.value })}
					/>
				</label>
				<label>
					Quantidade
					<input
						type="text"
						name="quantidade"
						required
						value={venda.quantidade}
						onChange={(e) => setVenda({ ...venda, quantidade: e.target.value })}
					/>
				</label>
				<button type="submit">Registrar venda</button>
			</form>
			<h3 className="block-title">Indicadores</h3>
			<p className="hint">
				<strong>Por ticker</strong>: preço médio e valores custo/mercado.{" "}
				<strong>Média da carteira</strong>: soma dos valores de mercado ÷ quantidade total de
				papéis.
			</p>
			<form
				id="form-indicador-ticker"
				className="form-inline wide"
				onSubmit={handleIndicadorTicker}
			>
				<label>
					ID da carteira
					<input
						type="number"
						name="carteiraId"
						min="1"
						required
						placeholder="ex.: 1"
						value={indicadorTicker.carteiraId}
						onChange={(e) =>
							setIndicadorTicker({ ...indicadorTicker, carteiraId: e.target.value })
						}
					/>
				</label>
				<label>
					Ticker
					<input
						type="text"
						name="ticker"
						required
						placeholder="PETR4"
						value={indicadorTicker.ticker}
						onChange={(e) =>
							setIndicadorTicker({ ...indicadorTicker, ticker: e.target.value })
						}
					/>
				</label>
				<button type="submit">Indicador por ticker</button>
			</form>
			<form
				id="form-indicador-media-carteira"
				className="form-inline wide"
				onSubmit={handleIndicadorMedia}
			>
				<label>
					ID da carteira
					<input
						type="number"
						name="carteiraId"
						min="1"
						required
						placeholder="ex.: 1"
						id="indicador-media-carteira-id"
						value={indicadorMedia.carteiraId}
						onChange={(e) =>
							setIndicadorMedia({ ...indicadorMedia, carteiraId: e.target.value })
						}
					/>
				</label>
				<button type="submit">Média da carteira inteira</button>
			</form>
			<h3 className="block-title">Excluir carteira</h3>
			<p className="hint">
				A carteira só pode ser excluída com <strong>saldo zerado</strong> e sem ações em
				posição (venda tudo antes).
			</p>
			<form id="form-carteira-excluir" className="form-inline" onSubmit={handleExcluir}>
				<label>
					ID da carteira
					<input
						type="number"
						name="id"
						min="1"
						required
						value={excluirIdValue}
						onChange={(e) => setExcluirIdValue(e.target.value)}
					/>
				</label>
				<button type="submit" className="btn-danger">
					Excluir carteira
				</button>
			</form>
			<StatusMessage status={status} />
			{tableRows && (
				<DataTable
					columns={COLUMNS}
					rows={tableRows}
					rowMapper={rowMapper}
					getRowKey={(c, i) => c.id ?? i}
				/>
			)}
			<JsonOutput data={jsonData} />
		</div>
	);
}
