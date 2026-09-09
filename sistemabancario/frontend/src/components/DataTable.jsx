import { esc } from "../services/format";

export default function DataTable({ columns, rows, rowMapper, getRowKey }) {
	if (!rows) {
		return null;
	}
	return (
		<div className="table-wrap">
			<table>
				<thead>
					<tr>
						{columns.map((c) => (
							<th key={c}>{c}</th>
						))}
					</tr>
				</thead>
				<tbody>
					{rows.map((row, idx) => {
						const cells = rowMapper(row);
						const key = getRowKey ? getRowKey(row, idx) : idx;
						return (
							<tr key={key}>
								{cells.map((cell, i) => (
									<td key={i}>{cell == null || cell === "" ? esc(cell) : cell}</td>
								))}
							</tr>
						);
					})}
				</tbody>
			</table>
		</div>
	);
}
