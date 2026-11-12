import { useEffect, useRef, useState } from "react";

export default function ScrollReveal({
	as: Element = "div",
	children,
	className = "",
	delay = 0,
	direction = "up",
}) {
	const elementRef = useRef(null);
	const [visible, setVisible] = useState(false);

	useEffect(() => {
		const element = elementRef.current;
		if (!element) return undefined;

		const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
		if (reducedMotion || !("IntersectionObserver" in window)) {
			setVisible(true);
			return undefined;
		}

		const observer = new IntersectionObserver(
			(entries) => {
				if (entries.some((entry) => entry.isIntersecting)) {
					setVisible(true);
					observer.unobserve(element);
				}
			},
			{ rootMargin: "0px 0px -10%", threshold: 0.12 }
		);

		observer.observe(element);
		return () => observer.disconnect();
	}, []);

	return (
		<Element
			ref={elementRef}
			className={`scroll-reveal scroll-reveal--${direction}${visible ? " is-visible" : ""}${className ? ` ${className}` : ""}`}
			style={{ "--reveal-delay": `${delay}ms` }}
		>
			{children}
		</Element>
	);
}
