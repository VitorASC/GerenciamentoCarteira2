import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { clearToken, getToken, setToken } from "../services/auth";
import { parseJwtPayload } from "../services/format";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
	const [token, setTokenState] = useState(() => getToken());

	const payload = useMemo(() => parseJwtPayload(token), [token]);

	const login = useCallback((accessToken) => {
		setToken(accessToken || "");
		setTokenState(accessToken || "");
	}, []);

	const logout = useCallback(() => {
		clearToken();
		setTokenState("");
	}, []);

	useEffect(() => {
		function syncFromStorage() {
			setTokenState(getToken());
		}
		window.addEventListener("storage", syncFromStorage);
		return () => window.removeEventListener("storage", syncFromStorage);
	}, []);

	const value = useMemo(
		() => ({
			token,
			payload,
			usuarioId: payload && payload.sub ? String(payload.sub) : "",
			isAuthenticated: Boolean(token),
			login,
			logout,
		}),
		[token, payload, login, logout]
	);

	return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
	const ctx = useContext(AuthContext);
	if (!ctx) {
		throw new Error("useAuth must be used inside <AuthProvider>");
	}
	return ctx;
}
