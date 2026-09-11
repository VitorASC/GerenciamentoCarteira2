import { useCallback, useEffect, useMemo, useState } from "react";
import { EntityDialog, EntityDialogActions, EntityEmpty, EntityFormMessage, EntityLoading, EntityNotice, EntityPageHeader } from "../components/EntityUi";
import { api } from "../services/api";
import { formatCnpj, formatNumber, stripDigits } from "../services/format";

const EMPTY_STATUS = { text: "", error: false };

export default function CorretorasPanel() {
	const [brokers, setBrokers] = useState([]);
	const [selectedId, setSelectedId] = useState("");
	const [selected, setSelected] = useState(null);
	const [loading, setLoading] = useState(true);
	const [detailLoading, setDetailLoading] = useState(false);
	const [notice, setNotice] = useState(null);
	const [filters, setFilters] = useState({ id: "", text: "" });
	const [appliedText, setAppliedText] = useState("");
	const [searchResult, setSearchResult] = useState(null);
	const [searching, setSearching] = useState(false);
	const [deleting, setDeleting] = useState(false);
	const [dialogOpen, setDialogOpen] = useState(false);

	const loadBrokers = useCallback(async (preferredId) => {
		setLoading(true);
		try {
			const page = await api("GET", "/corretoras?size=100");
			const rows = page?.content || [];
			setBrokers(rows);
			setSelectedId((current) => {
				const preferred = preferredId != null ? String(preferredId) : current;
				return rows.some((item) => String(item.id) === preferred) ? preferred : (rows[0]?.id != null ? String(rows[0].id) : "");
			});
			if (!rows.length) setSelected(null);
		} catch (error) { setBrokers([]); setNotice({ type: "error", text: error.message || "Não foi possível carregar as corretoras." }); }
		finally { setLoading(false); }
	}, []);

	useEffect(() => { loadBrokers(); }, [loadBrokers]);
	useEffect(() => {
		if (!selectedId) return;
		let active = true; setDetailLoading(true);
		api("GET", "/corretoras/" + encodeURIComponent(selectedId))
			.then((data) => { if (active) setSelected(data); })
			.catch((error) => { if (active) { setSelected(null); setNotice({ type: "error", text: error.message || "Não foi possível carregar a corretora." }); } })
			.finally(() => { if (active) setDetailLoading(false); });
		return () => { active = false; };
	}, [selectedId]);

	const visibleBrokers = useMemo(() => {
		const base = searchResult !== null ? searchResult : brokers;
		if (!appliedText) return base;
		const query = appliedText.toLocaleLowerCase("pt-BR");
		return base.filter((item) => item.razaoSocial?.toLocaleLowerCase("pt-BR").includes(query) || item.nomeFantasia?.toLocaleLowerCase("pt-BR").includes(query) || stripDigits(item.cnpj).includes(stripDigits(appliedText)));
	}, [brokers, searchResult, appliedText]);

	async function search(event) {
		event.preventDefault(); setNotice(null); setSearching(true);
		const id = filters.id.trim(); const text = filters.text.trim(); const digits = stripDigits(text);
		try {
			let result = null;
			if (id) result = await api("GET", "/corretoras/" + encodeURIComponent(id));
			else if (digits.length === 14) result = await api("GET", "/corretoras/cnpj/" + encodeURIComponent(digits));
			if (result) {
				const query = text.toLocaleLowerCase("pt-BR");
				const matches = !text || stripDigits(result.cnpj) === digits || result.razaoSocial?.toLocaleLowerCase("pt-BR").includes(query) || result.nomeFantasia?.toLocaleLowerCase("pt-BR").includes(query);
				setSearchResult(matches ? [result] : []); setAppliedText(""); if (matches) setSelectedId(String(result.id));
			} else { setSearchResult(null); setAppliedText(text); }
		} catch (error) { setSearchResult([]); setAppliedText(""); setNotice({ type: "error", text: error.message || "Corretora não encontrada." }); }
		finally { setSearching(false); }
	}

	function clearFilters() { setFilters({ id: "", text: "" }); setAppliedText(""); setSearchResult(null); setNotice(null); }

	async function removeBroker() {
		if (!selected || deleting || !window.confirm(`Excluir definitivamente a corretora #${selected.id} — ${selected.nomeFantasia || selected.razaoSocial}?`)) return;
		setDeleting(true); setNotice({ type: "loading", text: "Excluindo corretora..." });
		try { await api("DELETE", "/corretoras/" + encodeURIComponent(selected.id)); setSearchResult(null); await loadBrokers(""); setNotice({ type: "success", text: "Corretora excluída com sucesso." }); }
		catch (error) { setNotice({ type: "error", text: error.message || "Não foi possível excluir a corretora." }); }
		finally { setDeleting(false); }
	}

	return <div className="entity-page">
		<EntityPageHeader eyebrow="Instituições" title="Corretoras" description="Gerencie instituições e validações cadastrais." actionLabel="+ Nova corretora" onAction={() => setDialogOpen(true)} />
		<EntityNotice notice={notice} onClose={() => setNotice(null)} />
		<section className="dashboard-card entity-list-card"><header className="entity-card-head"><div><h3>Instituições cadastradas</h3><p>Consulte dados cadastrais e validação CVM.</p></div><span>{formatNumber(brokers.length)} {brokers.length === 1 ? "corretora" : "corretoras"}</span></header>
			<form className="entity-filters" onSubmit={search}><label><span>ID</span><input type="number" min="1" placeholder="Ex.: 1" value={filters.id} onChange={(e) => setFilters({ ...filters, id: e.target.value })} /></label><label><span>CNPJ ou nome</span><input type="search" placeholder="Buscar por CNPJ ou instituição" value={filters.text} onChange={(e) => setFilters({ ...filters, text: e.target.value })} /></label><button type="submit" disabled={searching}>{searching ? "Buscando..." : "Buscar"}</button><button type="button" className="wallet-filter-clear" onClick={clearFilters}>Limpar</button></form>
			{loading ? <EntityLoading label="Carregando corretoras..." /> : brokers.length === 0 ? <EntityEmpty title="Nenhuma corretora cadastrada." description="Cadastre uma instituição para vinculá-la a carteiras e ações." actionLabel="Cadastrar primeira corretora" onAction={() => setDialogOpen(true)} /> : visibleBrokers.length === 0 ? <EntityEmpty title="Nenhuma corretora corresponde aos filtros." actionLabel="Limpar filtros" onAction={clearFilters} /> : <BrokersTable rows={visibleBrokers} selectedId={selectedId} onSelect={setSelectedId} />}
		</section>
		{brokers.length ? detailLoading ? <EntityLoading label="Carregando dados cadastrais..." /> : selected ? <BrokerDetail broker={selected} deleting={deleting} onDelete={removeBroker} /> : null : null}
		{dialogOpen ? <CreateBrokerDialog onClose={() => setDialogOpen(false)} onCreated={async (item) => { setDialogOpen(false); await loadBrokers(item.id); setNotice({ type: "success", text: `Corretora #${item.id} cadastrada com sucesso.` }); }} /> : null}
	</div>;
}

