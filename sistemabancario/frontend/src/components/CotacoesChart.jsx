import {
	CategoryScale,
	Chart as ChartJS,
	Filler,
	Legend,
	LineElement,
	LinearScale,
	PointElement,
	Tooltip,
} from "chart.js";
import { useMemo } from "react";
import { Line } from "react-chartjs-2";

ChartJS.register(
	CategoryScale,
	LinearScale,
	PointElement,
	LineElement,
	Filler,
	Legend,
	Tooltip
);

export default function CotacoesChart({ points }) {
	const data = useMemo(() => {
		const rows = Array.isArray(points) ? points : [];
		return {
			labels: rows.map((r) => r.dataHora),
			datasets: [
				{
					label: "Cotação",
					data: rows.map((r) => Number(r.valor)),
					borderColor: "#60a5fa",
					backgroundColor: "rgba(96,165,250,0.15)",
					fill: true,
					tension: 0.2,
					pointRadius: 0,
				},
			],
		};
	}, [points]);

	const options = useMemo(
		() => ({
			responsive: true,
			maintainAspectRatio: false,
			scales: {
				x: {
					ticks: { maxRotation: 45, minRotation: 0, color: "#9aa5b1" },
					grid: { color: "#2d3848" },
				},
				y: {
					ticks: { color: "#9aa5b1" },
					grid: { color: "#2d3848" },
				},
			},
			plugins: {
				legend: { labels: { color: "#e8eaed" } },
			},
		}),
		[]
	);

	return (
		<div className="chart-wrap">
			<Line data={data} options={options} />
		</div>
	);
}
