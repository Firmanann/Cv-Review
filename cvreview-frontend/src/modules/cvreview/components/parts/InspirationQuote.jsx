// Italic quote with green left border
export default function InspirationQuote({ text = "Think of this as the calm before the work:\none useful handoff, no busywork." }) {
  return (
    <blockquote className="border-l-2 border-[var(--accent-green)] pl-5 mt-8">
      <p className="italic text-sm text-stone-600 leading-6 whitespace-pre-line">
        {text}
      </p>
    </blockquote>
  );
}
