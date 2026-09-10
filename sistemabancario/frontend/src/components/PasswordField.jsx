import { useState } from "react";

export default function PasswordField({ id, value, onChange, autoComplete, minLength }) {
	const [visible, setVisible] = useState(false);

	return (
		<label className="auth-field" htmlFor={id}>
			<span>Senha</span>
			<div className="password-control">
				<input
					id={id}
					type={visible ? "text" : "password"}
					name="senha"
					required
					minLength={minLength}
					autoComplete={autoComplete}
					value={value}
					onChange={onChange}
				/>
				<button
					type="button"
					className="password-toggle"
					aria-label={visible ? "Ocultar senha" : "Mostrar senha"}
					aria-pressed={visible}
					onClick={() => setVisible((current) => !current)}
				>
					<svg viewBox="0 0 24 24" aria-hidden="true">
						<path d="M2.6 12s3.4-5.5 9.4-5.5 9.4 5.5 9.4 5.5-3.4 5.5-9.4 5.5S2.6 12 2.6 12Z" />
						<circle cx="12" cy="12" r="2.5" />
						{visible && <path d="m4 4 16 16" />}
					</svg>
				</button>
			</div>
		</label>
	);
}
