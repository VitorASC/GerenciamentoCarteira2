import ScrollReveal from "./ScrollReveal";

export default function FinalCta({ onLogin, onRegister }) {
	return (
		<section className="final-cta-section">
			<div className="landing-container">
				<ScrollReveal className="final-cta">
					<div className="final-cta__pattern" aria-hidden="true" />
					<p className="landing-eyebrow">Comece agora</p>
					<h2>Pronto para organizar seus investimentos?</h2>
					<p>Crie sua conta e reúna as informações essenciais da sua carteira em uma experiência única.</p>
					<div><button type="button" className="landing-button landing-button--light landing-button--large" onClick={onRegister}>Criar conta <span aria-hidden="true">→</span></button><button type="button" className="landing-button landing-button--ghost landing-button--large" onClick={onLogin}>Já tenho uma conta</button></div>
				</ScrollReveal>
			</div>
		</section>
	);
}
