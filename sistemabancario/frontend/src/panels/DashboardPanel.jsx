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
		ctx.font = "600 11px system-ui";
		ctx.fillText("Patrimônio", x, y - 11);
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

	const selectedWallet = useMemo(
		() => carteiras.find((item) => String(item.id) === String(selectedId)) || null,
		[carteiras, selectedId]
	);
	const positions = useMemo(() => mergePositionData(carteira?.posicoes, indicadores?.posicoes), [carteira, indicadores]);
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
				<p className="dashboard-empty-eyebrow">Primeiro passo</p>
				<h2 id="empty-wallet-title">Você ainda não possui uma carteira.</h2>
				<p>Crie uma carteira para organizar seus investimentos e acompanhar os indicadores disponíveis.</p>
				<button type="button" onClick={() => onNavigate("carteiras")}>Criar carteira</button>
			</section>
		);
	}

	return (
		<div className="dashboard">
			<section className="dashboard-heading" aria-labelledby="dashboard-overview-title">
				<div className="dashboard-heading-copy">
					<p className="dashboard-eyebrow">Painel executivo</p>
					<h2 id="dashboard-overview-title">Visão geral dos seus investimentos</h2>
					<p>Acompanhe patrimônio, resultados e composição da carteira selecionada.</p>
				</div>
				<div className="dashboard-wallet-context">
					<div className="dashboard-wallet-label">
						<span>Carteira selecionada</span>
						{selectedWallet?.id != null ? <small>ID {selectedWallet.id}</small> : null}
					</div>
					<label className="wallet-selector">
						<span className="dashboard-visually-hidden">Selecionar carteira</span>
						<select
							value={selectedId}
							onChange={(event) => setSelectedId(event.target.value)}
							aria-label="Carteira selecionada"
						>
							{carteiras.map((item) => (
								<option key={item.id} value={item.id}>{item.nomeDaCarteira} · ID {item.id}</option>
							))}
						</select>
					</label>
				</div>
			</section>

			{loadingDashboard ? (
				<DashboardLoading compact />
			) : error ? (
				<DashboardError message={error} onRetry={() => setRefreshKey((key) => key + 1)} />
			) : (
				<>
					<PortfolioOverview carteira={carteira} indicadores={indicadores} />
					{positions.length === 0 ? (
						<DashboardEmptyPositions onNavigate={() => onNavigate("carteiras")} />
					) : (
						<>
							<section className="dashboard-insights">
								<DistributionCard distribution={distribution} patrimonio={indicadores?.valorMercadoTotalCarteira} />
								<PortfolioComposition indicadores={indicadores} />
							</section>
							<PositionsTable positions={positions} indicadores={indicadores} />
						</>
					)}
				</>
			)}
		</div>
	);
}

function mergePositionData(detailPositions, metricPositions) {
	const details = Array.isArray(detailPositions) ? detailPositions : [];
	if (!Array.isArray(metricPositions) || metricPositions.length === 0) return details;
	const metricsByTicker = new Map(metricPositions.map((position) => [position.ticker, position]));
	return details.map((position) => {
		const metrics = metricsByTicker.get(position.ticker);
		if (!metrics) return position;
		return {
			...position,
			quantidade: metrics.quantidade,
			precoMedioPonderado: metrics.precoMedioPonderado,
			cotacaoAtual: metrics.cotacaoMercadoAtual,
			valorCustoTotal: metrics.valorCusto,
			valorMercadoAtual: metrics.valorMercado,
		};
	});
}

