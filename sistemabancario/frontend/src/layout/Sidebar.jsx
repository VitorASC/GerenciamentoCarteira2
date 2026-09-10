const NAV_ITEMS = [
	{ id: "dashboard", label: "Dashboard", icon: "dashboard" },
	{ id: "carteiras", label: "Carteiras", icon: "wallet" },
	{ id: "acoes", label: "Ações", icon: "chart" },
	{ id: "corretoras", label: "Corretoras", icon: "building" },
	{ id: "conta", label: "Conta", icon: "user" },
];

export default function Sidebar({
	activePanel,
	onSelect,
	sidebarOpen,
	onBackdropClick,
	collapsed,
	onToggleCollapsed,
}) {
	return (
		<>
			<div
				id="sidebar-backdrop"
				className={"sidebar-backdrop" + (sidebarOpen ? "" : " is-hidden")}
				aria-hidden={sidebarOpen ? "false" : "true"}
				onClick={onBackdropClick}
			/>
			<aside
				id="sidebar"
				className={"sidebar" + (sidebarOpen ? " sidebar--open" : "")}
				aria-label="Menu principal"
			>
				<div className="sidebar-brand">
					<span className="sidebar-brand-symbol" aria-hidden="true">
						<BrandIcon />
					</span>
					<span className="sidebar-brand-name">GestãoAtiva</span>
				</div>
				<nav className="sidebar-nav">
					{NAV_ITEMS.map((item) => {
						const cls =
							"sidebar-link" + (activePanel === item.id ? " sidebar-link--active" : "");
						return (
							<button
								key={item.id}
								type="button"
								className={cls}
								data-panel={item.id}
								title={collapsed ? item.label : undefined}
								aria-label={item.label}
								onClick={() => onSelect(item.id)}
							>
								<NavIcon name={item.icon} />
								<span className="sidebar-link-label">{item.label}</span>
							</button>
						);
					})}
				</nav>
				<button
					type="button"
					className="sidebar-collapse"
					onClick={onToggleCollapsed}
					aria-label={collapsed ? "Expandir menu lateral" : "Recolher menu lateral"}
					title={collapsed ? "Expandir menu" : "Recolher menu"}
				>
					<ChevronIcon collapsed={collapsed} />
					<span className="sidebar-collapse-label">Recolher menu</span>
				</button>
			</aside>
		</>
	);
}

function BrandIcon() {
	return (
		<svg viewBox="0 0 24 24" aria-hidden="true">
			<path d="M4 17l5-5 4 3 7-8" />
			<path d="M15 7h5v5" />
		</svg>
	);
}

function NavIcon({ name }) {
	const paths = {
		dashboard: <><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" /></>,
		wallet: <><path d="M4 7.5h14a2 2 0 0 1 2 2v9H5a2 2 0 0 1-2-2v-11a2 2 0 0 1 2-2h11" /><path d="M20 12h-5a2 2 0 0 0 0 4h5" /></>,
		chart: <><path d="M4 19V9" /><path d="M10 19V5" /><path d="M16 19v-7" /><path d="M22 19H2" /></>,
		building: <><path d="M4 21V5l8-3 8 3v16" /><path d="M8 8h2M14 8h2M8 12h2M14 12h2M8 16h2M14 16h2" /></>,
		user: <><circle cx="12" cy="8" r="4" /><path d="M4.5 21a7.5 7.5 0 0 1 15 0" /></>,
	};
	return <svg className="sidebar-icon" viewBox="0 0 24 24" aria-hidden="true">{paths[name]}</svg>;
}

function ChevronIcon({ collapsed }) {
	return (
		<svg className="sidebar-icon" viewBox="0 0 24 24" aria-hidden="true">
			<path d={collapsed ? "m9 5 7 7-7 7" : "m15 5-7 7 7 7"} />
		</svg>
	);
}
