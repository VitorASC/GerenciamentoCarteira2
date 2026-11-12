import { useState } from "react";
import { useAuth } from "./hooks/useAuth";
import { useTheme } from "./hooks/useTheme";
import LandingPage from "./landing/LandingPage";
import AppShell from "./layout/AppShell";
import LoginScreen from "./screens/LoginScreen";

export default function App() {
	const { clearSessionNotice, isAuthenticated, sessionNotice } = useAuth();
	const { toggle } = useTheme();
	const [publicView, setPublicView] = useState("landing");
	const [authTab, setAuthTab] = useState("login");

	function showAuth(tab = "login") {
		clearSessionNotice();
		setAuthTab(tab);
		setPublicView("auth");
	}

	function showLanding() {
		clearSessionNotice();
		setPublicView("landing");
	}

	if (isAuthenticated) {
		return <AppShell onToggleTheme={toggle} />;
	}

	if (publicView === "auth" || sessionNotice) {
		return (
			<LoginScreen
				initialTab={sessionNotice ? "login" : authTab}
				onBack={showLanding}
				onToggleTheme={toggle}
			/>
		);
	}

	return <LandingPage onLogin={() => showAuth("login")} onRegister={() => showAuth("register")} onToggleTheme={toggle} />;
}