function PortfolioOverview({ carteira, indicadores }) {
	const marketValue = Number(indicadores?.valorMercadoTotalCarteira);
	const investedValue = Number(indicadores?.custoTotalCarteira);
	const currentResult = Number.isFinite(marketValue) && Number.isFinite(investedValue)
		? marketValue - investedValue
		: null;
	const currentTone = valueTone(currentResult);
	const realizedTone = valueTone(carteira?.lucroPrejuizoRealizado);

	return (
		<section className="dashboard-summary-grid" aria-label="Resumo financeiro da carteira">
			<article className="portfolio-hero">
				<header>
					<div>
						<span>Valor de mercado</span>
						<small>Valor atual das posições</small>
					</div>
					<span className="portfolio-hero-status">Posição atual</span>
				</header>
				<strong className="portfolio-hero-value">{formatMoney(indicadores?.valorMercadoTotalCarteira)}</strong>
				<div className="portfolio-current-result">
					<div>
						<span>Resultado atual</span>
						<small>Não realizado</small>
					</div>
					<div className={`portfolio-current-result-value value--${currentTone}`}>
						<strong>{formatMoney(currentResult)}</strong>
						<span>{formatPercent(indicadores?.rentabilidadeNaoRealizadaPercentual)}</span>
					</div>
				</div>
			</article>

			<article className="dashboard-secondary-kpis" aria-label="Indicadores complementares">
				<FinancialMetric label="Saldo disponível" value={formatMoney(carteira?.saldoTotal)} note="Disponível para investir" />
				<FinancialMetric label="Custo das posições" value={formatMoney(indicadores?.custoTotalCarteira)} note="Custo atual das posições" />
				<FinancialMetric
					label="Resultado realizado"
					value={formatMoney(carteira?.lucroPrejuizoRealizado)}
					note="Lucro ou prejuízo já consolidado"
					tone={realizedTone}
				/>
			</article>
		</section>
	);
}

function FinancialMetric({ label, value, note, tone }) {
	return (
		<div className="dashboard-metric-row">
			<div><span>{label}</span><small>{note}</small></div>
			<strong className={tone ? `value--${tone}` : ""}>{value}</strong>
		</div>
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
		cutout: "73%",
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
		<article className="dashboard-card dashboard-allocation-card">
			<header className="dashboard-card-head">
				<div><span className="dashboard-section-index">01</span><h3>Distribuição da carteira</h3><p>Participação de cada ativo no valor de mercado</p></div>
			</header>
			<div className="distribution-content">
				<div className="donut-wrap"><Doughnut data={data} options={options} /></div>
				<ul className="distribution-legend" aria-label="Participação dos ativos">
					{distribution.map((item) => (
						<li key={item.acaoId ?? item.ticker}>
							<span className="legend-dot" style={{ backgroundColor: item.color }} />
							<span className="legend-asset"><strong>{item.ticker}</strong><small>{formatMoney(item.marketValue)}</small></span>
							<span className="legend-share">{formatPercent(item.percentage).replace("+", "")}</span>
						</li>
					))}
				</ul>
			</div>
		</article>
	);
}

function PortfolioComposition({ indicadores }) {
	return (
		<article className="dashboard-card dashboard-composition-card">
			<header className="dashboard-card-head">
				<div><span className="dashboard-section-index">02</span><h3>Composição atual</h3><p>Quantidades e médias retornadas pela carteira</p></div>
			</header>
			<dl className="dashboard-composition-list">
				<div><dt>Posições</dt><dd>{formatNumber(indicadores?.quantidadePosicoes)}</dd></div>
				<div><dt>Total de ações</dt><dd>{formatNumber(indicadores?.quantidadeTotalTitulos)}</dd></div>
				<div><dt>Preço médio da carteira</dt><dd>{formatMoney(indicadores?.mediaPrecoMedioPonderado)}</dd></div>
				<div><dt>Valor médio por ação</dt><dd>{formatMoney(indicadores?.mediaValorMercadoPorTitulo)}</dd></div>
			</dl>
		</article>
	);
}

