import { useCallback, useState } from "react";
import DataTable from "../components/DataTable";
import JsonOutput from "../components/JsonOutput";
import StatusMessage from "../components/StatusMessage";
import { useAuth } from "../hooks/useAuth";
import { api } from "../services/api";
import { esc, stripDigits } from "../services/format";

const COLUMNS = ["ID", "CNPJ", "Razão social", "Cidade", "UF", "CVM"];

function rowMapper(c) {
	return [
		esc(c.id),
		esc(c.cnpj),
		esc(c.razaoSocial),
		esc(c.cidade),
		esc(c.uf),
		esc(c.validadaNaCvm),
	];
}

export default function CorretorasPanel() {
	const { isAuthenticated } = useAuth();
	const [status, setStatus] = useState({ text: "", error: false });
	const [tableRows, setTableRows] = useState(null);
	const [jsonData, setJsonData] = useState(null);

	const [cadastro, setCadastro] = useState({ cnpj: "", cep: "", numero: "", complemento: "" });
	const [buscaIdValue, setBuscaIdValue] = useState("");
	const [buscaCnpjValue, setBuscaCnpjValue] = useState("");
	const [excluirIdValue, setExcluirIdValue] = useState("");

	const requireAuth = useCallback(() => {
		if (!isAuthenticated) {
			setStatus({ text: "Faça login.", error: true });
			return false;
		}
		return true;
	}, [isAuthenticated]);

	const handleListar = useCallback(async () => {
		if (!requireAuth()) return;
		setStatus({ text: "Carregando…", error: false });
		try {
			const page = await api("GET", "/corretoras?size=50");
			const rows = page.content || [];
			setStatus({ text: rows.length + " corretoras.", error: false });
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
			cnpj: cadastro.cnpj.trim(),
			cep: cadastro.cep.trim(),
		};
		if (cadastro.numero.trim()) body.numero = cadastro.numero.trim();
		if (cadastro.complemento.trim()) body.complemento = cadastro.complemento.trim();
		setStatus({ text: "Cadastrando…", error: false });
		try {
			const data = await api("POST", "/corretoras", body);
			setStatus({ text: "Corretora criada (ID " + data.id + ").", error: false });
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
			const data = await api("GET", "/corretoras/" + encodeURIComponent(buscaIdValue));
			setStatus({ text: "OK.", error: false });
			setTableRows(null);
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleBuscaCnpj(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		const cnpj = stripDigits(buscaCnpjValue);
		setStatus({ text: "Buscando…", error: false });
		try {
			const data = await api("GET", "/corretoras/cnpj/" + encodeURIComponent(cnpj));
			setStatus({ text: "OK.", error: false });
			setTableRows(null);
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleExcluir(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		if (!window.confirm("Excluir definitivamente a corretora ID " + excluirIdValue + "?")) return;
		setStatus({ text: "Excluindo…", error: false });
		try {
			await api("DELETE", "/corretoras/" + encodeURIComponent(excluirIdValue));
			setStatus({
				text: "Corretora ID " + excluirIdValue + " excluída com sucesso.",
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
			<p className="hint">Integração Brasil API / CVM nos fluxos de cadastro.</p>
			<div className="toolbar">
				<button type="button" id="btn-corretoras-listar" onClick={handleListar}>
					Listar corretoras
				</button>
			</div>
			<form
				id="form-corretora-cadastro"
				className="form-grid wide"
				onSubmit={handleCadastro}
			>
				<label>
					CNPJ
					<input
						type="text"
						name="cnpj"
						required
						placeholder="00.000.000/0001-00 ou só dígitos"
						value={cadastro.cnpj}
						onChange={(e) => setCadastro({ ...cadastro, cnpj: e.target.value })}
					/>
				</label>
				<label>
					CEP
					<input
						type="text"
						name="cep"
						required
						value={cadastro.cep}
						onChange={(e) => setCadastro({ ...cadastro, cep: e.target.value })}
					/>
				</label>
				<label>
					Número
					<input
						type="text"
						name="numero"
						placeholder="opcional"
						value={cadastro.numero}
						onChange={(e) => setCadastro({ ...cadastro, numero: e.target.value })}
					/>
				</label>
				<label>
					Complemento
					<input
						type="text"
						name="complemento"
						placeholder="opcional"
						value={cadastro.complemento}
						onChange={(e) => setCadastro({ ...cadastro, complemento: e.target.value })}
					/>
				</label>
				<button type="submit">Cadastrar corretora</button>
			</form>
			<form id="form-corretora-buscar-id" className="form-inline" onSubmit={handleBuscaId}>
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
			<form id="form-corretora-buscar-cnpj" className="form-inline" onSubmit={handleBuscaCnpj}>
				<label>
					CNPJ
					<input
						type="text"
						name="cnpj"
						required
						value={buscaCnpjValue}
						onChange={(e) => setBuscaCnpjValue(e.target.value)}
					/>
				</label>
				<button type="submit">Buscar por CNPJ</button>
			</form>
			<h3 className="block-title">Excluir corretora</h3>
			<p className="hint">
				Só é possível excluir corretoras que não estão vinculadas a ações ou carteiras.
			</p>
			<form id="form-corretora-excluir" className="form-inline" onSubmit={handleExcluir}>
				<label>
					ID da corretora
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
					Excluir corretora
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
