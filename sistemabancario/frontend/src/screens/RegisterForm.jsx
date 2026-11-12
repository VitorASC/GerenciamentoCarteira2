import { useState } from "react";
import PasswordField from "../components/PasswordField";
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
	perfilAcesso: "PADRAO",
};

export default function RegisterForm({ onSuccess }) {
	const [form, setForm] = useState(INITIAL);
	const [status, setStatus] = useState({ text: "", error: false });
	const [submitting, setSubmitting] = useState(false);

	function update(field, value) {
		setForm((prev) => ({ ...prev, [field]: value }));
	}

	async function handleSubmit(e) {
		e.preventDefault();
		if (submitting) return;
		setSubmitting(true);
		setStatus({ text: "Criando conta...", error: false });
		const body = {
			nomeCompleto: form.nomeCompleto.trim(),
			cpf: stripDigits(form.cpf),
			email: form.email.trim(),
			senha: form.senha,
			perfilInvestidor: form.perfilInvestidor || "MODERADO",
			perfilAcesso: form.perfilAcesso || "PADRAO",
		};
		if (form.dataNascimento) {
			body.dataNascimento = form.dataNascimento;
		}
		try {
			await api("POST", "/usuarios", body);
			onSuccess(form.email.trim());
		} catch (err) {
			setStatus({ text: err.message, error: true });
		} finally {
			setSubmitting(false);
		}
	}

	return (
		<>
			<form id="form-register" className="form-grid auth-form auth-form--register" onSubmit={handleSubmit}>
				<div className="auth-register-fields">
					<label className="auth-field" htmlFor="register-name">
						<span>Nome completo</span>
						<input
							id="register-name"
							type="text"
							name="nomeCompleto"
							required
							value={form.nomeCompleto}
							onChange={(e) => update("nomeCompleto", e.target.value)}
						/>
					</label>
					<label className="auth-field" htmlFor="register-cpf">
						<span>CPF</span>
						<input
							id="register-cpf"
							type="text"
							name="cpf"
							required
							maxLength={11}
							pattern="\d{11}"
							inputMode="numeric"
							autoComplete="off"
							placeholder="11 dígitos"
							value={form.cpf}
							onChange={(e) => update("cpf", e.target.value)}
						/>
					</label>
					<label className="auth-field" htmlFor="register-email">
						<span>E-mail</span>
						<input
							id="register-email"
							type="email"
							name="email"
							required
							autoComplete="email"
							value={form.email}
							onChange={(e) => update("email", e.target.value)}
						/>
					</label>
					<PasswordField
						id="register-password"
						value={form.senha}
						onChange={(e) => update("senha", e.target.value)}
						autoComplete="new-password"
						minLength={6}
					/>
					<label className="auth-field auth-field--stacked-label" htmlFor="register-birthdate">
						<span>Data de nascimento <small>opcional</small></span>
						<input
							id="register-birthdate"
							type="date"
							name="dataNascimento"
							value={form.dataNascimento}
							onChange={(e) => update("dataNascimento", e.target.value)}
						/>
					</label>
					<label className="auth-field auth-field--stacked-label" htmlFor="register-profile">
						<span>Perfil de investidor</span>
						<select
							id="register-profile"
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
					<label className="auth-field auth-field--stacked-label auth-field--access" htmlFor="register-access-profile">
						<span>Tipo de usuário</span>
						<select
							id="register-access-profile"
							name="perfilAcesso"
							required
							value={form.perfilAcesso}
							onChange={(e) => update("perfilAcesso", e.target.value)}
						>
							<option value="PADRAO">Usuário padrão</option>
							<option value="ADMIN">Administrador</option>
						</select>
						<small className="auth-field-hint">Define as permissões de acesso ao sistema.</small>
					</label>
				</div>
				<div className="auth-register-actions">
					<button type="submit" className="btn-primary-wide auth-submit" disabled={submitting}>
						{submitting && <span className="button-spinner" aria-hidden="true" />}
						{submitting ? "Criando conta..." : "Criar conta"}
					</button>
				</div>
			</form>
			<StatusMessage status={status} />
		</>
	);
}
