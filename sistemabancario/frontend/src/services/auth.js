export const TOKEN_KEY = "sistemabancario.jwt";

export function getToken() {
	return sessionStorage.getItem(TOKEN_KEY) || "";
}

export function setToken(token) {
	sessionStorage.setItem(TOKEN_KEY, token || "");
}

export function clearToken() {
	sessionStorage.removeItem(TOKEN_KEY);
}

export function authHeaders() {
	const t = getToken();
	const h = { Accept: "application/json" };
	if (t) {
		h.Authorization = "Bearer " + t;
	}
	return h;
}
