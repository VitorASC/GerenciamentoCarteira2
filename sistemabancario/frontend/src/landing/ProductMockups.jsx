export function DashboardMockup() {
	return (
		<div className="product-window product-window--dashboard" role="img" aria-label="Demonstração ilustrativa do Dashboard do GestãoAtiva">
			<MockupBar label="Dashboard — dados ilustrativos" />
			<div className="dashboard-demo">
				<aside className="dashboard-demo__sidebar" aria-hidden="true">
					<span className="dashboard-demo__logo"><TrendIcon /></span>
					<span className="demo-nav-icon is-active" />
					<span className="demo-nav-icon" />
					<span className="demo-nav-icon" />
					<span className="demo-nav-icon" />
				</aside>
				<div className="dashboard-demo__content">
					<div className="demo-heading"><div><small>RESUMO FINANCEIRO</small><strong>Visão geral</strong></div><span>Carteira Principal⌄</span></div>
					<div className="demo-kpis">
						<MiniKpi label="Saldo disponível" value="R$ 8.420" />
						<MiniKpi label="Patrimônio atual" value="R$ 42.780" />
						<MiniKpi label="Rentabilidade" value="+ 8,7%" positive />
					</div>
					<div className="demo-dashboard-grid">
						<div className="demo-chart-card">
							<div className="demo-card-title"><span>Evolução da carteira</span><small>Últimos meses</small></div>
							<AreaChart />
						</div>
						<div className="demo-allocation-card">
							<div className="demo-card-title"><span>Distribuição</span><small>Por ativo</small></div>
							<div className="demo-donut"><span>100%</span></div>
							<div className="demo-legend"><i /> GATV3 <b>48%</b></div>
							<div className="demo-legend"><i /> NOVA11 <b>32%</b></div>
						</div>
					</div>
					<div className="demo-position-row"><strong>GATV3</strong><span>120 ações</span><span>R$ 18.740</span><b>+6,4%</b></div>
				</div>
			</div>
		</div>
	);
}

export function WalletMockup() {
	return (
		<div className="feature-mockup wallet-mockup" role="img" aria-label="Demonstração ilustrativa da gestão de carteiras">
			<MockupBar label="Carteiras — dados ilustrativos" compact />
			<div className="mockup-body">
				<div className="mockup-context"><div><small>CARTEIRA SELECIONADA</small><strong>Carteira Principal</strong></div><span className="mockup-tag">ID 01</span></div>
				<div className="wallet-stats">
					<MiniKpi label="Saldo" value="R$ 8.420" />
					<MiniKpi label="Patrimônio" value="R$ 42.780" />
					<MiniKpi label="Resultado" value="+ R$ 3.410" positive />
				</div>
				<div className="wallet-detail-grid">
					<div className="wallet-allocation"><div className="wallet-ring"><span>3<br /><small>ativos</small></span></div><div><p><i /> GATV3 <b>48%</b></p><p><i /> NOVA11 <b>32%</b></p><p><i /> VERD4 <b>20%</b></p></div></div>
					<div className="wallet-position-list"><small>POSIÇÕES</small><p><strong>GATV3</strong><span>R$ 18.740</span></p><p><strong>NOVA11</strong><span>R$ 12.510</span></p><p><strong>VERD4</strong><span>R$ 7.110</span></p></div>
				</div>
			</div>
		</div>
	);
}

export function ActionsMockup() {
	return (
		<div className="feature-mockup action-mockup" role="img" aria-label="Demonstração ilustrativa da consulta de ações e histórico">
			<MockupBar label="Ações — dados ilustrativos" compact />
			<div className="mockup-body">
				<div className="action-quote-head"><div><span className="ticker-tile">GA</span><div><small>ATIVO SELECIONADO</small><strong>GATV3</strong><p>Brasil · B3</p></div></div><div><small>COTAÇÃO ATUAL</small><strong>R$ 31,24</strong><span>+2,18%</span></div></div>
				<div className="action-chart-head"><div><strong>Histórico de cotação</strong><small>Últimos 30 dias</small></div><div className="chart-range"><span>7D</span><span className="is-active">30D</span><span>1A</span></div></div>
				<div className="action-chart"><StockChart /><span className="chart-marker">R$ 31,24</span></div>
				<div className="action-market-row"><span><small>Mercado</small><strong>Brasil</strong></span><span><small>Última atualização</small><strong>Hoje, 10:32</strong></span><span><small>Pontos no histórico</small><strong>24</strong></span></div>
			</div>
		</div>
	);
}

