import { useCallback, useEffect, useMemo, useState } from "react";
import { ArcElement, Chart as ChartJS, Legend, Tooltip } from "chart.js";
import { Doughnut } from "react-chartjs-2";
import { useAuth } from "../hooks/useAuth";
import { api } from "../services/api";
import { formatMoney, formatNumber, formatPercent, valueTone } from "../services/format";

const CHART_COLORS = ["#2ec98f", "#56a9a0", "#8ccf83", "#60a5d8", "#b2c56c", "#7e8fd0"];
const EMPTY_FORM_STATUS = { text: "", error: false };

const walletCenterTextPlugin = {
	id: "walletCenterText",
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

ChartJS.register(ArcElement, Tooltip, Legend, walletCenterTextPlugin);

export default function CarteirasPanel() {
	const { usuarioId } = useAuth();
	const [wallets, setWallets] = useState([]);
	const [selectedId, setSelectedId] = useState("");
	const [wallet, setWallet] = useState(null);
	const [metrics, setMetrics] = useState(null);
	const [loadingWallets, setLoadingWallets] = useState(true);
	const [loadingDetail, setLoadingDetail] = useState(false);
	const [loadError, setLoadError] = useState("");
	const [refreshKey, setRefreshKey] = useState(0);
	const [filters, setFilters] = useState({ id: "", name: "" });
	const [appliedName, setAppliedName] = useState("");
	const [idSearchResult, setIdSearchResult] = useState(null);
	const [searching, setSearching] = useState(false);
	const [notice, setNotice] = useState(null);
	const [deleting, setDeleting] = useState(false);
	const [dialog, setDialog] = useState(null);
	const [actions, setActions] = useState([]);
	const [brokers, setBrokers] = useState([]);
	const [referencesLoading, setReferencesLoading] = useState(false);
	const [referencesError, setReferencesError] = useState("");

	const loadWallets = useCallback(async (preferredId) => {
		if (!usuarioId) return;
		setLoadingWallets(true);
		setLoadError("");
		try {
			const response = await api("GET", "/carteiras/usuario/" + encodeURIComponent(usuarioId));
			const list = Array.isArray(response) ? response : [];
			setWallets(list);
			setSelectedId((current) => {
				const preferred = preferredId != null ? String(preferredId) : current;
				if (list.some((item) => String(item.id) === preferred)) return preferred;
				return list[0]?.id != null ? String(list[0].id) : "";
			});
			if (list.length === 0) {
				setWallet(null);
				setMetrics(null);
			}
		} catch (error) {
			setLoadError(error.message || "Não foi possível carregar suas carteiras.");
			setWallets([]);
			setSelectedId("");
		} finally {
			setLoadingWallets(false);
		}
	}, [usuarioId]);

	useEffect(() => { loadWallets(); }, [loadWallets]);

	useEffect(() => {
		if (!selectedId) return;
		let active = true;
		setLoadingDetail(true);
		setLoadError("");
		Promise.all([
			api("GET", "/carteiras/" + encodeURIComponent(selectedId)),
			api("GET", "/carteiras/" + encodeURIComponent(selectedId) + "/indicadores/media-carteira"),
		])
			.then(([detail, indicators]) => {
				if (!active) return;
				setWallet(detail);
				setMetrics(indicators);
			})
			.catch((error) => {
				if (!active) return;
				setLoadError(error.message || "Não foi possível carregar a carteira selecionada.");
				setWallet(null);
				setMetrics(null);
			})
			.finally(() => { if (active) setLoadingDetail(false); });
		return () => { active = false; };
	}, [selectedId, refreshKey]);

	const visibleWallets = useMemo(() => {
		const base = idSearchResult !== null ? idSearchResult : wallets;
		if (!appliedName) return base;
		const normalized = appliedName.toLocaleLowerCase("pt-BR");
		return base.filter((item) => item.nomeDaCarteira?.toLocaleLowerCase("pt-BR").includes(normalized));
	}, [wallets, idSearchResult, appliedName]);

	const positions = useMemo(() => wallet?.posicoes || [], [wallet]);
	const distribution = useMemo(() => {
		const valid = positions
			.map((position) => ({ ...position, marketValue: Number(position.valorMercadoAtual) }))
			.filter((position) => Number.isFinite(position.marketValue) && position.marketValue > 0);
		const total = valid.reduce((sum, position) => sum + position.marketValue, 0);
		return valid.map((position, index) => ({
			...position,
			percentage: total > 0 ? (position.marketValue / total) * 100 : 0,
			color: CHART_COLORS[index % CHART_COLORS.length],
		}));
	}, [positions]);

	async function handleSearch(event) {
		event.preventDefault();
		setNotice(null);
		setAppliedName(filters.name.trim());
		if (!filters.id.trim()) {
			setIdSearchResult(null);
			return;
		}
		setSearching(true);
		try {
			const result = await api("GET", "/carteiras/" + encodeURIComponent(filters.id.trim()));
			const nameQuery = filters.name.trim().toLocaleLowerCase("pt-BR");
			const matchesName = !nameQuery || result.nomeDaCarteira?.toLocaleLowerCase("pt-BR").includes(nameQuery);
			setIdSearchResult(matchesName ? [result] : []);
			if (matchesName) setSelectedId(String(result.id));
		} catch (error) {
			setIdSearchResult([]);
			setNotice({ type: "error", text: error.message || "Carteira não encontrada." });
		} finally {
			setSearching(false);
		}
	}

	function clearFilters() {
		setFilters({ id: "", name: "" });
		setAppliedName("");
		setIdSearchResult(null);
		setNotice(null);
	}

	async function loadReferences(kind) {
		setReferencesError("");
		if (kind === "sell") return;
		if (kind === "buy" && actions.length > 0) return;
		if ((kind === "create" || kind === "edit") && brokers.length > 0) return;
		setReferencesLoading(true);
		try {
			if (kind === "buy") {
				const response = await api("GET", "/acoes?size=100");
				setActions(response?.content || []);
			} else {
				const response = await api("GET", "/corretoras?size=100");
				setBrokers(response?.content || []);
			}
		} catch (error) {
			setReferencesError(error.message || "Não foi possível carregar as opções.");
		} finally {
			setReferencesLoading(false);
		}
	}

	function openDialog(type) {
		setNotice(null);
		setDialog(type);
		loadReferences(type);
	}

	async function refreshAfterChange(preferredId) {
		await loadWallets(preferredId);
		setRefreshKey((key) => key + 1);
	}

	async function handleDelete() {
		if (deleting || !wallet || !window.confirm(`Excluir definitivamente a carteira #${wallet.id}? O saldo deve estar zerado e não pode haver posições.`)) return;
		setDeleting(true);
		setNotice({ type: "loading", text: "Excluindo carteira..." });
		try {
			await api("DELETE", "/carteiras/" + encodeURIComponent(wallet.id));
			setNotice({ type: "success", text: `Carteira #${wallet.id} excluída com sucesso.` });
			await loadWallets("");
			setIdSearchResult(null);
		} catch (error) {
			setNotice({ type: "error", text: error.message || "Não foi possível excluir a carteira." });
		} finally {
			setDeleting(false);
		}
	}

	return (
		<div className="wallets-page">
			<section className="wallets-page-head">
				<div><p className="dashboard-eyebrow">Gestão de investimentos</p><h2>Carteiras</h2><p>Gerencie suas carteiras, posições e operações.</p></div>
				<button type="button" className="wallet-primary-action" onClick={() => openDialog("create")}>+ Nova carteira</button>
			</section>

			{notice ? <PageNotice notice={notice} onClose={() => setNotice(null)} /> : null}

			<section className="wallets-list-card dashboard-card" aria-labelledby="wallet-list-title">
				<div className="wallets-list-head"><div><h3 id="wallet-list-title">Minhas carteiras</h3><p>Selecione uma carteira para ver detalhes e operar.</p></div><span>{formatNumber(wallets.length)} {wallets.length === 1 ? "carteira" : "carteiras"}</span></div>
				<form className="wallet-filters" onSubmit={handleSearch}>
					<label><span>ID</span><input type="number" min="1" placeholder="Ex.: 1" value={filters.id} onChange={(event) => setFilters({ ...filters, id: event.target.value })} /></label>
					<label><span>Nome</span><input type="search" placeholder="Buscar pelo nome" value={filters.name} onChange={(event) => setFilters({ ...filters, name: event.target.value })} /></label>
					<button type="submit" disabled={searching}>{searching ? "Buscando..." : "Buscar"}</button>
					<button type="button" className="wallet-filter-clear" onClick={clearFilters}>Limpar</button>
				</form>

				{loadingWallets ? <WalletListLoading /> : loadError && wallets.length === 0 ? <InlineError message={loadError} onRetry={() => loadWallets()} /> : wallets.length === 0 ? <NoWallets onCreate={() => openDialog("create")} /> : visibleWallets.length === 0 ? <div className="wallet-filter-empty"><strong>Nenhuma carteira corresponde aos filtros.</strong><button type="button" onClick={clearFilters}>Limpar filtros</button></div> : (
					<div className="wallet-picker" role="listbox" aria-label="Selecione uma carteira">
						{visibleWallets.map((item) => <button key={item.id} type="button" role="option" aria-selected={String(item.id) === selectedId} className={"wallet-picker-item" + (String(item.id) === selectedId ? " wallet-picker-item--active" : "")} onClick={() => setSelectedId(String(item.id))}><span className="wallet-picker-id">#{item.id}</span><span className="wallet-picker-copy"><strong>{item.nomeDaCarteira}</strong><small>{item.corretoraId ? `Corretora #${item.corretoraId}` : "Sem corretora"}</small></span><span className="wallet-picker-balance"><small>Saldo</small><strong>{formatMoney(item.saldoTotal)}</strong></span></button>)}
					</div>
				)}
			</section>

			{wallets.length > 0 ? loadingDetail ? <WalletDetailLoading /> : loadError ? <InlineError message={loadError} onRetry={() => setRefreshKey((key) => key + 1)} /> : wallet ? <>
				<SelectedWalletHeader wallet={wallet} brokers={brokers} deleting={deleting} onBuy={() => openDialog("buy")} onSell={() => openDialog("sell")} onEdit={() => openDialog("edit")} onDelete={handleDelete} />
				<WalletKpis wallet={wallet} metrics={metrics} />
				<section className="wallet-insights"><WalletDistribution distribution={distribution} patrimonio={metrics?.valorMercadoTotalCarteira} onBuy={() => openDialog("buy")} /><WalletSecondaryMetrics metrics={metrics} /></section>
				<WalletPositions positions={positions} metrics={metrics} onBuy={() => openDialog("buy")} onSell={() => openDialog("sell")} />
			</> : null : null}

			{dialog === "create" ? <CreateWalletDialog usuarioId={usuarioId} brokers={brokers} loadingOptions={referencesLoading} optionsError={referencesError} onClose={() => setDialog(null)} onCreated={async (created) => { setDialog(null); setNotice({ type: "success", text: `Carteira #${created.id} criada com sucesso.` }); await refreshAfterChange(created.id); }} /> : null}
			{dialog === "edit" && wallet ? <EditWalletDialog wallet={wallet} brokers={brokers} loadingOptions={referencesLoading} optionsError={referencesError} onClose={() => setDialog(null)} onSaved={async (updated) => { setDialog(null); setNotice({ type: "success", text: `Carteira #${updated.id} atualizada com sucesso.` }); await refreshAfterChange(updated.id); }} /> : null}
			{dialog === "buy" && wallet ? <BuyDialog wallet={wallet} actions={actions} loadingOptions={referencesLoading} optionsError={referencesError} onClose={() => setDialog(null)} onCompleted={() => refreshAfterChange(wallet.id)} /> : null}
			{dialog === "sell" && wallet ? <SellDialog wallet={wallet} positions={positions} onClose={() => setDialog(null)} onCompleted={() => refreshAfterChange(wallet.id)} /> : null}
		</div>
	);
}

function SelectedWalletHeader({ wallet, brokers, deleting, onBuy, onSell, onEdit, onDelete }) {
	const broker = brokers.find((item) => String(item.id) === String(wallet.corretoraId));
	return <section className="selected-wallet-head dashboard-card"><div className="selected-wallet-identity"><span className="wallet-id">ID {wallet.id}</span><div><h3>{wallet.nomeDaCarteira}</h3><p>{wallet.corretoraId ? (broker?.nomeFantasia || broker?.razaoSocial || `Corretora #${wallet.corretoraId}`) : "Sem corretora associada"}</p></div></div><div className="selected-wallet-actions"><button type="button" className="wallet-action wallet-action--buy" onClick={onBuy}>Comprar</button><button type="button" className="wallet-action" onClick={onSell} disabled={!wallet.posicoes?.length}>Vender</button><button type="button" className="wallet-action wallet-action--secondary" onClick={onEdit}>Editar carteira</button><button type="button" className="wallet-action wallet-action--danger" onClick={onDelete} disabled={deleting}>{deleting ? "Excluindo..." : "Excluir"}</button></div></section>;
}

function WalletKpis({ wallet, metrics }) {
	const cards = [{ label: "Saldo disponível", value: formatMoney(wallet.saldoTotal), note: "Disponível para investir" }, { label: "Total investido", value: formatMoney(metrics?.custoTotalCarteira), note: "Custo atual da carteira" }, { label: "Patrimônio atual", value: formatMoney(metrics?.valorMercadoTotalCarteira), note: "Valor de mercado" }, { label: "Rentabilidade atual", value: formatPercent(metrics?.rentabilidadeNaoRealizadaPercentual), tone: valueTone(metrics?.rentabilidadeNaoRealizadaPercentual), note: "Resultado não realizado" }, { label: "Resultado realizado", value: formatMoney(wallet.lucroPrejuizoRealizado), tone: valueTone(wallet.lucroPrejuizoRealizado), note: "Lucro ou prejuízo acumulado" }];
	return <section className="kpi-grid wallets-kpi-grid" aria-label="Indicadores da carteira">{cards.map((card) => <article className="kpi-card" key={card.label}><span>{card.label}</span><strong className={card.tone ? `value--${card.tone}` : ""}>{card.value}</strong><small>{card.note}</small></article>)}</section>;
}

function WalletDistribution({ distribution, patrimonio, onBuy }) {
	const data = { labels: distribution.map((item) => item.ticker), datasets: [{ data: distribution.map((item) => item.marketValue), backgroundColor: distribution.map((item) => item.color), borderColor: "transparent", borderWidth: 0, hoverOffset: 4 }] };
	const options = { responsive: true, maintainAspectRatio: false, cutout: "72%", plugins: { legend: { display: false }, walletCenterText: { value: formatMoney(patrimonio) }, tooltip: { callbacks: { label(context) { const item = distribution[context.dataIndex]; return ` ${item.ticker}: ${formatMoney(item.marketValue)} (${formatPercent(item.percentage).replace("+", "")})`; } } } } };
	return <article className="dashboard-card wallet-distribution"><header className="dashboard-card-head"><div><h3>Distribuição da carteira</h3><p>Participação por valor de mercado</p></div></header>{distribution.length === 0 ? <WalletNoPositions onBuy={onBuy} compact /> : <div className="distribution-content"><div className="donut-wrap"><Doughnut data={data} options={options} /></div><ul className="distribution-legend">{distribution.map((item) => <li key={item.acaoId ?? item.ticker}><span className="legend-dot" style={{ backgroundColor: item.color }} /><strong>{item.ticker}</strong><span>{formatPercent(item.percentage).replace("+", "")}</span></li>)}</ul></div>}</article>;
}

function WalletSecondaryMetrics({ metrics }) {
	return <article className="dashboard-card summary-card"><header className="dashboard-card-head"><div><h3>Indicadores complementares</h3><p>Médias e composição atual</p></div></header><dl className="summary-list"><div><dt>Preço médio da carteira</dt><dd>{formatMoney(metrics?.mediaPrecoMedioPonderado)}</dd></div><div><dt>Valor médio por título</dt><dd>{formatMoney(metrics?.mediaValorMercadoPorTitulo)}</dd></div><div><dt>Posições</dt><dd>{formatNumber(metrics?.quantidadePosicoes)}</dd></div><div><dt>Total de ações</dt><dd>{formatNumber(metrics?.quantidadeTotalTitulos)}</dd></div></dl></article>;
}

function WalletPositions({ positions, metrics, onBuy, onSell }) {
	const positionCount = Number(metrics?.quantidadePosicoes);
	const titleCount = Number(metrics?.quantidadeTotalTitulos);
	return <section className="dashboard-card wallet-positions-card"><header className="dashboard-card-head wallet-positions-head"><div><h3>Posições</h3><p>{formatNumber(positionCount)} {positionCount === 1 ? "posição" : "posições"} · {formatNumber(titleCount)} {titleCount === 1 ? "ação" : "ações"}</p></div><div><button type="button" className="wallet-link-action" onClick={onBuy}>Comprar</button>{positions.length > 0 ? <button type="button" className="wallet-link-action" onClick={onSell}>Vender</button> : null}</div></header>{positions.length === 0 ? <WalletNoPositions onBuy={onBuy} /> : <div className="dashboard-table-wrap"><table className="dashboard-table wallet-positions-table"><thead><tr><th>ID</th><th>Ativo</th><th>Quantidade</th><th>Preço médio</th><th>Cotação</th><th>Custo</th><th>Valor atual</th><th>Resultado atual</th></tr></thead><tbody>{positions.map((position) => { const result = Number(position.valorMercadoAtual) - Number(position.valorCustoTotal); const cost = Number(position.valorCustoTotal); const resultPercent = cost > 0 ? (result / cost) * 100 : null; const tone = valueTone(result); return <tr key={position.acaoId ?? position.ticker}><td><span className="table-id">#{position.acaoId}</span></td><td><strong>{position.ticker}</strong>{position.nomeEmpresa ? <small>{position.nomeEmpresa}</small> : null}</td><td>{formatNumber(position.quantidade)}</td><td>{formatMoney(position.precoMedioPonderado)}</td><td>{formatMoney(position.cotacaoAtual)}</td><td>{formatMoney(position.valorCustoTotal)}</td><td><strong>{formatMoney(position.valorMercadoAtual)}</strong></td><td><strong className={`value--${tone}`}>{formatMoney(result)}</strong>{resultPercent != null ? <small className={`value--${tone}`}>{formatPercent(resultPercent)}</small> : null}</td></tr>; })}</tbody></table></div>}</section>;
}

function CreateWalletDialog({ usuarioId, brokers, loadingOptions, optionsError, onClose, onCreated }) {
	const [form, setForm] = useState({ name: "", brokerId: "", initialBalance: "" });
	const [status, setStatus] = useState(EMPTY_FORM_STATUS);
	const [submitting, setSubmitting] = useState(false);
	async function submit(event) { event.preventDefault(); if (submitting) return; setSubmitting(true); setStatus(EMPTY_FORM_STATUS); const body = { usuarioId: Number(usuarioId), nomeDaCarteira: form.name.trim() }; if (form.brokerId) body.corretoraId = Number(form.brokerId); if (String(form.initialBalance).trim()) body.saldoInicial = form.initialBalance; try { const created = await api("POST", "/carteiras", body); await onCreated(created); } catch (error) { setStatus({ text: error.message || "Não foi possível criar a carteira.", error: true }); } finally { setSubmitting(false); } }
	return <WalletDialog title="Nova carteira" subtitle="Crie uma carteira vinculada à sua conta." onClose={onClose}><form className="wallet-dialog-form" onSubmit={submit}><label><span>Nome da carteira</span><input type="text" required autoFocus maxLength="100" placeholder="Ex.: Carteira Principal" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label><label><span>Corretora <small>opcional</small></span><select value={form.brokerId} onChange={(event) => setForm({ ...form, brokerId: event.target.value })} disabled={loadingOptions}><option value="">Sem corretora</option>{brokers.map((broker) => <option key={broker.id} value={broker.id}>{broker.id} — {broker.nomeFantasia || broker.razaoSocial}</option>)}</select></label><label><span>Saldo inicial <small>opcional</small></span><div className="money-input"><span>R$</span><input type="number" min="0" step="0.01" placeholder="0,00" value={form.initialBalance} onChange={(event) => setForm({ ...form, initialBalance: event.target.value })} /></div></label>{optionsError ? <FormMessage status={{ text: optionsError, error: true }} /> : null}<FormMessage status={status} /><DialogActions onClose={onClose} submitting={submitting} submitLabel="Criar carteira" busyLabel="Criando..." /></form></WalletDialog>;
}

function EditWalletDialog({ wallet, brokers, loadingOptions, optionsError, onClose, onSaved }) {
	const [form, setForm] = useState({ name: wallet.nomeDaCarteira || "", brokerId: wallet.corretoraId != null ? String(wallet.corretoraId) : "", initialBalance: wallet.saldoTotal ?? "" });
	const [status, setStatus] = useState(EMPTY_FORM_STATUS);
	const [submitting, setSubmitting] = useState(false);
	async function submit(event) { event.preventDefault(); if (submitting) return; setSubmitting(true); setStatus(EMPTY_FORM_STATUS); const body = { nomeDaCarteira: form.name.trim() }; if (form.brokerId) body.corretoraId = Number(form.brokerId); if (!wallet.possuiOperacoes) body.saldoInicial = form.initialBalance; try { const updated = await api("PUT", "/carteiras/" + encodeURIComponent(wallet.id), body); await onSaved(updated); } catch (error) { setStatus({ text: error.message || "Não foi possível atualizar a carteira.", error: true }); } finally { setSubmitting(false); } }
	return <WalletDialog title="Editar carteira" subtitle={`Carteira #${wallet.id}`} onClose={onClose}><form className="wallet-dialog-form" onSubmit={submit}><label><span>Nome da carteira</span><input type="text" required autoFocus maxLength="100" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label><label><span>Corretora</span><select value={form.brokerId} onChange={(event) => setForm({ ...form, brokerId: event.target.value })} disabled={loadingOptions}><option value="">{wallet.corretoraId ? "Manter corretora atual" : "Sem corretora"}</option>{brokers.map((broker) => <option key={broker.id} value={broker.id}>{broker.id} — {broker.nomeFantasia || broker.razaoSocial}</option>)}</select></label><label><span>Saldo inicial</span><div className="money-input"><span>R$</span><input type="number" required min="0" step="0.01" value={form.initialBalance} onChange={(event) => setForm({ ...form, initialBalance: event.target.value })} disabled={wallet.possuiOperacoes} /></div></label><p className="wallet-form-hint">O saldo inicial pode ser ajustado enquanto a carteira não possuir operações. Após a primeira movimentação, o saldo é controlado exclusivamente pelas operações financeiras.</p>{optionsError ? <FormMessage status={{ text: optionsError, error: true }} /> : null}<FormMessage status={status} /><DialogActions onClose={onClose} submitting={submitting} submitLabel="Salvar alterações" busyLabel="Salvando..." /></form></WalletDialog>;
}

function BuyDialog({ wallet, actions, loadingOptions, optionsError, onClose, onCompleted }) {
	const [form, setForm] = useState({ actionId: "", quantity: "", operationPrice: "" });
	const [status, setStatus] = useState(EMPTY_FORM_STATUS);
	const [submitting, setSubmitting] = useState(false);
	const [receipt, setReceipt] = useState(null);
	const selectedAction = actions.find((item) => String(item.id) === form.actionId);
	const estimated = Number(form.operationPrice) > 0 && Number(form.quantity) > 0 ? Number(form.operationPrice) * Number(form.quantity) : null;
	async function submit(event) { event.preventDefault(); if (submitting) return; setSubmitting(true); setStatus(EMPTY_FORM_STATUS); try { const result = await api("POST", "/carteiras/" + encodeURIComponent(wallet.id) + "/compras", { acaoId: Number(form.actionId), quantidade: form.quantity, precoUnitario: form.operationPrice }); setReceipt(result); await onCompleted(); } catch (error) { setStatus({ text: error.message || "Não foi possível registrar a compra.", error: true }); } finally { setSubmitting(false); } }
	if (receipt) return <WalletDialog title="Compra realizada" subtitle={`Carteira #${wallet.id} — ${wallet.nomeDaCarteira}`} onClose={onClose}><OperationReceipt result={receipt} operation="buy" /><div className="wallet-dialog-actions"><button type="button" className="wallet-dialog-submit" onClick={onClose}>Concluir</button></div></WalletDialog>;
	return <WalletDialog title="Comprar ativo" subtitle={`Carteira #${wallet.id} — ${wallet.nomeDaCarteira}`} onClose={onClose}><form className="wallet-dialog-form" onSubmit={submit}><div className="operation-wallet-summary"><span>Saldo disponível</span><strong>{formatMoney(wallet.saldoTotal)}</strong></div><label><span>Ativo</span><select required autoFocus value={form.actionId} onChange={(event) => { const action = actions.find((item) => String(item.id) === event.target.value); setForm({ ...form, actionId: event.target.value, operationPrice: action?.cotacaoAtual ?? "" }); }} disabled={loadingOptions}><option value="">Selecione uma ação</option>{actions.map((action) => <option key={action.id} value={action.id}>{action.id} — {action.ticker}{action.nomeEmpresa ? ` — ${action.nomeEmpresa}` : ""}</option>)}</select></label><label><span>Quantidade</span><input type="number" required min="0.00000001" step="any" placeholder="Ex.: 10" value={form.quantity} onChange={(event) => setForm({ ...form, quantity: event.target.value })} /></label><div className="operation-price-grid"><div><span>Cotação atual/de referência</span><strong>{selectedAction?.cotacaoAtual != null ? formatMoney(selectedAction.cotacaoAtual) : "Indisponível"}</strong></div><label><span>Preço da operação</span><div className="money-input"><span>R$</span><input type="number" required min="0.000001" step="0.000001" value={form.operationPrice} onChange={(event) => setForm({ ...form, operationPrice: event.target.value })} /></div></label></div><OperationEstimate total={estimated} operation="buy" />{optionsError ? <FormMessage status={{ text: optionsError, error: true }} /> : null}<FormMessage status={status} /><DialogActions onClose={onClose} submitting={submitting} submitLabel="Confirmar compra" busyLabel="Comprando..." disabled={!actions.length} /></form></WalletDialog>;
}

function SellDialog({ wallet, positions, onClose, onCompleted }) {
	const [form, setForm] = useState({ actionId: positions[0]?.acaoId != null ? String(positions[0].acaoId) : "", quantity: "", operationPrice: positions[0]?.cotacaoAtual ?? "" });
	const [status, setStatus] = useState(EMPTY_FORM_STATUS);
	const [submitting, setSubmitting] = useState(false);
	const [receipt, setReceipt] = useState(null);
	const selectedPosition = positions.find((item) => String(item.acaoId) === form.actionId);
	const estimated = Number(form.operationPrice) > 0 && Number(form.quantity) > 0 ? Number(form.operationPrice) * Number(form.quantity) : null;
	const estimatedResult = estimated != null && selectedPosition ? (Number(form.operationPrice) - Number(selectedPosition.precoMedioPonderado)) * Number(form.quantity) : null;
	async function submit(event) { event.preventDefault(); if (submitting) return; setSubmitting(true); setStatus(EMPTY_FORM_STATUS); try { const result = await api("POST", "/carteiras/" + encodeURIComponent(wallet.id) + "/vendas", { acaoId: Number(form.actionId), quantidade: form.quantity, precoUnitario: form.operationPrice }); setReceipt(result); await onCompleted(); } catch (error) { setStatus({ text: error.message || "Não foi possível registrar a venda.", error: true }); } finally { setSubmitting(false); } }
	if (receipt) return <WalletDialog title="Venda realizada" subtitle={`Carteira #${wallet.id} — ${wallet.nomeDaCarteira}`} onClose={onClose}><OperationReceipt result={receipt} operation="sell" /><div className="wallet-dialog-actions"><button type="button" className="wallet-dialog-submit" onClick={onClose}>Concluir</button></div></WalletDialog>;
	return <WalletDialog title="Vender posição" subtitle={`Carteira #${wallet.id} — ${wallet.nomeDaCarteira}`} onClose={onClose}><form className="wallet-dialog-form" onSubmit={submit}><label><span>Posição</span><select required autoFocus value={form.actionId} onChange={(event) => { const position = positions.find((item) => String(item.acaoId) === event.target.value); setForm({ ...form, actionId: event.target.value, operationPrice: position?.cotacaoAtual ?? "" }); }}>{positions.map((position) => <option key={position.acaoId} value={position.acaoId}>{position.acaoId} — {position.ticker} — {formatNumber(position.quantidade)} {Number(position.quantidade) === 1 ? "ação" : "ações"}</option>)}</select></label>{selectedPosition ? <div className="position-reference-grid"><div><span>Disponível</span><strong>{formatNumber(selectedPosition.quantidade)}</strong></div><div><span>Preço médio</span><strong>{formatMoney(selectedPosition.precoMedioPonderado)}</strong></div><div><span>Cotação de referência</span><strong>{formatMoney(selectedPosition.cotacaoAtual)}</strong></div></div> : null}<label><span>Quantidade a vender</span><input type="number" required min="0.00000001" max={selectedPosition?.quantidade} step="any" placeholder="Ex.: 5" value={form.quantity} onChange={(event) => setForm({ ...form, quantity: event.target.value })} /></label><label><span>Preço da operação</span><div className="money-input"><span>R$</span><input type="number" required min="0.000001" step="0.000001" value={form.operationPrice} onChange={(event) => setForm({ ...form, operationPrice: event.target.value })} /></div></label><OperationEstimate total={estimated} result={estimatedResult} operation="sell" /><FormMessage status={status} /><DialogActions onClose={onClose} submitting={submitting} submitLabel="Confirmar venda" busyLabel="Vendendo..." /></form></WalletDialog>;
}

function OperationEstimate({ total, result, operation }) {
	return <div className={`operation-estimate operation-estimate--${operation}`}><div><span>{operation === "buy" ? "Custo da operação" : "Valor da venda"}</span><strong>{total != null && Number.isFinite(total) ? formatMoney(total) : "—"}</strong></div>{operation === "sell" ? <div><span>Resultado desta venda</span><strong className={`value--${valueTone(result)}`}>{result != null && Number.isFinite(result) ? formatMoney(result) : "—"}</strong></div> : null}<p>Cálculo baseado na quantidade e no preço da operação informados.</p></div>;
}

function OperationReceipt({ result, operation }) {
	const tone = valueTone(result.lucroPrejuizoRealizadoCarteira);
	return <div className="operation-receipt"><span className="operation-success-icon" aria-hidden="true">✓</span><h3>{operation === "buy" ? "Compra registrada com sucesso" : "Venda registrada com sucesso"}</h3><dl><div><dt>Ativo</dt><dd>#{result.acaoId} — {result.ticker}</dd></div><div><dt>Quantidade</dt><dd>{formatNumber(result.quantidade)}</dd></div><div><dt>Preço utilizado</dt><dd>{formatMoney(result.precoUnitario)}</dd></div><div><dt>Saldo após operação</dt><dd>{formatMoney(result.saldoCarteiraApos)}</dd></div>{operation === "sell" ? <div><dt>Resultado realizado acumulado</dt><dd className={`value--${tone}`}>{formatMoney(result.lucroPrejuizoRealizadoCarteira)}</dd></div> : null}</dl><p>Os indicadores e as posições da carteira já foram atualizados.</p></div>;
}

function WalletDialog({ title, subtitle, onClose, children }) {
	useEffect(() => { function handleKey(event) { if (event.key === "Escape") onClose(); } document.addEventListener("keydown", handleKey); return () => document.removeEventListener("keydown", handleKey); }, [onClose]);
	return <div className="wallet-dialog-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><section className="wallet-dialog" role="dialog" aria-modal="true" aria-labelledby="wallet-dialog-title"><header><div><h2 id="wallet-dialog-title">{title}</h2><p>{subtitle}</p></div><button type="button" className="wallet-dialog-close" aria-label="Fechar" onClick={onClose}>×</button></header>{children}</section></div>;
}

function DialogActions({ onClose, submitting, submitLabel, busyLabel, disabled = false }) {
	return <div className="wallet-dialog-actions"><button type="button" className="wallet-dialog-cancel" onClick={onClose} disabled={submitting}>Cancelar</button><button type="submit" className="wallet-dialog-submit" disabled={submitting || disabled}>{submitting ? busyLabel : submitLabel}</button></div>;
}

function FormMessage({ status }) {
	if (!status?.text) return null;
	return <p className={"wallet-form-message" + (status.error ? " wallet-form-message--error" : "")} role={status.error ? "alert" : "status"}>{status.text}</p>;
}

function PageNotice({ notice, onClose }) {
	return <div className={`wallet-page-notice wallet-page-notice--${notice.type}`} role={notice.type === "error" ? "alert" : "status"}><span>{notice.text}</span>{notice.type !== "loading" ? <button type="button" onClick={onClose} aria-label="Fechar mensagem">×</button> : null}</div>;
}

function NoWallets({ onCreate }) {
	return <div className="wallets-empty"><span className="empty-icon" aria-hidden="true"><WalletIcon /></span><h3>Você ainda não possui uma carteira.</h3><p>Crie sua primeira carteira para começar a organizar seus investimentos.</p><button type="button" onClick={onCreate}>Criar minha primeira carteira</button></div>;
}

function WalletNoPositions({ onBuy, compact = false }) {
	return <div className={"wallet-positions-empty" + (compact ? " wallet-positions-empty--compact" : "")}><strong>Esta carteira ainda não possui posições.</strong><p>{compact ? "A distribuição será exibida após a primeira compra." : "Registre uma compra para começar a acompanhar sua distribuição."}</p>{compact ? null : <button type="button" onClick={onBuy}>Registrar primeira compra</button>}</div>;
}

function InlineError({ message, onRetry }) {
	return <div className="wallet-inline-error" role="alert"><strong>Não foi possível carregar os dados.</strong><span>{message}</span><button type="button" onClick={onRetry}>Tentar novamente</button></div>;
}

function WalletListLoading() {
	return <div className="wallet-list-loading" aria-label="Carregando carteiras">{[0, 1, 2].map((item) => <span key={item} />)}</div>;
}

function WalletDetailLoading() {
	return <div className="wallet-detail-loading" aria-label="Carregando carteira"><div /><section>{[0, 1, 2, 3, 4].map((item) => <span key={item} />)}</section><div /></div>;
}

function WalletIcon() {
	return <svg viewBox="0 0 24 24"><path d="M4 7.5h14a2 2 0 0 1 2 2v9H5a2 2 0 0 1-2-2v-11a2 2 0 0 1 2-2h11" /><path d="M20 12h-5a2 2 0 0 0 0 4h5" /></svg>;
}
