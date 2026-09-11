import { CategoryScale, Chart as ChartJS, Filler, Legend, LineElement, LinearScale, PointElement, Tooltip } from "chart.js";
import { useMemo } from "react";
import { Line } from "react-chartjs-2";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Legend, Tooltip);

export default function CotacoesChart({ points }) {
	const data = useMemo(() => {
		const rows = Array.isArray(points) ? points : [];
		return {
			labels: rows.map((row) => new Date(row.dataHora).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" })),
			datasets: [{ label: "Cotação (R$)", data: rows.map((row) => Number(row.valor)), borderColor: "#2ec98f", backgroundColor: "rgba(46,201,143,0.12)", fill: true, tension: 0.25, pointRadius: rows.length < 20 ? 2 : 0, pointHoverRadius: 4 }],
		};
	}, [points]);

	const options = {
		responsive: true,
		maintainAspectRatio: false,
		interaction: { mode: "index", intersect: false },
		scales: {
			x: { ticks: { maxRotation: 0, maxTicksLimit: 7, color: "#91a39b" }, grid: { color: "rgba(145,163,155,0.14)" } },
			y: { ticks: { color: "#91a39b", callback: (value) => `R$ ${Number(value).toLocaleString("pt-BR")}` }, grid: { color: "rgba(145,163,155,0.14)" } },
		},
		plugins: { legend: { display: false }, tooltip: { callbacks: { label: (context) => ` R$ ${Number(context.parsed.y).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` } } },
	};

	return <div className="chart-wrap"><Line data={data} options={options} /></div>;
}
