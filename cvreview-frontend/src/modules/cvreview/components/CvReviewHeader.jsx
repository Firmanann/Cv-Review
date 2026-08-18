import { CheckCircle2 } from 'lucide-react';

export default function CvReviewHeader() {
  return (
    <header className="flex justify-between items-center text-[11px] font-mono tracking-widest uppercase text-stone-600">
      <span>RELAY DESK</span>
      <CheckCircle2 className="w-4 h-4 text-stone-800" />
    </header>
  );
}
