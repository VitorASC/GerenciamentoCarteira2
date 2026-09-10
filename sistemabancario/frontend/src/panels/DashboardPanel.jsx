import { useCallback, useEffect, useMemo, useState } from "react";
import {
	ArcElement,
	Chart as ChartJS,
	Legend,
	Tooltip,
} from "chart.js";
import { Doughnut } from "react-chartjs-2";
import { useAuth } from "../hooks/useAuth";
import { api } from "../services/api";
import { formatMoney, formatNumber, formatPercent, valueTone } from "../services/format";

const CHART_COLORS = ["#2ec98f", "#56a9a0", "#8ccf83", "#60a5d8", "#b2c56c", "#7e8fd0"];

const centerTextPlugin = {
	id: "dashboardCenterText",
	beforeDraw(chart, _args, options) {
		if (!options?.value) return;
		const { ctx, chartArea } = chart;
		if (!chartArea) return;
		const styles = getComputedStyle(document.documentElement);
		const textColor = styles.getPropertyValue("--text").trim() || "#eef7f3";
		const mutedColor = styles.getPropertyValue("--text-muted").trim() || "#91a39b";
		const x = (chartArea.left + chartArea.right) / 2;
		const y = (chartArea.top + chartArea.bottom) / 2;
		ctx.save();
		ctx.textAlign = "center";
		ctx.textBaseline = "middle";
		ctx.fillStyle = mutedColor;
		ctx.font = "500 12px system-ui";
		ctx.fillText("Patrimônio", x, y - 10);
		ctx.fillStyle = textColor;
		ctx.font = "700 15px system-ui";
		ctx.fillText(options.value, x, y + 13);
		ctx.restore();
	},
};

ChartJS.register(ArcElement, Tooltip, Legend, centerTextPlugin);

export default function DashboardPanel({ onNavigate }) {
	const { usuarioId } = useAuth();
	const [carteiras, setCarteiras] = useState([]);
	const [selectedId, setSelectedId] = useState("");
	const [carteira, setCarteira] = useState(null);
	const [indicadores, setIndicadores] = useState(null);
	const [loadingCarteiras, setLoadingCarteiras] = useState(true);
	const [loadingDashboard, setLoadingDashboard] = useState(false);
	const [error, setError] = useState("");
	const [refreshKey, setRefreshKey] = useState(0);

	const loadCarteiras = useCallback(async () => {
		if (!usuarioId) return;
		setLoadingCarteiras(true);
		setError("");
		try {
			const list = await api("GET", "/carteiras/usuario/" + encodeURIComponent(usuarioId));
			const next = Array.isArray(list) ? list : [];
			setCarteiras(next);
			setSelectedId((current) => {
				if (next.some((item) => String(item.id) === String(current))) return current;
				return next[0]?.id != null ? String(next[0].id) : "";
			});
			if (next.length === 0) {
				setCarteira(null);
				setIndicadores(null);
			}
		} catch (err) {
			setError(err.message || "Não foi possível carregar suas carteiras.");
			setCarteiras([]);
			setSelectedId("");
		} finally {
			setLoadingCarteiras(false);
		}
	}, [usuarioId]);

	useEffect(() => {
		loadCarteiras();
	}, [loadCarteiras, refreshKey]);

	useEffect(() => {
		if (!selectedId) return;
		let active = true;
		setLoadingDashboard(true);
		setError("");
		Promise.all([
			api("GET", "/carteiras/" + encodeURIComponent(selectedId)),
			api("GET", "/carteiras/" + encodeURIComponent(selectedId) + "/indicadores/media-carteira"),
		])
			.then(([detail, metrics]) => {
				if (!active) return;
				setCarteira(detail);
				setIndicadores(metrics);
			})
			.catch((err) => {
				if (!active) return;
				setError(err.message || "Não foi possível carregar os dados da carteira.");
				setCarteira(null);
				setIndicadores(null);
			})
			.finally(() => {
				if (active) setLoadingDashboard(false);
			});
		return () => {
			active = false;
		};
	}, [selectedId, refreshKey]);

	const positions = useMemo(() => carteira?.posicoes || [], [carteira]);
	const distribution = useMemo(() => {
		const positive = positions
			.map((position) => ({ ...position, marketValue: Number(position.valorMercadoAtual) }))
			.filter((position) => Number.isFinite(position.marketValue) && position.marketValue > 0);
		const total = positive.reduce((sum, position) => sum + position.marketValue, 0);
		return positive.map((position, index) => ({
			...position,
			percentage: total > 0 ? (position.marketValue / total) * 100 : 0,
			color: CHART_COLORS[index % CHART_COLORS.length],
		}));
	}, [positions]);

	if (loadingCarteiras) return <DashboardLoading />;

	if (error && !selectedId) {
		return <DashboardError message={error} onRetry={() => setRefreshKey((key) => key + 1)} />;
	}

	if (carteiras.length === 0) {
		return (
		<section className="dashboard-empty card" aria-labelledby="empty-wallet-title">
			<span className="empty-icon" aria-hidden="true"><WalletIcon /></span>
			<h2 id="empty-wallet-title">Você ainda não possui uma carteira.</h2>
			<p>Crie sua primeira carteira para começar a acompanhar seus investimentos.</p>
			<button type="button" onClick={() => onNavigate("carteiras")}>Criar carteira</button>
		</section>
	);
	}

	return (
		<div className="dashboard">
			<section className="dashboard-heading" aria-labelledby="dashboard-overview-title">
				<div>
					<p className="dashboard-eyebrow">Resumo financeiro</p>
					<h2 id="dashboard-overview-title">Visão geral</h2>
					<p>Acompanhe os principais números da sua carteira.</p>
				</div>
				<label className="wallet-selector">
					<span>Carteira selecionada</span>
					<select value={selectedId} onChange={(event) => setSelectedId(event.target.value)}>
						{carteiras.map((item) => (
							<option key={item.id} value={item.id}>{item.id} — {item.nomeDaCarteira}</option>
						))}
					</select>
				</label>
			</section>

			{loadingDashboard ? (
				<DashboardLoading compact />
			) : error ? (
				<DashboardError message={error} onRetry={() => setRefreshKey((key) => key + 1)} />
			) : (
				<>
					<KpiGrid carteira={carteira} indicadores={indicadores} />
					<section className="dashboard-insights">
						<DistributionCard distribution={distribution} patrimonio={indicadores?.valorMercadoTotalCarteira} />
						<PortfolioSummary indicadores={indicadores} carteira={carteira} />
					</section>
					<PositionsTable positions={positions} indicadores={indicadores} />
				</>
			)}
		</div>
	);
}

