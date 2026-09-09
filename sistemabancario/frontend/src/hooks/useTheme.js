import { useCallback, useEffect, useState } from "react";

const THEME_KEY = "finfef.theme";

function normalize(value) {
	return value === "light" || value === "dark" ? value : "dark";
}

function readInitial() {
	if (typeof window === "undefined") {
		return "dark";
	}
	return normalize(window.localStorage.getItem(THEME_KEY));
}

export function useTheme() {
	const [theme, setTheme] = useState(readInitial);

	useEffect(() => {
		document.documentElement.setAttribute("data-theme", theme);
		try {
			localStorage.setItem(THEME_KEY, theme);
		} catch (_) {
			/* ignore quota errors */
		}
	}, [theme]);

	const toggle = useCallback(() => {
		setTheme((cur) => (cur === "light" ? "dark" : "light"));
	}, []);

	return { theme, toggle };
}
