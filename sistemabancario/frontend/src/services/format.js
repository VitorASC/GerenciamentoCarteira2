export function stripDigits(s) {
	return String(s || "").replace(/\D/g, "");
}

export function formatCpf(value) {
	const digits = stripDigits(value);
	return digits.length === 11 ? digits.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4") : value || "—";
}

export function formatCnpj(value) {
	const digits = stripDigits(value);
	return digits.length === 14 ? digits.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, "$1.$2.$3/$4-$5") : value || "—";
}

export function formatDate(value) {
	if (!value) return "—";
	const date = new Date(String(value).length === 10 ? value + "T00:00:00" : value);
	return Number.isNaN(date.getTime()) ? "—" : date.toLocaleDateString("pt-BR");
}

export function formatDateTime(value) {
	if (!value) return "—";
	const date = new Date(value);
	return Number.isNaN(date.getTime()) ? "—" : date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export function esc(s) {
	if (s == null || s === "") {
		return "—";
	}
	return String(s);
}

const MONEY_FORMATTER = new Intl.NumberFormat("pt-BR", {
	style: "currency",
	currency: "BRL",
	minimumFractionDigits: 2,
	maximumFractionDigits: 2,
});

const NUMBER_FORMATTER = new Intl.NumberFormat("pt-BR", {
	minimumFractionDigits: 0,
	maximumFractionDigits: 8,
});

export function formatMoney(value) {
	const number = Number(value);
	if (!Number.isFinite(number)) return "—";
	const formatted = MONEY_FORMATTER.format(Math.abs(number));
	return number < 0 ? "-" + formatted : formatted;
}

export function formatNumber(value) {
	const number = Number(value);
	return Number.isFinite(number) ? NUMBER_FORMATTER.format(number) : "—";
}

export function formatPercent(value) {
	const number = Number(value);
	if (!Number.isFinite(number)) return "—";
	const sign = number > 0 ? "+" : "";
	return sign + number.toLocaleString("pt-BR", {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	}) + "%";
}

export function valueTone(value) {
	const number = Number(value);
	if (!Number.isFinite(number) || number === 0) return "neutral";
	return number > 0 ? "positive" : "negative";
}

export function parseJwtPayload(token) {
	if (!token) {
		return null;
	}
	try {
		const part = token.split(".")[1];
		const b64 = part.replace(/-/g, "+").replace(/_/g, "/");
		const json = decodeURIComponent(
			atob(b64)
				.split("")
				.map(function (c) {
					return "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2);
				})
				.join("")
		);
		return JSON.parse(json);
	} catch (_) {
		return null;
	}
}