function KpiGrid({ carteira, indicadores }) {
	const cards = [
		{ label: "Saldo disponível", value: formatMoney(carteira?.saldoTotal), note: "Disponível para investir" },
		{ label: "Total investido", value: formatMoney(indicadores?.custoTotalCarteira), note: "Custo atual da carteira" },
		{ label: "Patrimônio atual", value: formatMoney(indicadores?.valorMercadoTotalCarteira), note: "Valor de mercado" },
		{ label: "Rentabilidade atual", value: formatPercent(indicadores?.rentabilidadeNaoRealizadaPercentual), tone: valueTone(indicadores?.rentabilidadeNaoRealizadaPercentual), note: "Resultado não realizado" },
		{ label: "Resultado realizado", value: formatMoney(carteira?.lucroPrejuizoRealizado), tone: valueTone(carteira?.lucroPrejuizoRealizado), note: "Lucro ou prejuízo consolidado" },
	];
	return (
		<section className="kpi-grid" aria-label="Indicadores da carteira">
			{cards.map((card) => (
				<article className="kpi-card" key={card.label}>
					<span>{card.label}</span>
					<strong className={card.tone ? "value--" + card.tone : ""}>{card.value}</strong>
					<small>{card.note}</small>
				</article>
			))}
		</section>
	);
}

