// Step indicator: "01 ---- RECEIVE"
export default function StepIndicator({ step, label }) {
  return (
    <div className="flex items-center gap-3 mb-8">
      <span className="text-[var(--accent-orange)] font-mono text-sm font-semibold">
        {step}
      </span>
      <span className="text-stone-300 tracking-[0.3em] text-xs select-none">
        – – – – – –
      </span>
      <span className="font-mono text-xs tracking-[0.2em] uppercase text-stone-600 font-medium">
        {label}
      </span>
    </div>
  );
}
