import BrandMark from "../components/BrandMark";

export default function LandingFooter() {
	return (
		<footer className="landing-footer">
			<div className="landing-container landing-footer__inner">
				<a href="#inicio" aria-label="GestãoAtiva — voltar ao início"><BrandMark className="landing-brand" /></a>
				<p>Projeto acadêmico · {new Date().getFullYear()}</p>
				<nav aria-label="Navegação do rodapé"><a href="#sobre">Sobre</a><a href="#carteiras">Carteiras</a><a href="#acoes">Ações</a><a href="#corretoras">Corretoras</a></nav>
			</div>
		</footer>
	);
}
