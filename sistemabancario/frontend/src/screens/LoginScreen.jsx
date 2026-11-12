import { useState } from "react";
import BrandMark from "../components/BrandMark";
import ThemeToggle from "../components/ThemeToggle";
import LoginForm from "./LoginForm";
import RegisterForm from "./RegisterForm";
import { useAuth } from "../hooks/useAuth";

export default function LoginScreen({ initialTab = "login", onBack, onToggleTheme }) {
	const { sessionNotice, clearSessionNotice } = useAuth();
	const [tab, setTab] = useState(initialTab);
	const [loginEmail, setLoginEmail] = useState("");
	const [loginNotice, setLoginNotice] = useState(null);
	const isLogin = tab === "login";

	function showLogin() {
		setLoginNotice(null);
		clearSessionNotice();
		setTab("login");
	}

	function showRegister() {
		setLoginNotice(null);
		clearSessionNotice();
		setTab("register");
	}

	function handleRegistrationSuccess(email) {
		clearSessionNotice();
		setLoginEmail(email);
		setLoginNotice({
			text: "Conta criada com sucesso. Faça login para continuar.",
			error: false,
		});
		setTab("login");
	}

	return (
		<main id="screen-login" className="screen-login" aria-label="Autenticação">
			<section className="auth-brand-panel" aria-labelledby="auth-brand-title">
				<div className="auth-brand-content">
					<BrandMark className="auth-brand-wordmark" />
					<h1 id="auth-brand-title">Seus investimentos, organizados em um só lugar.</h1>
					<p>
						Acompanhe sua carteira, preço médio, cotações e corretoras de forma simples e
						organizada.
					</p>
					<ul className="auth-benefits" aria-label="Recursos da plataforma">
						<li>Acompanhe sua carteira</li>
						<li>Monitore cotações</li>
						<li>Gerencie suas corretoras</li>
					</ul>
				</div>
				<MarketLines />
			</section>

			<section className={`auth-form-panel${isLogin ? "" : " auth-form-panel--register"}`}>
				<div className={`auth-card${isLogin ? "" : " auth-card--register"}`}>
					<div className="auth-card-toolbar">
						<button type="button" className="auth-back-link" onClick={onBack}>
							<span aria-hidden="true">←</span> Voltar ao início
						</button>
						<ThemeToggle onToggle={onToggleTheme} />
					</div>
					<div className="auth-card-brand"><BrandMark /></div>
					<div className="auth-card-heading">
						<p className="auth-eyebrow">{isLogin ? "Bem-vindo de volta" : "Comece agora"}</p>
						<h2>{isLogin ? "Acesse sua conta" : "Crie sua conta"}</h2>
						<p>
							{isLogin
								? "Entre para acompanhar seus investimentos."
								: "Organize seus investimentos em um só lugar."}
						</p>
					</div>
					<div className="auth-form-transition" key={tab}>
						{isLogin ? (
							<LoginForm initialEmail={loginEmail} initialStatus={loginNotice || sessionNotice} />
						) : (
							<RegisterForm onSuccess={handleRegistrationSuccess} />
						)}
					</div>
					<p className="auth-switch">
						{isLogin ? "Não possui uma conta?" : "Já possui uma conta?"}{" "}
						<button type="button" onClick={isLogin ? showRegister : showLogin}>
							{isLogin ? "Criar uma conta" : "Entrar"}
						</button>
					</p>
				</div>
			</section>
		</main>
	);
}

function MarketLines() {
	return (
		<svg className="auth-market-lines" viewBox="0 0 760 420" aria-hidden="true">
			<defs>
				<pattern id="market-grid" width="64" height="64" patternUnits="userSpaceOnUse">
					<path d="M64 0H0V64" />
				</pattern>
			</defs>
			<rect width="760" height="420" fill="url(#market-grid)" />
			<path className="market-path-shadow" d="M0 332C82 306 111 344 185 286s110-5 164-65 119-9 174-93 123-39 237-114" />
			<path className="market-path" d="M0 332C82 306 111 344 185 286s110-5 164-65 119-9 174-93 123-39 237-114" />
		</svg>
	);
}
