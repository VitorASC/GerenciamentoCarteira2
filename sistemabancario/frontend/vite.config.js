import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const SPRING_BOOT_URL = "http://localhost:8080";

const apiPaths = ["/auth", "/usuarios", "/corretoras", "/acoes", "/carteiras"];

const proxy = apiPaths.reduce((acc, path) => {
	acc[path] = { target: SPRING_BOOT_URL, changeOrigin: true };
	return acc;
}, {});

export default defineConfig({
	plugins: [react()],
	server: {
		port: 5173,
		proxy,
	},
	build: {
		outDir: "../src/main/resources/static",
		emptyOutDir: true,
		sourcemap: false,
	},
});
