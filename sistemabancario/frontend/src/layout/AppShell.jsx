import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../hooks/useAuth";
import AcoesPanel from "../panels/AcoesPanel";
import CarteirasPanel from "../panels/CarteirasPanel";
import ContaPanel from "../panels/ContaPanel";
import CorretorasPanel from "../panels/CorretorasPanel";
import DashboardPanel from "../panels/DashboardPanel";
import { api } from "../services/api";
import AppHeader from "./AppHeader";
import Sidebar from "./Sidebar";

const PANEL_META = {
	dashboard: { title: "Dashboard" },
	corretoras: {
		title: "Corretoras",
		sub: "Cadastro e consulta com Brasil API e validação CVM.",
	},
	acoes: {
		title: "Ações e cotações",
		sub: "Ativos, histórico e gráfico de cotações.",
	},
	carteiras: {
		title: "Carteiras",
		sub: "Carteiras, compra, venda e indicadores.",
	},
	conta: {
		title: "Conta",
		sub: "Sessão JWT e usuários.",
	},
};

export default function AppShell({ onToggleTheme }) {
	const { logout, payload, usuarioId } = useAuth();
	const [activePanel, setActivePanel] = useState("dashboard");
	const [sidebarOpen, setSidebarOpen] = useState(false);
	const [sidebarCollapsed, setSidebarCollapsed] = useState(() => {
		try { return localStorage.getItem("gestaoativa.sidebar-collapsed") === "true"; }
		catch (_) { return false; }
	});
	const [user, setUser] = useState(() => ({ email: payload?.email || "" }));

	const meta = PANEL_META[activePanel] || PANEL_META.dashboard;

	useEffect(() => {
		if (!usuarioId) return;
		let active = true;
		api("GET", "/usuarios/" + encodeURIComponent(usuarioId))
			.then((data) => { if (active) setUser(data); })
			.catch(() => { if (active) setUser({ email: payload?.email || "" }); });
		return () => { active = false; };
	}, [usuarioId, payload?.email]);

	useEffect(() => {
		if (sidebarOpen) {
			document.body.classList.add("sidebar-open");
		} else {
			document.body.classList.remove("sidebar-open");
		}
		return () => document.body.classList.remove("sidebar-open");
	}, [sidebarOpen]);

	const handleSelect = useCallback((panel) => {
		setActivePanel(panel);
		setSidebarOpen(false);
	}, []);

	const handleToggleMenu = useCallback(() => {
		setSidebarOpen((s) => !s);
	}, []);

	const handleBackdrop = useCallback(() => {
		setSidebarOpen(false);
	}, []);

	const handleToggleCollapsed = useCallback(() => {
		setSidebarCollapsed((current) => {
			const next = !current;
			try { localStorage.setItem("gestaoativa.sidebar-collapsed", String(next)); } catch (_) { /* ignore */ }
			return next;
		});
	}, []);

	return (
		<div id="screen-app" className={"screen-app" + (sidebarCollapsed ? " screen-app--collapsed" : "")} aria-hidden="false">
			<Sidebar
				activePanel={activePanel}
				onSelect={handleSelect}
				sidebarOpen={sidebarOpen}
				onBackdropClick={handleBackdrop}
				collapsed={sidebarCollapsed}
				onToggleCollapsed={handleToggleCollapsed}
			/>
			<div className="app-workspace">
				<AppHeader
					title={meta.title}
					user={user}
					onToggleMenu={handleToggleMenu}
					sidebarOpen={sidebarOpen}
					onToggleTheme={onToggleTheme}
					onLogout={logout}
				/>
				<main className="app-main">
					<PanelContainer panel="dashboard" active={activePanel}>
						<DashboardPanel onNavigate={handleSelect} />
					</PanelContainer>
					<PanelContainer panel="corretoras" active={activePanel}>
						<CorretorasPanel />
					</PanelContainer>
					<PanelContainer panel="acoes" active={activePanel}>
						<AcoesPanel />
					</PanelContainer>
					<PanelContainer panel="carteiras" active={activePanel}>
						<CarteirasPanel />
					</PanelContainer>
					<PanelContainer panel="conta" active={activePanel}>
						<ContaPanel />
					</PanelContainer>
				</main>
			</div>
		</div>
	);
}

function PanelContainer({ panel, active, children }) {
	const isActive = panel === active;
	const cls = "panel" + (isActive ? " panel--active" : "");
	if (!isActive) {
		return null;
	}
	return (
		<div id={"panel-" + panel} className={cls} data-panel-id={panel}>
			{children}
		</div>
	);
}
