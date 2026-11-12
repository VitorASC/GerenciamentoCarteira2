import AboutSection from "./AboutSection";
import FeatureSection from "./FeatureSection";
import FinalCta from "./FinalCta";
import HeroSection from "./HeroSection";
import LandingFooter from "./LandingFooter";
import LandingHeader from "./LandingHeader";
import { ActionsMockup, BrokersMockup, WalletMockup } from "./ProductMockups";
import SecuritySection from "./SecuritySection";

export default function LandingPage({ onLogin, onRegister, onToggleTheme }) {
	return (
		<div className="landing-page">
			<LandingHeader onLogin={onLogin} onRegister={onRegister} onToggleTheme={onToggleTheme} />
			<main>
				<HeroSection onLogin={onLogin} onRegister={onRegister} />
				<AboutSection />
				<FeatureSection
					id="carteiras"
					eyebrow="Carteiras"
					title="Sua carteira, sem perder nada de vista."
					description="Acompanhe o que está disponível, investido e distribuído em cada carteira. Operações e indicadores ficam próximos para você entender cada posição com clareza."
					points={["Saldo, patrimônio e resultado", "Compras, vendas e preço médio", "Posições e distribuição por ativo"]}
					visual={<WalletMockup />}
				/>
				<FeatureSection
					id="acoes"
					eyebrow="Ações e cotações"
					title="Cotações e histórico em um só lugar."
					description="Consulte ativos dos mercados brasileiro e americano, acompanhe a cotação atual e visualize a evolução dos registros com uma leitura simples."
					points={["Ativos do Brasil e dos Estados Unidos", "Cotação atual e data de atualização", "Histórico conectado aos provedores de mercado"]}
					visual={<ActionsMockup />}
					reversed
				/>
				<FeatureSection
					id="corretoras"
					eyebrow="Corretoras"
					title="Instituições organizadas e verificadas."
					description="Cadastre as corretoras relacionadas aos seus investimentos e concentre os principais dados cadastrais em uma consulta rápida."
					points={["Consulta por CNPJ", "Endereço e situação cadastral", "Indicação de validação pela CVM"]}
					visual={<BrokersMockup />}
				/>
				<SecuritySection />
				<FinalCta onLogin={onLogin} onRegister={onRegister} />
			</main>
			<LandingFooter />
		</div>
	);
}
