import ThemeToggle from "../components/ThemeToggle";

export default function AppHeader({
	title,
	user,
	onToggleMenu,
	sidebarOpen,
	onToggleTheme,
	onLogout,
}) {
	const displayName = user?.nomeCompleto || user?.email || "Usuário";
	const initials = displayName
		.split(/\s+/)
		.filter(Boolean)
		.slice(0, 2)
		.map((part) => part[0])
		.join("")
		.toUpperCase();

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
			<div className="topbar-context">
				<span className="topbar-kicker">Área de investimentos</span>
				<strong>{title}</strong>
			</div>
			<div className="header-spacer" />
			<div className="topbar-user">
				<span className="topbar-avatar" aria-hidden="true">{initials || "U"}</span>
				<span className="topbar-user-copy">
					<strong>{displayName}</strong>
					{user?.perfilInvestidor ? <small>{user.perfilInvestidor}</small> : null}
				</span>
			</div>
			<ThemeToggle onToggle={onToggleTheme} />
			<button type="button" id="btn-logout" className="btn-logout" onClick={onLogout}>
				<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 17l5-5-5-5M15 12H3M14 4h5a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-5" /></svg>
				<span>Sair</span>
			</button>
		</header>
	);
}
