import ThemeToggle from "../components/ThemeToggle";

export default function AppHeader({ onToggleMenu, sidebarOpen, onToggleTheme, onLogout }) {
	return (
		<header className="app-header">
			<button
				type="button"
				id="btn-menu"
				className="btn-icon hamburger"
				aria-label="Abrir menu"
				aria-expanded={sidebarOpen ? "true" : "false"}
				aria-controls="sidebar"
				onClick={onToggleMenu}
			>
				☰
			</button>
			<span className="app-logo">GestãoAtiva</span>
			<div className="header-spacer" />
			<ThemeToggle onToggle={onToggleTheme} />
			<button type="button" id="btn-logout" className="btn-logout" onClick={onLogout}>
				Sair
			</button>
		</header>
	);
}
