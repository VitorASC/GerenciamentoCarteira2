export default function JsonOutput({ data, compact }) {
	if (data === undefined || data === null) {
		return null;
	}
	const text = typeof data === "string" ? data : JSON.stringify(data, null, 2);
	const className = compact ? "json-out json-out--compact" : "json-out";
	return <pre className={className}>{text}</pre>;
}
