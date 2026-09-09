import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../hooks/useAuth";
import AcoesPanel from "../panels/AcoesPanel";
import CarteirasPanel from "../panels/CarteirasPanel";
import ContaPanel from "../panels/ContaPanel";
import CorretorasPanel from "../panels/CorretorasPanel";
import AppHeader from "./AppHeader";
import Sidebar from "./Sidebar";

const PANEL_META = {
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
	const { logout } = useAuth();
	const [activePanel, setActivePanel] = useState("corretoras");
	const [sidebarOpen, setSidebarOpen] = useState(false);

	const meta = PANEL_META[activePanel] || PANEL_META.corretoras;

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

	return (
		<div id="screen-app" className="screen-app" aria-hidden="false">
			<AppHeader
				onToggleMenu={handleToggleMenu}
				sidebarOpen={sidebarOpen}
				onToggleTheme={onToggleTheme}
				onLogout={logout}
			/>
			<div className="app-shell">
				<Sidebar
					activePanel={activePanel}
					onSelect={handleSelect}
					sidebarOpen={sidebarOpen}
					onBackdropClick={handleBackdrop}
				/>
				<main className="app-main">
					<div className="main-head">
						<h2 id="app-main-title" className="main-title">
							{meta.title}
						</h2>
						<p id="app-main-sub" className="main-subtitle">
							{meta.sub}
						</p>
					</div>
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
