import { useState } from "react";
import JsonOutput from "../components/JsonOutput";
import StatusMessage from "../components/StatusMessage";
import { api } from "../services/api";
import { stripDigits } from "../services/format";

const INITIAL = {
	nomeCompleto: "",
	cpf: "",
	email: "",
	senha: "",
	dataNascimento: "",
	perfilInvestidor: "MODERADO",
};

export default function RegisterForm() {
	const [form, setForm] = useState(INITIAL);
	const [status, setStatus] = useState({ text: "", error: false });
	const [jsonData, setJsonData] = useState(null);

	function update(field, value) {
		setForm((prev) => ({ ...prev, [field]: value }));
	}

	async function handleSubmit(e) {
		e.preventDefault();
		setStatus({ text: "Enviando…", error: false });
		setJsonData(null);
		const body = {
			nomeCompleto: form.nomeCompleto.trim(),
			cpf: stripDigits(form.cpf),
			email: form.email.trim(),
			senha: form.senha,
			perfilInvestidor: form.perfilInvestidor || "MODERADO",
		};
		if (form.dataNascimento) {
			body.dataNascimento = form.dataNascimento;
		}
		try {
			const data = await api("POST", "/usuarios", body);
			setStatus({
				text: "Cadastro criado. ID " + data.id + ". Você pode entrar na aba Entrar.",
				error: false,
			});
			setJsonData(data);
		} catch (err) {
			setStatus({ text: err.message, error: true });
		}
	}

	return (
		<>
			<form id="form-register" className="form-grid" onSubmit={handleSubmit}>
				<label>
					Nome completo
					<input
						type="text"
						name="nomeCompleto"
						required
						value={form.nomeCompleto}
						onChange={(e) => update("nomeCompleto", e.target.value)}
					/>
				</label>
				<label>
					CPF (11 dígitos)
					<input
						type="text"
						name="cpf"
						required
						maxLength={11}
						pattern="\d{11}"
						value={form.cpf}
						onChange={(e) => update("cpf", e.target.value)}
					/>
				</label>
				<label>
					E-mail
					<input
						type="email"
						name="email"
						required
						value={form.email}
						onChange={(e) => update("email", e.target.value)}
					/>
				</label>
				<label>
					Senha
					<input
						type="password"
						name="senha"
						required
						minLength={6}
						value={form.senha}
						onChange={(e) => update("senha", e.target.value)}
					/>
				</label>
				<label>
					Data de nascimento
					<input
						type="date"
						name="dataNascimento"
						value={form.dataNascimento}
						onChange={(e) => update("dataNascimento", e.target.value)}
					/>
				</label>
				<label>
					Perfil
					<select
						name="perfilInvestidor"
						required
						value={form.perfilInvestidor}
						onChange={(e) => update("perfilInvestidor", e.target.value)}
					>
						<option value="CONSERVADOR">CONSERVADOR</option>
						<option value="MODERADO">MODERADO</option>
						<option value="ARROJADO">ARROJADO</option>
					</select>
				</label>
				<button type="submit" className="btn-primary-wide">
					Criar conta
				</button>
			</form>
			<StatusMessage status={status} />
			<JsonOutput data={jsonData} compact />
		</>
	);
}
