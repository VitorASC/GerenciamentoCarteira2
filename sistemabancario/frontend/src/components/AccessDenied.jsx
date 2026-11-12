export default function AccessDenied({ user }) {
	const firstName = getFirstName(user?.nomeCompleto);
	const attention = firstName ? `Atenção, ${firstName}.` : "Atenção.";

	return (
		<section className="access-denied" aria-labelledby="access-denied-title">
			<div className="access-denied-card dashboard-card">
				<span className="access-denied-icon" aria-hidden="true">
					<svg viewBox="0 0 24 24">
						<path d="M12 3 4.5 6v5.3c0 4.7 3.1 8.2 7.5 9.7 4.4-1.5 7.5-5 7.5-9.7V6L12 3Z" />
						<rect x="8.5" y="10.5" width="7" height="5.5" rx="1.2" />
						<path d="M10 10.5V9a2 2 0 0 1 4 0v1.5" />
					</svg>
				</span>
				<div className="access-denied-copy">
					<span className="access-denied-eyebrow">Acesso restrito</span>
					<h2 id="access-denied-title">Esta área requer permissão de administrador</h2>
					<p><strong>{attention}</strong> Você não tem permissão para acessar esta tela.</p>
					<small>Esta funcionalidade está disponível apenas para usuários administradores.</small>
				</div>
			</div>
		</section>
	);
}

function getFirstName(fullName) {
	return String(fullName || "").trim().split(/\s+/)[0] || "";
}