export function BrokersMockup() {
	return (
		<div className="feature-mockup broker-mockup" role="img" aria-label="Demonstração ilustrativa do cadastro e validação de corretoras">
			<MockupBar label="Corretoras — dados ilustrativos" compact />
			<div className="mockup-body">
				<div className="mockup-context"><div><small>INSTITUIÇÕES</small><strong>Corretoras cadastradas</strong></div><span className="mockup-count">3 corretoras</span></div>
				<div className="broker-table-head"><span>Instituição</span><span>Localização</span><span>Status CVM</span></div>
				<BrokerRow initials="AV" name="Ativa Valores" location="São Paulo / SP" />
				<BrokerRow initials="NV" name="Nova Invest" location="Curitiba / PR" />
				<BrokerRow initials="VR" name="Verde Capital" location="Recife / PE" />
				<div className="broker-detail"><div><small>CNPJ</small><strong>00.000.000/0001-00</strong></div><div><small>Endereço</small><strong>Dados consultados e organizados</strong></div><span><CheckIcon /> Validada pela CVM</span></div>
			</div>
		</div>
	);
}

function MockupBar({ label, compact = false }) {
	return <div className={`mockup-bar${compact ? " mockup-bar--compact" : ""}`}><div><i /><i /><i /></div><span>{label}</span><b>GestãoAtiva</b></div>;
}

function MiniKpi({ label, value, positive = false }) {
	return <div className="mini-kpi"><small>{label}</small><strong className={positive ? "is-positive" : ""}>{value}</strong><i /></div>;
}

function BrokerRow({ initials, name, location }) {
	return <div className="broker-row"><span className="broker-avatar">{initials}</span><strong>{name}</strong><span>{location}</span><b><CheckIcon /> Validada</b></div>;
}

function TrendIcon() {
	return <svg viewBox="0 0 24 24"><path d="M4 17l5-5 4 3 7-8" /><path d="M15 7h5v5" /></svg>;
}

function CheckIcon() {
	return <svg viewBox="0 0 20 20" aria-hidden="true"><path d="m5 10 3 3 7-7" /></svg>;
}

function AreaChart() {
	return (
		<svg className="demo-area-chart" viewBox="0 0 520 180" preserveAspectRatio="none" aria-hidden="true">
			<defs><linearGradient id="dashboard-chart-fill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#35c891" stopOpacity=".35" /><stop offset="1" stopColor="#35c891" stopOpacity="0" /></linearGradient></defs>
			<path className="chart-grid-line" d="M0 28H520M0 76H520M0 124H520M0 172H520" />
			<path fill="url(#dashboard-chart-fill)" d="M0 150C45 142 57 122 92 130S148 110 184 118s55-35 91-25 63-20 96-14 53-43 83-30 46-19 66-32V180H0Z" />
			<path className="chart-line" d="M0 150C45 142 57 122 92 130S148 110 184 118s55-35 91-25 63-20 96-14 53-43 83-30 46-19 66-32" />
		</svg>
	);
}

function StockChart() {
	return (
		<svg viewBox="0 0 620 210" preserveAspectRatio="none" aria-hidden="true">
			<defs><linearGradient id="stock-chart-fill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#31bf89" stopOpacity=".28" /><stop offset="1" stopColor="#31bf89" stopOpacity="0" /></linearGradient></defs>
			<path className="chart-grid-line" d="M0 30H620M0 86H620M0 142H620M0 198H620" />
			<path fill="url(#stock-chart-fill)" d="M0 165C35 155 56 171 90 148s60 14 93-25 64-4 92-36 55 13 92-20 60 4 90-25 53 11 82-17 51 2 81-17V210H0Z" />
			<path className="chart-line" d="M0 165C35 155 56 171 90 148s60 14 93-25 64-4 92-36 55 13 92-20 60 4 90-25 53 11 82-17 51 2 81-17" />
			<circle cx="620" cy="8" r="5" />
		</svg>
	);
}
