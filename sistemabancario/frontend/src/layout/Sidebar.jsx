const NAV_ITEMS = [
	{ id: "corretoras", label: "Corretoras" },
	{ id: "acoes", label: "Ações" },
	{ id: "carteiras", label: "Carteiras" },
	{ id: "conta", label: "Conta" },
];

export default function Sidebar({ activePanel, onSelect, sidebarOpen, onBackdropClick }) {
	return (
		<>
			<div
				id="sidebar-backdrop"
				className={"sidebar-backdrop" + (sidebarOpen ? "" : " is-hidden")}
				aria-hidden={sidebarOpen ? "false" : "true"}
				onClick={onBackdropClick}
			/>
			<aside id="sidebar" className="sidebar" aria-label="Menu principal">
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
								onClick={() => onSelect(item.id)}
							>
								{item.label}
							</button>
						);
					})}
				</nav>
			</aside>
		</>
	);
}
