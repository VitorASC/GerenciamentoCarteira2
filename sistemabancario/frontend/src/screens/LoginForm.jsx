import { useState } from "react";
import StatusMessage from "../components/StatusMessage";
import { useAuth } from "../hooks/useAuth";
import { api } from "../services/api";

export default function LoginForm() {
	const { login } = useAuth();
	const [email, setEmail] = useState("");
	const [senha, setSenha] = useState("");
	const [status, setStatus] = useState({ text: "", error: false });

	async function handleSubmit(e) {
		e.preventDefault();
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
		}
	}

	return (
		<>
			<form id="form-login" className="form-grid" onSubmit={handleSubmit}>
				<label>
					E-mail
					<input
						type="email"
						name="email"
						required
						autoComplete="username"
						value={email}
						onChange={(e) => setEmail(e.target.value)}
					/>
				</label>
				<label>
					Senha
					<input
						type="password"
						name="senha"
						required
						autoComplete="current-password"
						value={senha}
						onChange={(e) => setSenha(e.target.value)}
					/>
				</label>
				<button type="submit" className="btn-primary-wide">
					Entrar
				</button>
			</form>
			<StatusMessage status={status} />
		</>
	);
}