function DistributionCard({ distribution, patrimonio }) {
	const data = {
		labels: distribution.map((item) => item.ticker),
		datasets: [{
			data: distribution.map((item) => item.marketValue),
			backgroundColor: distribution.map((item) => item.color),
			borderColor: "transparent",
			borderWidth: 0,
			hoverOffset: 4,
		}],
	};
	const options = {
		responsive: true,
		maintainAspectRatio: false,
		cutout: "72%",
		plugins: {
			legend: { display: false },
			dashboardCenterText: { value: formatMoney(patrimonio) },
			tooltip: {
				callbacks: {
					label(context) {
						const item = distribution[context.dataIndex];
						return ` ${item.ticker}: ${formatMoney(item.marketValue)} (${formatPercent(item.percentage).replace("+", "")})`;
					},
				},
			},
		},
	};

	return (
		<article className="dashboard-card distribution-card">
			<header className="dashboard-card-head"><div><h3>Distribuição da carteira</h3><p>Participação por valor de mercado</p></div></header>
			{distribution.length === 0 ? <PositionsEmpty /> : (
				<div className="distribution-content">
					<div className="donut-wrap"><Doughnut data={data} options={options} /></div>
					<ul className="distribution-legend">
						{distribution.map((item) => (
							<li key={item.acaoId ?? item.ticker}>
								<span className="legend-dot" style={{ backgroundColor: item.color }} />
								<strong>{item.ticker}</strong>
								<span>{formatPercent(item.percentage).replace("+", "")}</span>
							</li>
						))}
					</ul>
				</div>
			)}
		</article>
	);
}

function PortfolioSummary({ indicadores, carteira }) {
	return (
		<article className="dashboard-card summary-card">
			<header className="dashboard-card-head"><div><h3>Resumo da carteira</h3><p>Informações complementares</p></div><span className="wallet-id">ID {carteira?.id}</span></header>
			<dl className="summary-list">
				<div><dt>Preço médio da carteira</dt><dd>{formatMoney(indicadores?.mediaPrecoMedioPonderado)}</dd></div>
				<div><dt>Valor médio por título</dt><dd>{formatMoney(indicadores?.mediaValorMercadoPorTitulo)}</dd></div>
				<div><dt>Quantidade de posições</dt><dd>{formatNumber(indicadores?.quantidadePosicoes)}</dd></div>
				<div><dt>Total de títulos</dt><dd>{formatNumber(indicadores?.quantidadeTotalTitulos)}</dd></div>
			</dl>
		</article>
	);
}

function PositionsTable({ positions, indicadores }) {
	return (
		<section className="dashboard-card positions-card">
			<header className="dashboard-card-head">
				<div><h3>Posições da carteira</h3><p>{formatNumber(indicadores?.quantidadePosicoes)} posições · {formatNumber(indicadores?.quantidadeTotalTitulos)} títulos</p></div>
			</header>
			{positions.length === 0 ? <PositionsEmpty /> : (
				<div className="dashboard-table-wrap">
					<table className="dashboard-table">
						<thead><tr><th>ID</th><th>Ativo</th><th>Quantidade</th><th>Preço médio</th><th>Cotação atual</th><th>Valor atual</th></tr></thead>
						<tbody>
							{positions.map((position) => (
								<tr key={position.acaoId ?? position.ticker}>
									<td><span className="table-id">#{position.acaoId}</span></td>
									<td><strong>{position.ticker}</strong>{position.nomeEmpresa ? <small>{position.nomeEmpresa}</small> : null}</td>
									<td>{formatNumber(position.quantidade)}</td>
									<td>{formatMoney(position.precoMedioPonderado)}</td>
									<td>{formatMoney(position.cotacaoAtual)}</td>
									<td><strong>{formatMoney(position.valorMercadoAtual)}</strong></td>
								</tr>
							))}
						</tbody>
					</table>
				</div>
			)}
		</section>
	);
}

function PositionsEmpty() {
	return <div className="positions-empty"><strong>Sua carteira ainda não possui ações.</strong><span>Registre uma compra para começar a acompanhar sua distribuição.</span></div>;
}

function DashboardLoading({ compact = false }) {
	return <div className={"dashboard-loading" + (compact ? " dashboard-loading--compact" : "")} aria-label="Carregando dados do Dashboard"><div className="loading-line loading-line--title" /><div className="loading-kpis">{Array.from({ length: 5 }, (_, index) => <div className="loading-card" key={index} />)}</div><div className="loading-panel" /></div>;
}

function DashboardError({ message, onRetry }) {
	return <section className="dashboard-error card" role="alert"><strong>Não foi possível carregar o Dashboard.</strong><p>{message}</p><button type="button" onClick={onRetry}>Tentar novamente</button></section>;
}

function WalletIcon() {
	return <svg viewBox="0 0 24 24"><path d="M4 7.5h14a2 2 0 0 1 2 2v9H5a2 2 0 0 1-2-2v-11a2 2 0 0 1 2-2h11" /><path d="M20 12h-5a2 2 0 0 0 0 4h5" /></svg>;
}
