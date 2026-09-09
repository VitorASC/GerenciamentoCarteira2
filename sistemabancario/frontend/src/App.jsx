import { useAuth } from "./hooks/useAuth";
import { useTheme } from "./hooks/useTheme";
import AppShell from "./layout/AppShell";
import LoginScreen from "./screens/LoginScreen";

export default function App() {
	const { isAuthenticated } = useAuth();
	const { toggle } = useTheme();

	if (isAuthenticated) {
		return <AppShell onToggleTheme={toggle} />;
	}
	return <LoginScreen onToggleTheme={toggle} />;
}
