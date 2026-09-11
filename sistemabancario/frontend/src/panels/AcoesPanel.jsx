import { useCallback, useEffect, useMemo, useState } from "react";
import CotacoesChart from "../components/CotacoesChart";
import { EntityDialog, EntityDialogActions, EntityEmpty, EntityFormMessage, EntityLoading, EntityNotice, EntityPageHeader } from "../components/EntityUi";
import { api } from "../services/api";
import { formatDateTime, formatMoney, formatNumber } from "../services/format";

const EMPTY_STATUS = { text: "", error: false };

export default function AcoesPanel() {
	const [actions, setActions] = useState([]);
	const [selectedId, setSelectedId] = useState("");
	const [selected, setSelected] = useState(null);
	const [history, setHistory] = useState([]);
	const [loading, setLoading] = useState(true);
	const [detailLoading, setDetailLoading] = useState(false);
	const [historyError, setHistoryError] = useState("");
	const [notice, setNotice] = useState(null);
	const [filters, setFilters] = useState({ id: "", text: "" });
	const [appliedText, setAppliedText] = useState("");
	const [searchResult, setSearchResult] = useState(null);
	const [searching, setSearching] = useState(false);
	const [updating, setUpdating] = useState(false);
	const [deleting, setDeleting] = useState(false);
	const [dialogOpen, setDialogOpen] = useState(false);
	const [brokers, setBrokers] = useState([]);

	const loadActions = useCallback(async (preferredId) => {
		setLoading(true);
		try {
			const page = await api("GET", "/acoes?size=100");
			const rows = page?.content || [];
			setActions(rows);
			setSelectedId((current) => {
				const preferred = preferredId != null ? String(preferredId) : current;
				return rows.some((item) => String(item.id) === preferred) ? preferred : (rows[0]?.id != null ? String(rows[0].id) : "");
			});
			if (!rows.length) setSelected(null);
		} catch (error) {
			setNotice({ type: "error", text: error.message || "Não foi possível carregar as ações." });
			setActions([]);
		} finally { setLoading(false); }
	}, []);

	useEffect(() => { loadActions(); }, [loadActions]);

	const loadDetail = useCallback(async (id) => {
		if (!id) return;
		setDetailLoading(true);
		setHistoryError("");
		const [detailResult, historyResult] = await Promise.allSettled([
			api("GET", "/acoes/" + encodeURIComponent(id)),
			api("GET", "/acoes/" + encodeURIComponent(id) + "/historico-cotacoes?size=500"),
		]);
		if (detailResult.status === "fulfilled") setSelected(detailResult.value);
		else { setSelected(null); setNotice({ type: "error", text: detailResult.reason?.message || "Não foi possível carregar o ativo." }); }
		if (historyResult.status === "fulfilled") setHistory(historyResult.value?.content || []);
		else { setHistory([]); setHistoryError(historyResult.reason?.message || "Não foi possível carregar o histórico."); }
		setDetailLoading(false);
	}, []);

	useEffect(() => { loadDetail(selectedId); }, [selectedId, loadDetail]);

	const visibleActions = useMemo(() => {
		const base = searchResult !== null ? searchResult : actions;
		if (!appliedText) return base;
		const query = appliedText.toLocaleLowerCase("pt-BR");
		return base.filter((item) => item.ticker?.toLocaleLowerCase("pt-BR").includes(query) || item.nomeEmpresa?.toLocaleLowerCase("pt-BR").includes(query));
	}, [actions, searchResult, appliedText]);

	async function search(event) {
		event.preventDefault(); setNotice(null); setSearching(true);
		const id = filters.id.trim(); const text = filters.text.trim();
		try {
			if (id) {
				const item = await api("GET", "/acoes/" + encodeURIComponent(id));
				const query = text.toLocaleLowerCase("pt-BR");
				const matches = !text || item.ticker?.toLocaleLowerCase("pt-BR").includes(query) || item.nomeEmpresa?.toLocaleLowerCase("pt-BR").includes(query);
				setSearchResult(matches ? [item] : []); setAppliedText(""); if (matches) setSelectedId(String(item.id));
			} else if (text && actions.some((item) => item.ticker?.toUpperCase() === text.toUpperCase())) {
				const item = await api("GET", "/acoes/ticker/" + encodeURIComponent(text.toUpperCase()));
				setSearchResult([item]); setAppliedText(""); setSelectedId(String(item.id));
			} else { setSearchResult(null); setAppliedText(text); }
		} catch (error) {
			setSearchResult([]); setAppliedText(""); setNotice({ type: "error", text: error.message || "Ativo não encontrado." });
		} finally { setSearching(false); }
	}

	function clearFilters() { setFilters({ id: "", text: "" }); setAppliedText(""); setSearchResult(null); setNotice(null); }

	async function openCreate() {
		setDialogOpen(true);
		if (brokers.length) return;
		try { const page = await api("GET", "/corretoras?size=100"); setBrokers(page?.content || []); }
		catch (error) { setNotice({ type: "error", text: error.message || "Não foi possível carregar as corretoras." }); }
	}

	async function updateQuote() {
		if (!selected || updating) return;
		setUpdating(true); setNotice({ type: "loading", text: "Atualizando cotação..." });
		try { await api("PUT", "/acoes/" + encodeURIComponent(selected.id) + "/atualizar-cotacao"); await loadActions(selected.id); await loadDetail(selected.id); setNotice({ type: "success", text: "Cotação atualizada com sucesso." }); }
		catch (error) { setNotice({ type: "error", text: error.message || "Não foi possível atualizar a cotação." }); }
		finally { setUpdating(false); }
	}

	async function removeAction() {
		if (!selected || deleting || !window.confirm(`Excluir definitivamente a ação #${selected.id} — ${selected.ticker}?`)) return;
		setDeleting(true); setNotice({ type: "loading", text: "Excluindo ação..." });
		try { await api("DELETE", "/acoes/" + encodeURIComponent(selected.id)); setSearchResult(null); await loadActions(""); setNotice({ type: "success", text: "Ação excluída com sucesso." }); }
		catch (error) { setNotice({ type: "error", text: error.message || "Não foi possível excluir a ação." }); }
		finally { setDeleting(false); }
	}

	return <div className="entity-page">
		<EntityPageHeader eyebrow="Mercado" title="Ações" description="Acompanhe ativos, cotações e histórico." actionLabel="+ Nova ação" onAction={openCreate} />
		<EntityNotice notice={notice} onClose={() => setNotice(null)} />
		<section className="dashboard-card entity-list-card"><header className="entity-card-head"><div><h3>Ativos cadastrados</h3><p>Selecione um ativo para acompanhar sua cotação.</p></div><span>{formatNumber(actions.length)} {actions.length === 1 ? "ação" : "ações"}</span></header>
			<form className="entity-filters" onSubmit={search}><label><span>ID</span><input type="number" min="1" placeholder="Ex.: 1" value={filters.id} onChange={(e) => setFilters({ ...filters, id: e.target.value })} /></label><label><span>Ticker ou nome</span><input type="search" placeholder="Ex.: PETR4 ou Petrobras" value={filters.text} onChange={(e) => setFilters({ ...filters, text: e.target.value })} /></label><button type="submit" disabled={searching}>{searching ? "Buscando..." : "Buscar"}</button><button type="button" className="wallet-filter-clear" onClick={clearFilters}>Limpar</button></form>
			{loading ? <EntityLoading label="Carregando ações..." /> : actions.length === 0 ? <EntityEmpty title="Nenhuma ação cadastrada." description="Cadastre um ativo para começar a acompanhar suas cotações." actionLabel="Cadastrar primeira ação" onAction={openCreate} /> : visibleActions.length === 0 ? <EntityEmpty title="Nenhum ativo corresponde aos filtros." actionLabel="Limpar filtros" onAction={clearFilters} /> : <ActionsTable rows={visibleActions} selectedId={selectedId} onSelect={setSelectedId} />}
		</section>
		{actions.length ? detailLoading ? <EntityLoading label="Carregando detalhes e histórico..." /> : selected ? <><ActionDetail action={selected} brokers={brokers} updating={updating} deleting={deleting} onUpdate={updateQuote} onDelete={removeAction} /><section className="dashboard-card entity-chart-card"><header className="dashboard-card-head"><div><h3>Histórico de cotação</h3><p>Registros reais persistidos para {selected.ticker}</p></div><span className="entity-count-badge">{formatNumber(history.length)} {history.length === 1 ? "ponto" : "pontos"}</span></header>{historyError ? <EntityFormMessage status={{ text: historyError, error: true }} /> : history.length < 2 ? <EntityEmpty title="Ainda não há histórico de cotação suficiente para este ativo." description="Atualize a cotação para criar novos registros ao longo do tempo." /> : <CotacoesChart points={history} />}</section></> : null : null}
		{dialogOpen ? <CreateActionDialog brokers={brokers} onClose={() => setDialogOpen(false)} onCreated={async (item) => { setDialogOpen(false); await loadActions(item.id); setNotice({ type: "success", text: `Ação #${item.id} — ${item.ticker} cadastrada com sucesso.` }); }} /> : null}
	</div>;
}

