export default function ThemeToggle({ onToggle }) {
	return (
		<button
			type="button"
			className="btn-icon theme-toggle js-theme-toggle"
			title="Alternar tema claro/escuro"
			aria-label="Tema claro ou escuro"
			onClick={onToggle}
		>
			<span className="theme-icon theme-icon--sun" aria-hidden="true">
				☀
			</span>
			<span className="theme-sep">/</span>
			<span className="theme-icon theme-icon--moon" aria-hidden="true">
				☽
			</span>
		</button>
	);
}
