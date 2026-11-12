import { useEffect, useState } from "react";
import BrandMark from "../components/BrandMark";
import ThemeToggle from "../components/ThemeToggle";

const NAV_ITEMS = [
	{ id: "inicio", label: "Início" },
	{ id: "sobre", label: "Sobre" },
	{ id: "carteiras", label: "Carteiras" },
	{ id: "acoes", label: "Ações" },
	{ id: "corretoras", label: "Corretoras" },
];

export default function LandingHeader({ onLogin, onRegister, onToggleTheme }) {
	const [activeSection, setActiveSection] = useState("inicio");
	const [menuOpen, setMenuOpen] = useState(false);
	const [scrolled, setScrolled] = useState(false);

	useEffect(() => {
		function handleScroll() {
			setScrolled(window.scrollY > 24);
		}
		handleScroll();
		window.addEventListener("scroll", handleScroll, { passive: true });
		return () => window.removeEventListener("scroll", handleScroll);
	}, []);

	useEffect(() => {
		const sections = NAV_ITEMS.map((item) => document.getElementById(item.id)).filter(Boolean);
		if (!("IntersectionObserver" in window) || sections.length === 0) return undefined;

		const observer = new IntersectionObserver(
			(entries) => {
				const visible = entries
					.filter((entry) => entry.isIntersecting)
					.sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
				if (visible?.target?.id) setActiveSection(visible.target.id);
			},
			{ rootMargin: "-22% 0px -62%", threshold: [0, 0.1, 0.4] }
		);

		sections.forEach((section) => observer.observe(section));
		return () => observer.disconnect();
	}, []);

	function closeMenu() {
		setMenuOpen(false);
	}

	return (
		<header className={`landing-header${scrolled ? " landing-header--scrolled" : ""}`}>
			<div className="landing-header__inner">
				<a className="landing-header__brand" href="#inicio" onClick={closeMenu} aria-label="GestãoAtiva — início">
					<BrandMark className="landing-brand" />
				</a>

				<button
					type="button"
					className="landing-menu-toggle"
					aria-label={menuOpen ? "Fechar menu" : "Abrir menu"}
					aria-expanded={menuOpen}
					aria-controls="landing-navigation"
					onClick={() => setMenuOpen((current) => !current)}
				>
					<span />
					<span />
				</button>

				<div id="landing-navigation" className={`landing-header__navigation${menuOpen ? " is-open" : ""}`}>
					<nav className="landing-nav" aria-label="Navegação da página pública">
						{NAV_ITEMS.map((item) => (
							<a
								key={item.id}
								href={`#${item.id}`}
								className={activeSection === item.id ? "is-active" : ""}
								aria-current={activeSection === item.id ? "location" : undefined}
								onClick={closeMenu}
							>
								{item.label}
							</a>
						))}
					</nav>
					<div className="landing-header__actions">
						<ThemeToggle onToggle={onToggleTheme} />
						<button type="button" className="landing-button landing-button--outline" onClick={onLogin}>Entrar</button>
						<button type="button" className="landing-button landing-button--primary" onClick={onRegister}>Criar conta</button>
					</div>
				</div>
			</div>
		</header>
	);
}
