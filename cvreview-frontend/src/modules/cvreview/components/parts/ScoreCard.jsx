// Overall ATS score card (dark green)
export default function ScoreCard({ totalScore }) {
  const isPassing = totalScore >= 70;

  return (
    <div className="bg-[var(--accent-green)] rounded-2xl p-6 text-white">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <span className="font-mono text-[10px] tracking-[0.15em] uppercase text-white/60">
          Overall_ATS_Score
        </span>
        <span
          className={`px-3 py-1 rounded-full text-[10px] font-mono tracking-wider uppercase font-medium ${
            isPassing
              ? 'bg-orange-100 text-[var(--accent-orange)]'
              : 'bg-red-100 text-red-600'
          }`}
        >
          {isPassing ? 'Passing Threshold' : 'Below Threshold'}
        </span>
      </div>

      {/* Score */}
      <div className="font-serif text-6xl font-bold tracking-tight">
        {totalScore}<span className="text-3xl text-white/40 font-sans">/100</span>
      </div>
    </div>
  );
}
