// Sub-score breakdown card
export default function ScoreBreakdown({ parsingScore, completenessScore, keywordScore }) {
  const scores = [
    { label: 'Parsing_Score', value: parsingScore, highlight: false },
    { label: 'Formatting', value: completenessScore, highlight: false },
    { label: 'Keyword_Matching', value: keywordScore, highlight: keywordScore < 70 },
  ];

  return (
    <div className="bg-[var(--card-white)] rounded-2xl border border-stone-200/60 p-5">
      <div className="flex flex-col gap-0">
        {scores.map((score, i) => (
          <div
            key={score.label}
            className={`flex items-center justify-between py-3 font-mono text-xs tracking-wide ${
              i < scores.length - 1 ? 'border-b border-stone-100' : ''
            } ${
              score.highlight
                ? 'bg-orange-50/80 -mx-3 px-3 rounded-lg border border-orange-200/50 text-[var(--accent-orange)] font-semibold'
                : 'text-stone-600'
            }`}
          >
            <span className="uppercase tracking-[0.1em]">{score.label}</span>
            <span className="font-semibold">{score.value}/100</span>
          </div>
        ))}
      </div>
    </div>
  );
}