function ActionsTable({ rows, selectedId, onSelect }) {
	return <div className="dashboard-table-wrap"><table className="dashboard-table entity-table"><thead><tr><th>ID</th><th>Ativo</th><th>Mercado</th><th>Cotação atual</th><th>Atualização</th><th>Corretora</th><th /></tr></thead><tbody>{rows.map((item) => <tr key={item.id} className={String(item.id) === selectedId ? "entity-row--selected" : ""}><td><span className="table-id">#{item.id}</span></td><td><strong>{item.ticker}</strong><small>{item.nomeEmpresa || "Empresa não informada"}</small></td><td><span className="entity-badge">{marketLabel(item.mercado)}</span></td><td><strong>{formatMoney(item.cotacaoAtual)}</strong></td><td>{formatDateTime(item.dataHoraCotacao)}</td><td>{item.corretoraId ? `#${item.corretoraId}` : "—"}</td><td><button type="button" className="entity-table-action" onClick={() => onSelect(String(item.id))}>Ver detalhes</button></td></tr>)}</tbody></table></div>;
}

function ActionDetail({ action, brokers, updating, deleting, onUpdate, onDelete }) {
	const broker = brokers.find((item) => String(item.id) === String(action.corretoraId));
	return <section className="dashboard-card entity-detail"><div className="entity-detail-primary"><span className="wallet-id">ID {action.id}</span><div><span className="entity-symbol">{action.ticker}</span><h3>{action.nomeEmpresa || "Empresa não informada"}</h3><p>{marketLabel(action.mercado)} · {action.corretoraId ? (broker?.nomeFantasia || broker?.razaoSocial || `Corretora #${action.corretoraId}`) : "Sem corretora associada"}</p></div></div><div className="entity-quote"><span>Cotação atual</span><strong>{formatMoney(action.cotacaoAtual)}</strong><small>Atualizada em {formatDateTime(action.dataHoraCotacao)}</small></div><div className="entity-detail-actions"><button type="button" className="wallet-action wallet-action--buy" onClick={onUpdate} disabled={updating}>{updating ? "Atualizando..." : "Atualizar cotação"}</button><button type="button" className="wallet-action wallet-action--danger" onClick={onDelete} disabled={deleting}>{deleting ? "Excluindo..." : "Excluir ação"}</button></div></section>;
}

function CreateActionDialog({ brokers, onClose, onCreated }) {
	const [form, setForm] = useState({ ticker: "", market: "BRASIL", brokerId: "" });
	const [status, setStatus] = useState(EMPTY_STATUS);
	const [submitting, setSubmitting] = useState(false);
	async function submit(event) { event.preventDefault(); if (submitting) return; setSubmitting(true); setStatus(EMPTY_STATUS); const body = { ticker: form.ticker.trim().toUpperCase(), mercado: form.market }; if (form.brokerId) body.corretoraId = Number(form.brokerId); try { const created = await api("POST", "/acoes", body); await onCreated(created); } catch (error) { setStatus({ text: error.message || "Não foi possível cadastrar a ação.", error: true }); } finally { setSubmitting(false); } }
	return <EntityDialog title="Nova ação" subtitle="A cotação inicial será consultada pela integração do mercado." onClose={onClose}><form className="wallet-dialog-form" onSubmit={submit}><label><span>Ticker</span><input type="text" required autoFocus maxLength="15" placeholder="Ex.: PETR4" value={form.ticker} onChange={(e) => setForm({ ...form, ticker: e.target.value.toUpperCase() })} /></label><label><span>Mercado</span><select required value={form.market} onChange={(e) => setForm({ ...form, market: e.target.value })}><option value="BRASIL">Brasil</option><option value="ESTADOS_UNIDOS">Estados Unidos</option></select></label><label><span>Corretora <small>opcional</small></span><select value={form.brokerId} onChange={(e) => setForm({ ...form, brokerId: e.target.value })}><option value="">Sem corretora</option>{brokers.map((broker) => <option key={broker.id} value={broker.id}>{broker.id} — {broker.nomeFantasia || broker.razaoSocial}</option>)}</select></label><p className="wallet-form-hint">O backend escolherá o provedor e consultará a cotação conforme o mercado selecionado.</p><EntityFormMessage status={status} /><EntityDialogActions onClose={onClose} submitting={submitting} submitLabel="Cadastrar ação" busyLabel="Cadastrando..." /></form></EntityDialog>;
}

function marketLabel(value) { return value === "ESTADOS_UNIDOS" ? "Estados Unidos" : value === "BRASIL" ? "Brasil" : value || "—"; }
