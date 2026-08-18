// Bullet point optimizer card (original vs suggestion)
export default function OptimizerCard({ rewriteSuggestions }) {
  if (!rewriteSuggestions || rewriteSuggestions.length === 0) return null;

  return (
    <div className="bg-[var(--card-white)] rounded-2xl border border-stone-200/60 p-5">
      <span className="font-mono text-[10px] tracking-[0.15em] uppercase text-stone-500 block mb-4">
        Bullet_Point_Optimizer
      </span>

      <div className="flex flex-col gap-4">
        {rewriteSuggestions.map((item, i) => (
          <div key={i} className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {/* Original */}
            <div className="bg-[var(--bg-cream)]/50 rounded-xl p-4">
              <span className="font-mono text-[9px] tracking-[0.15em] uppercase text-[var(--accent-green)] block mb-2 font-semibold">
                Original
              </span>
              <p className="text-sm text-stone-600 leading-relaxed">
                {item.original}
              </p>
            </div>

            {/* Suggestion */}
            <div className="bg-orange-50/50 rounded-xl p-4">
              <span className="font-mono text-[9px] tracking-[0.15em] uppercase text-[var(--accent-orange)] block mb-2 font-semibold">
                Suggestion
              </span>
              <p className="text-sm text-stone-800 leading-relaxed font-medium">
                {item.suggestion}
              </p>
              {item.reason && (
                <p className="text-xs text-stone-400 mt-2 italic">
                  Reason: {item.reason}
                </p>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
