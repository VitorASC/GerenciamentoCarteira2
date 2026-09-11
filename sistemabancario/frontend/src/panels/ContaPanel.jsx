import { useCallback, useEffect, useMemo, useState } from "react";
import { EntityDialog, EntityDialogActions, EntityEmpty, EntityFormMessage, EntityLoading, EntityNotice, EntityPageHeader } from "../components/EntityUi";
import { useAuth } from "../hooks/useAuth";
import { api } from "../services/api";
import { formatCpf, formatDate, formatNumber, stripDigits } from "../services/format";

const EMPTY_STATUS = { text: "", error: false };

export default function ContaPanel({ onUserUpdated }) {
	const { usuarioId } = useAuth();
	const currentUserId = usuarioId;
	const [profile, setProfile] = useState(null);
	const [users, setUsers] = useState([]);
	const [loadingProfile, setLoadingProfile] = useState(true);
	const [loadingUsers, setLoadingUsers] = useState(true);
	const [notice, setNotice] = useState(null);
	const [filters, setFilters] = useState({ id: "", text: "" });
	const [appliedText, setAppliedText] = useState("");
	const [searchResult, setSearchResult] = useState(null);
	const [searching, setSearching] = useState(false);
	const [editOpen, setEditOpen] = useState(false);

	const loadProfile = useCallback(async () => {
		if (!currentUserId) return;
		setLoadingProfile(true);
		try { const data = await api("GET", "/usuarios/" + encodeURIComponent(currentUserId)); setProfile(data); onUserUpdated?.(data); }
		catch (error) { setNotice({ type: "error", text: error.message || "Não foi possível carregar seu perfil." }); }
		finally { setLoadingProfile(false); }
	}, [currentUserId, onUserUpdated]);

	const loadUsers = useCallback(async () => {
		setLoadingUsers(true);
		try { const page = await api("GET", "/usuarios?size=100"); setUsers(page?.content || []); }
		catch (error) { setUsers([]); setNotice({ type: "error", text: error.message || "Não foi possível carregar os usuários." }); }
		finally { setLoadingUsers(false); }
	}, []);

	useEffect(() => { loadProfile(); loadUsers(); }, [loadProfile, loadUsers]);

	const visibleUsers = useMemo(() => {
		const base = searchResult !== null ? searchResult : users;
		if (!appliedText) return base;
		const query = appliedText.toLocaleLowerCase("pt-BR"); const digits = stripDigits(appliedText);
		return base.filter((item) => item.nomeCompleto?.toLocaleLowerCase("pt-BR").includes(query) || (digits && stripDigits(item.cpf).includes(digits)));
	}, [users, searchResult, appliedText]);

	async function search(event) {
		event.preventDefault(); setNotice(null); setSearching(true);
		const id = filters.id.trim(); const text = filters.text.trim(); const digits = stripDigits(text);
		try {
			let result = null;
			if (id) result = await api("GET", "/usuarios/" + encodeURIComponent(id));
			else if (digits.length === 11) result = await api("GET", "/usuarios/cpf/" + encodeURIComponent(digits));
			if (result) {
				const query = text.toLocaleLowerCase("pt-BR");
				const matches = !text || stripDigits(result.cpf) === digits || result.nomeCompleto?.toLocaleLowerCase("pt-BR").includes(query);
				setSearchResult(matches ? [result] : []); setAppliedText("");
			} else { setSearchResult(null); setAppliedText(text); }
		} catch (error) { setSearchResult([]); setAppliedText(""); setNotice({ type: "error", text: error.message || "Usuário não encontrado." }); }
		finally { setSearching(false); }
	}

	function clearFilters() { setFilters({ id: "", text: "" }); setAppliedText(""); setSearchResult(null); setNotice(null); }

	return <div className="entity-page account-page">
		<EntityPageHeader eyebrow="Perfil" title="Conta" description="Gerencie seus dados e preferências de perfil." />
		<EntityNotice notice={notice} onClose={() => setNotice(null)} />
		{loadingProfile ? <EntityLoading label="Carregando seu perfil..." /> : profile ? <ProfileCard profile={profile} onEdit={() => setEditOpen(true)} /> : null}
		<section className="dashboard-card entity-list-card account-users"><header className="entity-card-head"><div><h3>Usuários cadastrados</h3><p>Consulta secundária para identificação e demonstração dos registros.</p></div><span>{formatNumber(users.length)} {users.length === 1 ? "usuário" : "usuários"}</span></header>
			<form className="entity-filters" onSubmit={search}><label><span>ID</span><input type="number" min="1" placeholder="Ex.: 1" value={filters.id} onChange={(e) => setFilters({ ...filters, id: e.target.value })} /></label><label><span>CPF ou nome</span><input type="search" placeholder="Buscar por CPF ou nome" value={filters.text} onChange={(e) => setFilters({ ...filters, text: e.target.value })} /></label><button type="submit" disabled={searching}>{searching ? "Buscando..." : "Buscar"}</button><button type="button" className="wallet-filter-clear" onClick={clearFilters}>Limpar</button></form>
			{loadingUsers ? <EntityLoading label="Carregando usuários..." /> : visibleUsers.length === 0 ? <EntityEmpty title="Nenhum usuário encontrado." actionLabel={filters.id || filters.text || appliedText || searchResult !== null ? "Limpar filtros" : undefined} onAction={clearFilters} /> : <UsersTable rows={visibleUsers} currentId={currentUserId} />}
		</section>
		{editOpen && profile ? <EditProfileDialog profile={profile} onClose={() => setEditOpen(false)} onSaved={(updated) => { setProfile(updated); onUserUpdated?.(updated); setUsers((rows) => rows.map((item) => String(item.id) === String(updated.id) ? updated : item)); setEditOpen(false); setNotice({ type: "success", text: "Dados atualizados com sucesso." }); }} /> : null}
	</div>;
}