function BrokersTable({ rows, selectedId, onSelect }) {
	return <div className="dashboard-table-wrap"><table className="dashboard-table entity-table"><thead><tr><th>ID</th><th>Instituição</th><th>CNPJ</th><th>Localização</th><th>Status CVM</th><th /></tr></thead><tbody>{rows.map((item) => <tr key={item.id} className={String(item.id) === selectedId ? "entity-row--selected" : ""}><td><span className="table-id">#{item.id}</span></td><td><strong>{item.nomeFantasia || item.razaoSocial}</strong>{item.nomeFantasia ? <small>{item.razaoSocial}</small> : null}</td><td>{formatCnpj(item.cnpj)}</td><td>{[item.cidade, item.uf].filter(Boolean).join(" / ") || "—"}</td><td><CvmBadge valid={item.validadaNaCvm} /></td><td><button type="button" className="entity-table-action" onClick={() => onSelect(String(item.id))}>Ver detalhes</button></td></tr>)}</tbody></table></div>;
}

function BrokerDetail({ broker, deleting, onDelete }) {
	const address = [broker.logradouro, broker.numero, broker.complemento].filter(Boolean).join(", ");
	return <section className="dashboard-card entity-record-detail"><header className="entity-detail-title"><div><span className="wallet-id">ID {broker.id}</span><h3>{broker.nomeFantasia || broker.razaoSocial}</h3><p>{broker.razaoSocial}</p></div><div><CvmBadge valid={broker.validadaNaCvm} /><button type="button" className="wallet-action wallet-action--danger" onClick={onDelete} disabled={deleting}>{deleting ? "Excluindo..." : "Excluir corretora"}</button></div></header><dl className="entity-detail-grid"><Detail label="CNPJ" value={formatCnpj(broker.cnpj)} /><Detail label="CEP" value={broker.cep} /><Detail label="Endereço" value={address || "—"} /><Detail label="Cidade / UF" value={[broker.cidade, broker.uf].filter(Boolean).join(" / ") || "—"} /><Detail label="Complemento" value={broker.complemento || "—"} /><Detail label="Situação cadastral" value={broker.situacaoCadastral || "—"} /></dl></section>;
}

function CreateBrokerDialog({ onClose, onCreated }) {
	const [form, setForm] = useState({ cnpj: "", cep: "", number: "", complement: "" });
	const [status, setStatus] = useState(EMPTY_STATUS);
	const [submitting, setSubmitting] = useState(false);
	async function submit(event) { event.preventDefault(); if (submitting) return; setSubmitting(true); setStatus(EMPTY_STATUS); const body = { cnpj: stripDigits(form.cnpj), cep: stripDigits(form.cep) }; if (form.number.trim()) body.numero = form.number.trim(); if (form.complement.trim()) body.complemento = form.complement.trim(); try { const created = await api("POST", "/corretoras", body); await onCreated(created); } catch (error) { setStatus({ text: error.message || "Não foi possível validar e cadastrar a corretora.", error: true }); } finally { setSubmitting(false); } }
	return <EntityDialog title="Nova corretora" subtitle="Os dados da empresa e o status CVM serão consultados externamente." onClose={onClose}><form className="wallet-dialog-form" onSubmit={submit}><div className="entity-form-grid"><label><span>CNPJ</span><input type="text" required autoFocus inputMode="numeric" maxLength="18" placeholder="00.000.000/0000-00" value={form.cnpj} onChange={(e) => setForm({ ...form, cnpj: e.target.value })} /></label><label><span>CEP</span><input type="text" required inputMode="numeric" maxLength="9" placeholder="00000-000" value={form.cep} onChange={(e) => setForm({ ...form, cep: e.target.value })} /></label></div><div className="entity-form-grid"><label><span>Número <small>opcional</small></span><input type="text" maxLength="20" value={form.number} onChange={(e) => setForm({ ...form, number: e.target.value })} /></label><label><span>Complemento <small>opcional</small></span><input type="text" maxLength="100" value={form.complement} onChange={(e) => setForm({ ...form, complement: e.target.value })} /></label></div><p className="wallet-form-hint">O cadastro pode levar alguns segundos enquanto CNPJ, endereço e CVM são consultados.</p><EntityFormMessage status={status} /><EntityDialogActions onClose={onClose} submitting={submitting} submitLabel="Validar e cadastrar" busyLabel="Validando e cadastrando..." /></form></EntityDialog>;
}

function CvmBadge({ valid }) { return <span className={`entity-status-badge ${valid ? "entity-status-badge--positive" : "entity-status-badge--neutral"}`}>{valid ? "Validada pela CVM" : "Não validada na CVM"}</span>; }
function Detail({ label, value }) { return <div><dt>{label}</dt><dd>{value}</dd></div>; }
