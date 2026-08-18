// Large serif heading + subtitle text
export default function HeroHeading({ title, subtitle }) {
  return (
    <div className="mb-6">
      <h1 className="font-serif text-5xl md:text-6xl font-bold leading-[1.1] tracking-tight mb-6 text-[var(--text-dark)]">
        {title}
      </h1>
      {subtitle && (
        <p className="text-stone-600 text-sm md:text-base leading-relaxed max-w-md">
          {subtitle}
        </p>
      )}
    </div>
  );
}
