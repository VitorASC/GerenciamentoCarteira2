import { useCallback, useEffect, useState } from "react";
import DataTable from "../components/DataTable";
import JsonOutput from "../components/JsonOutput";
import StatusMessage from "../components/StatusMessage";
import { useAuth } from "../hooks/useAuth";
import { api } from "../services/api";
import { esc, stripDigits } from "../services/format";

const COLUMNS = ["ID", "Nome", "E-mail", "CPF", "Perfil", "Ativo"];

function rowMapper(u) {
	return [
		esc(u.id),
		esc(u.nomeCompleto),
		esc(u.email),
		esc(u.cpf),
		esc(u.perfilInvestidor),
		esc(u.ativo),
	];
}

function JwtSummary({ payload }) {
	if (!payload) {
		return (
			<p id="jwt-summary" className="hint mono jwt-box">
				—
			</p>
		);
	}
	const uid = payload.sub;
	const email = payload.email != null ? payload.email : "—";
	const exp = payload.exp ? new Date(payload.exp * 1000).toLocaleString() : "—";
	return (
		<p id="jwt-summary" className="hint mono jwt-box">
			<strong>ID (subject)</strong>: {uid} · <strong>e-mail</strong>: {email} · exp: {exp}
		</p>
	);
}

export default function ContaPanel() {
	const { isAuthenticated, payload, usuarioId } = useAuth();
	const [status, setStatus] = useState({ text: "", error: false });
	const [tableRows, setTableRows] = useState(null);
	const [jsonData, setJsonData] = useState(null);

	const [buscaIdValue, setBuscaIdValue] = useState("");
	const [buscaCpfValue, setBuscaCpfValue] = useState("");
	const [atualizar, setAtualizar] = useState({
		id: "",
		nomeCompleto: "",
		email: "",
		perfilInvestidor: "",
		ativoOpt: "",
	});

	useEffect(() => {
		if (!usuarioId) return;
		setAtualizar((prev) => (prev.id ? prev : { ...prev, id: usuarioId }));
	}, [usuarioId]);

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
			const page = await api("GET", "/usuarios?size=50");
			const rows = page.content || [];
			setStatus({ text: rows.length + " usuários (página).", error: false });
			setTableRows(rows);
			setJsonData(null);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}, [requireAuth]);

	async function handleBuscaId(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		setStatus({ text: "Buscando…", error: false });
		try {
			const data = await api("GET", "/usuarios/" + encodeURIComponent(buscaIdValue));
			setStatus({ text: "OK.", error: false });
			setTableRows(null);
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	async function handleBuscaCpf(e) {
		e.preventDefault();
		if (!requireAuth()) return;
		const cpf = stripDigits(buscaCpfValue);
		if (cpf.length !== 11) {
			setStatus({ text: "CPF deve ter 11 dígitos.", error: true });
			return;
		}
		setStatus({ text: "Buscando…", error: false });
		try {
			const data = await api("GET", "/usuarios/cpf/" + encodeURIComponent(cpf));
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
		const body = {};
		const nome = atualizar.nomeCompleto.trim();
		const email = atualizar.email.trim();
		const perfil = atualizar.perfilInvestidor.trim();
		const ativoOpt = atualizar.ativoOpt.trim();
		if (nome) body.nomeCompleto = nome;
		if (email) body.email = email;
		if (perfil) body.perfilInvestidor = perfil;
		if (ativoOpt === "true") body.ativo = true;
		if (ativoOpt === "false") body.ativo = false;
		setStatus({ text: "Atualizando…", error: false });
		try {
			const data = await api(
				"PUT",
				"/usuarios/" + encodeURIComponent(atualizar.id),
				body
			);
			setStatus({ text: "Usuário atualizado.", error: false });
			setTableRows(null);
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	return (
		<>
			<div className="card section">
				<h3 className="block-title">Sessão</h3>
				<p className="hint">Token JWT armazenado no navegador após o login.</p>
				<JwtSummary payload={payload} />
			</div>
			<div className="card section">
				<h3 className="block-title">Usuários</h3>
				<p className="hint">Consultas e atualizações conforme permissões da API.</p>
				<div className="toolbar">
					<button type="button" id="btn-usuarios-listar" onClick={handleListar}>
						Listar usuários
					</button>
				</div>
				<form id="form-usuario-buscar-id" className="form-inline" onSubmit={handleBuscaId}>
					<label>
						ID
						<input
							type="number"
							name="id"
							min="1"
							required
							placeholder="ex.: 1"
							value={buscaIdValue}
							onChange={(e) => setBuscaIdValue(e.target.value)}
						/>
					</label>
					<button type="submit">Buscar por ID</button>
				</form>
				<form id="form-usuario-buscar-cpf" className="form-inline" onSubmit={handleBuscaCpf}>
					<label>
						CPF
						<input
							type="text"
							name="cpf"
							maxLength={14}
							placeholder="somente dígitos"
							value={buscaCpfValue}
							onChange={(e) => setBuscaCpfValue(e.target.value)}
						/>
					</label>
					<button type="submit">Buscar por CPF</button>
				</form>
				<h3 className="block-title">Atualizar meu cadastro</h3>
				<form id="form-usuario-atualizar" className="form-grid wide" onSubmit={handleAtualizar}>
					<label>
						ID do usuário
						<input
							type="number"
							name="id"
							min="1"
							required
							id="usuario-update-id"
							value={atualizar.id}
							onChange={(e) => setAtualizar({ ...atualizar, id: e.target.value })}
						/>
					</label>
					<label>
						Nome completo
						<input
							type="text"
							name="nomeCompleto"
							placeholder="opcional"
							value={atualizar.nomeCompleto}
							onChange={(e) =>
								setAtualizar({ ...atualizar, nomeCompleto: e.target.value })
							}
						/>
					</label>
					<label>
						E-mail
						<input
							type="email"
							name="email"
							placeholder="opcional"
							value={atualizar.email}
							onChange={(e) => setAtualizar({ ...atualizar, email: e.target.value })}
						/>
					</label>
					<label>
						Perfil
						<select
							name="perfilInvestidor"
							value={atualizar.perfilInvestidor}
							onChange={(e) =>
								setAtualizar({ ...atualizar, perfilInvestidor: e.target.value })
							}
						>
							<option value="">— não alterar —</option>
							<option value="CONSERVADOR">CONSERVADOR</option>
							<option value="MODERADO">MODERADO</option>
							<option value="ARROJADO">ARROJADO</option>
						</select>
					</label>
					<label>
						Conta ativa
						<select
							name="ativoOpt"
							value={atualizar.ativoOpt}
							onChange={(e) => setAtualizar({ ...atualizar, ativoOpt: e.target.value })}
						>
							<option value="">— não alterar —</option>
							<option value="true">Ativo</option>
							<option value="false">Inativo</option>
						</select>
					</label>
					<button type="submit">Salvar alterações</button>
				</form>
				<StatusMessage status={status} />
				{tableRows && (
					<DataTable
						columns={COLUMNS}
						rows={tableRows}
						rowMapper={rowMapper}
						getRowKey={(u, i) => u.id ?? i}
					/>
				)}
				<JsonOutput data={jsonData} />
			</div>
		</>
	);
}
