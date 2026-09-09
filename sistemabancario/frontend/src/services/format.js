export function stripDigits(s) {
	return String(s || "").replace(/\D/g, "");
}

export function esc(s) {
	if (s == null || s === "") {
		return "—";
	}
	return String(s);
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
