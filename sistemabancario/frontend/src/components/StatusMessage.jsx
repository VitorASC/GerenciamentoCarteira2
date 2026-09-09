export default function StatusMessage({ status }) {
	const text = status && status.text ? status.text : "";
	const isError = Boolean(status && status.error);
	const className = isError ? "status error" : "status";
	return (
		<p className={className} role="status">
			{text}
		</p>
	);
}
