import { useEffect } from "react";

export function EntityPageHeader({ eyebrow, title, description, actionLabel, onAction }) {
	return <header className="wallets-page-head"><div><p className="dashboard-eyebrow">{eyebrow}</p><h2>{title}</h2><p>{description}</p></div>{actionLabel ? <button type="button" className="wallet-primary-action" onClick={onAction}>{actionLabel}</button> : null}</header>;
}

export function EntityNotice({ notice, onClose }) {
	if (!notice?.text) return null;
	return <div className={`wallet-page-notice wallet-page-notice--${notice.type || "success"}`} role={notice.type === "error" ? "alert" : "status"}><span>{notice.text}</span>{notice.type !== "loading" ? <button type="button" onClick={onClose} aria-label="Fechar mensagem">×</button> : null}</div>;
}

export function EntityDialog({ title, subtitle, onClose, children }) {
	useEffect(() => { const handleKey = (event) => { if (event.key === "Escape") onClose(); }; document.addEventListener("keydown", handleKey); return () => document.removeEventListener("keydown", handleKey); }, [onClose]);
	return <div className="wallet-dialog-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><section className="wallet-dialog" role="dialog" aria-modal="true" aria-labelledby="entity-dialog-title"><header><div><h2 id="entity-dialog-title">{title}</h2><p>{subtitle}</p></div><button type="button" className="wallet-dialog-close" aria-label="Fechar" onClick={onClose}>×</button></header>{children}</section></div>;
}

export function EntityDialogActions({ onClose, submitting, submitLabel, busyLabel, disabled = false }) {
	return <div className="wallet-dialog-actions"><button type="button" className="wallet-dialog-cancel" onClick={onClose} disabled={submitting}>Cancelar</button><button type="submit" className="wallet-dialog-submit" disabled={submitting || disabled}>{submitting ? busyLabel : submitLabel}</button></div>;
}

export function EntityFormMessage({ status }) {
	if (!status?.text) return null;
	return <p className={`wallet-form-message${status.error ? " wallet-form-message--error" : ""}`} role={status.error ? "alert" : "status"}>{status.text}</p>;
}

export function EntityEmpty({ title, description, actionLabel, onAction }) {
	return <div className="entity-empty"><span className="entity-empty-icon" aria-hidden="true">⌁</span><strong>{title}</strong>{description ? <p>{description}</p> : null}{actionLabel ? <button type="button" onClick={onAction}>{actionLabel}</button> : null}</div>;
}

export function EntityLoading({ label = "Carregando..." }) {
	return <div className="entity-loading" role="status"><span className="entity-spinner" aria-hidden="true" />{label}</div>;
}
