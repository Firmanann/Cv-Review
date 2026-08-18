// Critical issues card
export default function IssueCard({ issues }) {
  if (!issues || issues.length === 0) return null;

  const severityStyles = {
    high: 'bg-red-100 text-red-700',
    medium: 'bg-orange-100 text-[var(--accent-orange)]',
    low: 'bg-yellow-50 text-yellow-700',
  };

  return (
    <div className="bg-[var(--card-white)] rounded-2xl border border-stone-200/60 p-5">
      <span className="font-mono text-[10px] tracking-[0.15em] uppercase text-stone-500 block mb-4">
        Critical_Issues_Found
      </span>

      <div className="flex flex-col gap-0">
        {issues.map((issue, i) => (
          <div
            key={i}
            className={`flex items-start gap-3 py-3 ${
              i < issues.length - 1 ? 'border-b border-dashed border-stone-200' : ''
            }`}
          >
            <span
              className={`shrink-0 px-2.5 py-1 rounded text-[9px] font-mono tracking-wider uppercase font-semibold ${
                severityStyles[issue.severity] || severityStyles.medium
              }`}
            >
              {issue.severity}_Severity
            </span>
            <p className="text-sm text-stone-600 leading-relaxed">
              {issue.description}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}
