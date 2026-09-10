import { useState } from "react";
import PasswordField from "../components/PasswordField";
import StatusMessage from "../components/StatusMessage";
import { useAuth } from "../hooks/useAuth";
import { api } from "../services/api";

export default function LoginForm({ initialEmail = "", initialStatus = null }) {
	const { login } = useAuth();
	const [email, setEmail] = useState(initialEmail);
	const [senha, setSenha] = useState("");
	const [status, setStatus] = useState(initialStatus || { text: "", error: false });
	const [submitting, setSubmitting] = useState(false);

	async function handleSubmit(e) {
		e.preventDefault();
		if (submitting) return;
		setSubmitting(true);
		setStatus({ text: "Entrando…", error: false });
		try {
			const data = await api("POST", "/auth/login", {
				email: email.trim(),
				senha,
			});
			login(data && data.accessToken ? data.accessToken : "");
			setStatus({ text: "Login realizado.", error: false });
		} catch (err) {
			setStatus({ text: err.message, error: true });
		} finally {
			setSubmitting(false);
		}
	}

	return (
		<>
			<form id="form-login" className="form-grid auth-form" onSubmit={handleSubmit}>
				<label className="auth-field" htmlFor="login-email">
					<span>E-mail</span>
					<input
						id="login-email"
						type="email"
						name="email"
						required
						autoComplete="username"
						value={email}
						onChange={(e) => setEmail(e.target.value)}
					/>
				</label>
				<PasswordField
					id="login-password"
					value={senha}
					onChange={(e) => setSenha(e.target.value)}
					autoComplete="current-password"
				/>
				<button type="submit" className="btn-primary-wide auth-submit" disabled={submitting}>
					{submitting && <span className="button-spinner" aria-hidden="true" />}
					{submitting ? "Entrando..." : "Entrar"}
				</button>
			</form>
			<StatusMessage status={status} />
		</>
	);
}
