import { useState } from "react";
import ThemeToggle from "../components/ThemeToggle";
import LoginForm from "./LoginForm";
import RegisterForm from "./RegisterForm";

export default function LoginScreen({ onToggleTheme }) {
	const [tab, setTab] = useState("login");
	const isLogin = tab === "login";

	return (
		<>
			<div className="theme-float" aria-hidden="false">
				<ThemeToggle onToggle={onToggleTheme} />
			</div>
			<div id="screen-login" className="screen-login" aria-label="Autenticação">
				<div className="login-backdrop" />
				<div className="login-center">
					<h1 className="brand-title">FinFEF</h1>
					<p className="brand-tagline">Gestão de corretoras, ações e carteiras</p>
					<div className="login-card card-elevated">
						<div className="login-tabs" role="tablist">
							<button
								type="button"
								id="tab-login"
								className={"login-tab" + (isLogin ? " login-tab--active" : "")}
								data-tab="login"
								role="tab"
								aria-selected={isLogin ? "true" : "false"}
								onClick={() => setTab("login")}
							>
								Entrar
							</button>
							<button
								type="button"
								id="tab-register"
								className={"login-tab" + (!isLogin ? " login-tab--active" : "")}
								data-tab="register"
								role="tab"
								aria-selected={!isLogin ? "true" : "false"}
								onClick={() => setTab("register")}
							>
								Cadastrar
							</button>
						</div>
						<div
							id="login-panel-login"
							className="login-panel"
							role="tabpanel"
							hidden={!isLogin}
						>
							{isLogin && <LoginForm />}
						</div>
						<div
							id="login-panel-register"
							className="login-panel"
							role="tabpanel"
							hidden={isLogin}
						>
							{!isLogin && <RegisterForm />}
						</div>
					</div>
					<p className="login-footnote">
						API REST · JWT · Integrações Brasil API, ViaCEP, CVM, BRAPI, Alpha Vantage
					</p>
				</div>
			</div>
		</>
	);
}
