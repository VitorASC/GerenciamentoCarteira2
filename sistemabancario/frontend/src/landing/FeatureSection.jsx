import ScrollReveal from "./ScrollReveal";

export default function FeatureSection({
	description,
	eyebrow,
	id,
	points,
	reversed = false,
	title,
	visual,
}) {
	return (
		<section id={id} className={`landing-section feature-section${reversed ? " feature-section--reversed" : ""}`}>
			<div className="landing-container feature-section__grid">
				<ScrollReveal className="feature-section__copy" direction={reversed ? "right" : "left"}>
					<p className="landing-eyebrow">{eyebrow}</p>
					<h2>{title}</h2>
					<p className="feature-section__description">{description}</p>
					<ul className="feature-points">
						{points.map((point) => <li key={point}><span aria-hidden="true">✓</span>{point}</li>)}
					</ul>
				</ScrollReveal>
				<ScrollReveal className="feature-section__visual" direction={reversed ? "left" : "right"} delay={100}>
					{visual}
				</ScrollReveal>
			</div>
		</section>
	);
}
