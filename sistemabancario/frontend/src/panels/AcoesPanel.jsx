import { useCallback, useState } from "react";
import CotacoesChart from "../components/CotacoesChart";
import DataTable from "../components/DataTable";
import JsonOutput from "../components/JsonOutput";
import StatusMessage from "../components/StatusMessage";
import { useAuth } from "../hooks/useAuth";
import { api } from "../services/api";
import { esc } from "../services/format";

const COLUMNS = ["ID", "Ticker", "Empresa", "Mercado", "Cotação", "Corretora"];

function rowMapper(a) {
	return [
		esc(a.id),
		esc(a.ticker),
		esc(a.nomeEmpresa),
		esc(a.mercado),
		esc(a.cotacaoAtual),
		esc(a.corretoraId),
	];
}

export default function AcoesPanel() {
	const { isAuthenticated } = useAuth();
	const [status, setStatus] = useState({ text: "", error: false });
	const [tableRows, setTableRows] = useState(null);
	const [jsonData, setJsonData] = useState(null);

	const [chartStatus, setChartStatus] = useState({ text: "", error: false });
	const [chartPoints, setChartPoints] = useState(null);

	const [cadastro, setCadastro] = useState({ ticker: "", mercado: "BRASIL", corretoraId: "" });
	const [buscaIdValue, setBuscaIdValue] = useState("");
	const [buscaTickerValue, setBuscaTickerValue] = useState("");
	const [atualizarIdValue, setAtualizarIdValue] = useState("");
	const [excluirIdValue, setExcluirIdValue] = useState("");
	const [chartIdValue, setChartIdValue] = useState("");

	const requireAuth = useCallback(
		(setStatusFn) => {
			if (!isAuthenticated) {
				(setStatusFn || setStatus)({ text: "Faça login.", error: true });
				return false;
			}
			return true;
		},
		[isAuthenticated]
	);

	const handleListar = useCallback(async () => {
		if (!requireAuth()) return;
		setStatus({ text: "Carregando…", error: false });
		try {
			const page = await api("GET", "/acoes?size=50");
			const rows = page.content || [];
			setStatus({ text: rows.length + " ações.", error: false });
			setTableRows(rows);
			setJsonData(null);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}, [requireAuth]);

	async function handleCadastro(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		const body = {
			ticker: cadastro.ticker.trim().toUpperCase(),
			mercado: cadastro.mercado || "BRASIL",
		};
		if (cadastro.corretoraId && String(cadastro.corretoraId).trim() !== "") {
			body.corretoraId = Number(cadastro.corretoraId);
		}
		setStatus({ text: "Cadastrando…", error: false });
		try {
			const data = await api("POST", "/acoes", body);
			setStatus({ text: "Ação criada (ID " + data.id + ").", error: false });
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
			const data = await api("GET", "/acoes/" + encodeURIComponent(buscaIdValue));
			setStatus({ text: "OK.", error: false });
			setTableRows(null);
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleBuscaTicker(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		setStatus({ text: "Buscando…", error: false });
		try {
			const data = await api("GET", "/acoes/ticker/" + encodeURIComponent(buscaTickerValue.trim()));
			setStatus({ text: "OK.", error: false });
			setTableRows(null);
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleAtualizar(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		setStatus({ text: "Atualizando cotação…", error: false });
		try {
			const data = await api(
				"PUT",
				"/acoes/" + encodeURIComponent(atualizarIdValue) + "/atualizar-cotacao"
			);
			setStatus({ text: "Cotação atualizada.", error: false });
			setTableRows(null);
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleExcluir(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		if (!window.confirm("Excluir definitivamente a ação ID " + excluirIdValue + "?")) return;
		setStatus({ text: "Excluindo…", error: false });
		try {
			await api("DELETE", "/acoes/" + encodeURIComponent(excluirIdValue));
			setStatus({
				text: "Ação ID " + excluirIdValue + " excluída com sucesso.",
				error: false,
			});
			setTableRows(null);
			setJsonData(null);
			setExcluirIdValue("");
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleChart(e) {
		e.preventDefault();
		if (!isAuthenticated) {
			setChartStatus({ text: "Faça login antes de carregar o gráfico.", error: true });
			return;
		}
		setChartStatus({ text: "Carregando…", error: false });
		try {
			const page = await api(
				"GET",
				"/acoes/" + encodeURIComponent(chartIdValue) + "/historico-cotacoes?size=500"
			);
			const rows = page.content || [];
			if (rows.length === 0) {
				setChartStatus({ text: "Nenhum ponto de histórico para esta ação.", error: false });
				setChartPoints(null);
				return;
			}
			setChartPoints(rows);
			setChartStatus({ text: rows.length + " pontos carregados.", error: false });
		} catch (err) {
			setChartStatus({ text: err.message, error: true });
		}
	}

	return (
		<div className="card section">
			<p className="hint">
				Listagens e cadastro; atualização de cotação via integração. Mercado BRASIL: BRAPI —
				sem token só PETR4, VALE3, MGLU3 e ITUB4. EUA: Alpha Vantage (
				<code>ALPHAVANTAGE_API_KEY</code>).
			</p>
			<div className="toolbar">
				<button type="button" id="btn-acoes-listar" onClick={handleListar}>
					Listar ações
				</button>
			</div>
			<form id="form-acao-cadastro" className="form-grid wide" onSubmit={handleCadastro}>
				<label>
					Ticker
					<input
						type="text"
						name="ticker"
						required
						placeholder="ex.: PETR4"
						value={cadastro.ticker}
						onChange={(e) => setCadastro({ ...cadastro, ticker: e.target.value })}
					/>
				</label>
				<label>
					Mercado
					<select
						name="mercado"
						required
						value={cadastro.mercado}
						onChange={(e) => setCadastro({ ...cadastro, mercado: e.target.value })}
					>
						<option value="BRASIL">BRASIL</option>
						<option value="ESTADOS_UNIDOS">ESTADOS_UNIDOS</option>
					</select>
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
				<button type="submit">Cadastrar ação</button>
			</form>
			<form id="form-acao-buscar-id" className="form-inline" onSubmit={handleBuscaId}>
				<label>
					ID
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
			<form id="form-acao-buscar-ticker" className="form-inline" onSubmit={handleBuscaTicker}>
				<label>
					Ticker
					<input
						type="text"
						name="ticker"
						required
						value={buscaTickerValue}
						onChange={(e) => setBuscaTickerValue(e.target.value)}
					/>
				</label>
				<button type="submit">Buscar por ticker</button>
			</form>
			<form
				id="form-acao-atualizar-cotacao"
				className="form-inline"
				onSubmit={handleAtualizar}
			>
				<label>
					ID da ação
					<input
						type="number"
						name="id"
						min="1"
						required
						value={atualizarIdValue}
						onChange={(e) => setAtualizarIdValue(e.target.value)}
					/>
				</label>
				<button type="submit">Atualizar cotação</button>
			</form>
			<h3 className="block-title">Excluir ação</h3>
			<p className="hint">
				Só é possível excluir ações sem posições em carteiras nem transações registradas.
			</p>
			<form id="form-acao-excluir" className="form-inline" onSubmit={handleExcluir}>
				<label>
					ID da ação
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
					Excluir ação
				</button>
			</form>
			<h3 className="block-title">Histórico de cotações (gráfico)</h3>
			<form id="form-chart" className="form-inline" onSubmit={handleChart}>
				<label>
					ID da ação
					<input
						type="number"
						name="acaoId"
						min="1"
						required
						placeholder="ex.: 1"
						value={chartIdValue}
						onChange={(e) => setChartIdValue(e.target.value)}
					/>
				</label>
				<button type="submit">Carregar histórico</button>
			</form>
			<StatusMessage status={chartStatus} />
			{chartPoints && chartPoints.length > 0 ? (
				<CotacoesChart points={chartPoints} />
			) : (
				<div className="chart-wrap" />
			)}
			<StatusMessage status={status} />
			{tableRows && (
				<DataTable
					columns={COLUMNS}
					rows={tableRows}
					rowMapper={rowMapper}
					getRowKey={(a, i) => a.id ?? i}
				/>
			)}
			<JsonOutput data={jsonData} />
		</div>
	);
}