function ProfileCard({ profile, onEdit }) {
	return <section className="dashboard-card profile-card"><header className="profile-head"><div className="profile-avatar" aria-hidden="true">{initials(profile.nomeCompleto)}</div><div><span className="wallet-id">ID {profile.id}</span><h3>{profile.nomeCompleto}</h3><p>{profile.email}</p></div><div className="profile-badges"><span className="entity-status-badge entity-status-badge--profile">{profileLabel(profile.perfilInvestidor)}</span><span className={`entity-status-badge ${profile.ativo ? "entity-status-badge--positive" : "entity-status-badge--neutral"}`}>{profile.ativo ? "Conta ativa" : "Conta inativa"}</span></div><button type="button" className="wallet-action wallet-action--buy" onClick={onEdit}>Editar dados</button></header><dl className="profile-grid"><Detail label="Nome completo" value={profile.nomeCompleto} /><Detail label="E-mail" value={profile.email} /><Detail label="CPF" value={formatCpf(profile.cpf)} /><Detail label="Data de nascimento" value={formatDate(profile.dataNascimento)} /><Detail label="Perfil do investidor" value={profileLabel(profile.perfilInvestidor)} /><Detail label="Situação" value={profile.ativo ? "Ativa" : "Inativa"} /></dl></section>;
}

function UsersTable({ rows, currentId }) {
	return <div className="dashboard-table-wrap"><table className="dashboard-table entity-table"><thead><tr><th>ID</th><th>Usuário</th><th>CPF</th><th>E-mail</th><th>Perfil</th><th>Status</th></tr></thead><tbody>{rows.map((item) => <tr key={item.id} className={String(item.id) === String(currentId) ? "entity-row--selected" : ""}><td><span className="table-id">#{item.id}</span></td><td><strong>{item.nomeCompleto}</strong>{String(item.id) === String(currentId) ? <small>Você</small> : null}</td><td>{formatCpf(item.cpf)}</td><td>{item.email}</td><td><span className="entity-badge">{profileLabel(item.perfilInvestidor)}</span></td><td><span className={`entity-status-badge ${item.ativo ? "entity-status-badge--positive" : "entity-status-badge--neutral"}`}>{item.ativo ? "Ativa" : "Inativa"}</span></td></tr>)}</tbody></table></div>;
}

function EditProfileDialog({ profile, onClose, onSaved }) {
	const [form, setForm] = useState({ name: profile.nomeCompleto || "", email: profile.email || "", investorProfile: profile.perfilInvestidor || "", active: String(profile.ativo) });
	const [status, setStatus] = useState(EMPTY_STATUS);
	const [submitting, setSubmitting] = useState(false);
	async function submit(event) { event.preventDefault(); if (submitting) return; setSubmitting(true); setStatus(EMPTY_STATUS); try { const updated = await api("PUT", "/usuarios/" + encodeURIComponent(profile.id), { nomeCompleto: form.name.trim(), email: form.email.trim(), perfilInvestidor: form.investorProfile, ativo: form.active === "true" }); onSaved(updated); } catch (error) { setStatus({ text: error.message || "Não foi possível atualizar seus dados.", error: true }); } finally { setSubmitting(false); } }
	return <EntityDialog title="Editar meus dados" subtitle={`Conta #${profile.id}`} onClose={onClose}><form className="wallet-dialog-form" onSubmit={submit}><label><span>Nome completo</span><input type="text" required autoFocus value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label><label><span>E-mail</span><input type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></label><div className="entity-form-grid"><label><span>Perfil do investidor</span><select required value={form.investorProfile} onChange={(e) => setForm({ ...form, investorProfile: e.target.value })}><option value="CONSERVADOR">Conservador</option><option value="MODERADO">Moderado</option><option value="ARROJADO">Arrojado</option></select></label><label><span>Situação da conta</span><select required value={form.active} onChange={(e) => setForm({ ...form, active: e.target.value })}><option value="true">Ativa</option><option value="false">Inativa</option></select></label></div><p className="wallet-form-hint">CPF, data de nascimento e senha não são alterados por este endpoint.</p><EntityFormMessage status={status} /><EntityDialogActions onClose={onClose} submitting={submitting} submitLabel="Salvar alterações" busyLabel="Salvando..." /></form></EntityDialog>;
}

function Detail({ label, value }) { return <div><dt>{label}</dt><dd>{value || "—"}</dd></div>; }
function initials(name) { return String(name || "U").split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join("").toUpperCase(); }
function profileLabel(value) { return value === "CONSERVADOR" ? "Conservador" : value === "MODERADO" ? "Moderado" : value === "ARROJADO" ? "Arrojado" : value || "—"; }
