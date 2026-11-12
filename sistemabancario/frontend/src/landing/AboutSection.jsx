import ScrollReveal from "./ScrollReveal";

const BENEFITS = [
	{ number: "01", title: "Gestão centralizada", text: "Carteiras, operações e posições reunidas em uma única experiência." },
	{ number: "02", title: "Cotações integradas", text: "Valores atuais e históricos para acompanhar os ativos cadastrados." },
	{ number: "03", title: "Indicadores financeiros", text: "Preço médio, patrimônio e resultados organizados para uma leitura direta." },
	{ number: "04", title: "Controle de acesso", text: "Áreas e ações adequadas aos perfis Padrão e Administrador." },
];

export default function AboutSection() {
	return (
		<section id="sobre" className="landing-section about-section">
			<div className="landing-container">
				<ScrollReveal className="about-section__intro">
					<div><p className="landing-eyebrow">Sobre o GestãoAtiva</p><h2>Uma visão mais simples para uma rotina financeira mais organizada.</h2></div>
					<p>O GestãoAtiva reúne as principais informações da sua carteira em uma interface única, permitindo acompanhar operações, ativos, corretoras e indicadores sem perder o contexto.</p>
				</ScrollReveal>
				<div className="about-benefits">
					{BENEFITS.map((benefit, index) => (
						<ScrollReveal as="article" className="about-benefit" delay={index * 80} key={benefit.number}>
							<span>{benefit.number}</span>
							<h3>{benefit.title}</h3>
							<p>{benefit.text}</p>
						</ScrollReveal>
					))}
				</div>
			</div>
		</section>
	);
}
