import ScrollReveal from "./ScrollReveal";

export default function SecuritySection() {
	return (
		<section className="landing-section security-section">
			<div className="landing-container security-section__panel">
				<ScrollReveal className="security-section__copy" direction="left">
					<p className="landing-eyebrow">Conta e acesso</p>
					<h2>Seu perfil e seus acessos, no lugar certo.</h2>
					<p>Dados pessoais, preferências de perfil e áreas administrativas são apresentados conforme o tipo de usuário conectado.</p>
				</ScrollReveal>
				<ScrollReveal className="security-steps" direction="right" delay={100}>
					<div className="security-step"><span><UserIcon /></span><div><strong>Perfil pessoal</strong><small>Informações e preferências da conta</small></div><b>01</b></div>
					<div className="security-connector" aria-hidden="true" />
					<div className="security-step"><span><ShieldIcon /></span><div><strong>Sessão protegida</strong><small>Autenticação e acesso controlado</small></div><b>02</b></div>
					<div className="security-connector" aria-hidden="true" />
					<div className="security-step"><span><AccessIcon /></span><div><strong>Perfis de acesso</strong><small>Padrão e Administrador</small></div><b>03</b></div>
				</ScrollReveal>
			</div>
		</section>
	);
}

function UserIcon() {
	return <svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="4" /><path d="M4.5 21a7.5 7.5 0 0 1 15 0" /></svg>;
}

function ShieldIcon() {
	return <svg viewBox="0 0 24 24"><path d="M12 3 5 6v5c0 4.5 2.9 8.1 7 10 4.1-1.9 7-5.5 7-10V6l-7-3Z" /><path d="m9 12 2 2 4-5" /></svg>;
}

function AccessIcon() {
	return <svg viewBox="0 0 24 24"><circle cx="8" cy="8" r="3" /><circle cx="17" cy="9" r="2.5" /><path d="M2.5 20a5.5 5.5 0 0 1 11 0M13 20a4.5 4.5 0 0 1 8.5-2" /></svg>;
}