function PositionsTable({ positions, indicadores }) {
	return (
		<section className="dashboard-card dashboard-positions-card">
			<header className="dashboard-card-head dashboard-positions-head">
				<div><span className="dashboard-section-index">03</span><h3>Posições da carteira</h3><p>{formatNumber(indicadores?.quantidadePosicoes)} {Number(indicadores?.quantidadePosicoes) === 1 ? "posição" : "posições"} · {formatNumber(indicadores?.quantidadeTotalTitulos)} {Number(indicadores?.quantidadeTotalTitulos) === 1 ? "ação" : "ações"}</p></div>
				<span className="dashboard-table-caption">Valores atuais</span>
			</header>
			<div className="dashboard-table-wrap">
				<table className="dashboard-table dashboard-positions-table">
					<thead><tr><th>Ativo</th><th>Quantidade</th><th>Preço médio</th><th>Cotação atual</th><th>Custo</th><th>Valor atual</th><th>Resultado atual</th></tr></thead>
					<tbody>
						{positions.map((position) => {
							const marketValue = Number(position.valorMercadoAtual);
							const costValue = Number(position.valorCustoTotal);
							const result = Number.isFinite(marketValue) && Number.isFinite(costValue) ? marketValue - costValue : null;
							const resultPercent = Number.isFinite(result) && costValue > 0 ? (result / costValue) * 100 : null;
							const tone = valueTone(result);
							return (
								<tr key={position.acaoId ?? position.ticker}>
									<td><strong className="position-ticker">{position.ticker}</strong>{position.nomeEmpresa ? <small>{position.nomeEmpresa}</small> : null}</td>
									<td>{formatNumber(position.quantidade)}</td>
									<td>{formatMoney(position.precoMedioPonderado)}</td>
									<td>{formatMoney(position.cotacaoAtual)}</td>
									<td>{formatMoney(position.valorCustoTotal)}</td>
									<td><strong>{formatMoney(position.valorMercadoAtual)}</strong></td>
									<td><strong className={`value--${tone}`}>{formatMoney(result)}</strong>{resultPercent != null ? <small className={`position-result-percent value--${tone}`}>{formatPercent(resultPercent)}</small> : null}</td>
								</tr>
							);
						})}
					</tbody>
				</table>
			</div>
		</section>
	);
}

function DashboardEmptyPositions({ onNavigate }) {
	return (
		<section className="dashboard-card dashboard-empty-positions" aria-labelledby="empty-positions-title">
			<span className="empty-positions-mark" aria-hidden="true">0</span>
			<div>
				<p className="dashboard-empty-eyebrow">Carteira sem posições</p>
				<h3 id="empty-positions-title">Esta carteira ainda não possui ativos.</h3>
				<p>Quando a primeira compra for registrada, a distribuição e as posições aparecerão aqui.</p>
			</div>
			<button type="button" onClick={onNavigate}>Ir para Carteiras</button>
		</section>
	);
}

function DashboardLoading({ compact = false }) {
	return (
		<div className={`dashboard-loading dashboard-loading--executive${compact ? " dashboard-loading--compact" : ""}`} role="status" aria-label="Carregando dados do Dashboard">
			{compact ? null : <div className="dashboard-loading-heading"><span /><span /></div>}
			<div className="dashboard-loading-summary"><div className="loading-hero" /><div className="loading-metrics">{[0, 1, 2].map((item) => <span key={item} />)}</div></div>
			<div className="dashboard-loading-insights"><div /><div /></div>
			<span className="dashboard-visually-hidden">Carregando dados do Dashboard</span>
		</div>
	);
}

function DashboardError({ message, onRetry }) {
	return <section className="dashboard-error card" role="alert"><strong>Não foi possível carregar o Dashboard.</strong><p>{message}</p><button type="button" onClick={onRetry}>Tentar novamente</button></section>;
}

function WalletIcon() {
	return <svg viewBox="0 0 24 24"><path d="M4 7.5h14a2 2 0 0 1 2 2v9H5a2 2 0 0 1-2-2v-11a2 2 0 0 1 2-2h11" /><path d="M20 12h-5a2 2 0 0 0 0 4h5" /></svg>;
}
