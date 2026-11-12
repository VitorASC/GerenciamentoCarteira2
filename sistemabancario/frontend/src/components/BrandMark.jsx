export default function BrandMark({ className = "" }) {
	return (
		<div className={("brand-mark " + className).trim()} aria-label="GestãoAtiva">
			<span className="brand-symbol" aria-hidden="true">
				<svg viewBox="0 0 36 36">
					<path d="M8 24.5 14.2 18l4.7 3.8L28 11.5" />
					<path d="M23 11.5h5v5" />
				</svg>
			</span>
			<span>GestãoAtiva</span>
		</div>
	);
}
