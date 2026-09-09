import { authHeaders } from "./auth";

export async function api(method, path, body) {
	const opts = {
		method,
		headers: { ...authHeaders() },
	};
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
		throw new Error(msg);
	}
	return data;
}
