import { DashboardMockup } from "./ProductMockups";
import ScrollReveal from "./ScrollReveal";

export default function HeroSection({ onLogin, onRegister }) {
	return (
		<section id="inicio" className="landing-hero">
			<div className="landing-hero__glow landing-hero__glow--one" aria-hidden="true" />
			<div className="landing-hero__glow landing-hero__glow--two" aria-hidden="true" />
			<div className="landing-container landing-hero__grid">
				<ScrollReveal className="landing-hero__copy" direction="left">
					<p className="landing-eyebrow"><span /> Gestão de investimentos</p>
					<h1>Seus investimentos, <em>organizados</em> em um só lugar.</h1>
					<p className="landing-hero__description">Acompanhe carteiras, posições, cotações e corretoras com informações organizadas para facilitar suas decisões.</p>
					<div className="landing-hero__actions">
						<button type="button" className="landing-button landing-button--primary landing-button--large" onClick={onRegister}>Criar conta <span aria-hidden="true">→</span></button>
						<button type="button" className="landing-button landing-button--outline landing-button--large" onClick={onLogin}>Entrar</button>
					</div>
					<div className="landing-resource-line" aria-label="Recursos da plataforma">
						<span>Carteiras</span><i />
						<span>Cotações</span><i />
						<span>Indicadores</span><i />
						<span>Corretoras</span>
					</div>
				</ScrollReveal>
				<ScrollReveal className="landing-hero__visual" direction="right" delay={120}>
					<div className="hero-visual-caption"><span><i /> VISÃO DO PRODUTO</span><small>Dados ilustrativos</small></div>
					<DashboardMockup />
					<div className="hero-floating-note hero-floating-note--top"><span>↗</span><div><small>Informação centralizada</small><strong>Leitura mais clara</strong></div></div>
					<div className="hero-floating-note hero-floating-note--bottom"><span>✓</span><div><small>Carteira acompanhada</small><strong>Posições organizadas</strong></div></div>
				</ScrollReveal>
			</div>
			<a className="hero-scroll-cue" href="#sobre"><span>Explore</span><i aria-hidden="true">↓</i></a>
		</section>
	);
}
