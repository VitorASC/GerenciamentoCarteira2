import { authHeaders, SESSION_EXPIRED_EVENT } from "./auth";

export async function api(method, path, body) {
	const opts = {
		method,
		headers: { ...authHeaders() },
	};
	const isAuthenticatedRequest = Boolean(opts.headers.Authorization);
	if (body !== undefined && body !== null && method !== "GET" && method !== "HEAD") {
		opts.headers["Content-Type"] = "application/json";
		opts.body = JSON.stringify(body);
	}
	const res = await fetch(path, opts);
	if (res.status === 204) {
		return null;
	}
	const text = await res.text();
	let data = null;
	try {
		data = text ? JSON.parse(text) : null;
	} catch (_) {
		data = text;
	}
	if (!res.ok) {
		const msg =
			data && typeof data === "object" && data.mensagem != null
				? data.mensagem
				: "HTTP " + res.status;
		if (res.status === 401 && isAuthenticatedRequest) {
			window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
		}
		const error = new Error(msg);
		error.status = res.status;
		throw error;
	}
	return data;
}
