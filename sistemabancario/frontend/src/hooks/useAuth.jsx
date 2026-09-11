import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { clearToken, getToken, SESSION_EXPIRED_EVENT, setToken } from "../services/auth";
import { parseJwtPayload } from "../services/format";

const AuthContext = createContext(null);
const SESSION_EXPIRED_NOTICE = {
	text: "Sua sessão expirou. Faça login novamente.",
	error: true,
};

function getInitialAuthState() {
	const token = getToken();
	if (!token) return { token: "", sessionNotice: null };
	const payload = parseJwtPayload(token);
	const expiresAt = Number(payload?.exp) * 1000;
	if (!payload || !Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
		return { token: "", sessionNotice: SESSION_EXPIRED_NOTICE };
	}
	return { token, sessionNotice: null };
}

export function AuthProvider({ children }) {
	const [authState, setAuthState] = useState(getInitialAuthState);
	const { token, sessionNotice } = authState;

	const payload = useMemo(() => parseJwtPayload(token), [token]);

	const login = useCallback((accessToken) => {
		setToken(accessToken || "");
		setAuthState({ token: accessToken || "", sessionNotice: null });
	}, []);

	const logout = useCallback(() => {
		clearToken();
		setAuthState({ token: "", sessionNotice: null });
	}, []);

	const clearSessionNotice = useCallback(() => {
		setAuthState((current) => ({ ...current, sessionNotice: null }));
	}, []);

	useEffect(() => {
		if (!token && sessionNotice) clearToken();
	}, [token, sessionNotice]);

	useEffect(() => {
		function syncFromStorage() {
			setAuthState(getInitialAuthState());
		}
		function expireSession() {
			clearToken();
			setAuthState({ token: "", sessionNotice: SESSION_EXPIRED_NOTICE });
		}
		window.addEventListener("storage", syncFromStorage);
		window.addEventListener(SESSION_EXPIRED_EVENT, expireSession);
		return () => {
			window.removeEventListener("storage", syncFromStorage);
			window.removeEventListener(SESSION_EXPIRED_EVENT, expireSession);
		};
	}, []);

	const value = useMemo(
		() => ({
			token,
			payload,
			usuarioId: payload && payload.sub ? String(payload.sub) : "",
			isAuthenticated: Boolean(token),
			sessionNotice,
			login,
			logout,
			clearSessionNotice,
		}),
		[token, payload, sessionNotice, login, logout, clearSessionNotice]
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
