import ScoreCard from '../parts/ScoreCard';
import ScoreBreakdown from '../parts/ScoreBreakdown';
import IssueCard from '../parts/IssueCard';
import OptimizerCard from '../parts/OptimizerCard';
import { RotateCcw, AlertTriangle } from 'lucide-react';

export default function ResultScreen({ result, onReset }) {
  return (
    <div className="flex flex-col gap-5 max-w-4xl mx-auto">
      {/* Critical ATS Warning Banner */}
      {result?.isCritical && result?.criticalWarning && (
        <div className="flex items-start gap-3 p-4 bg-red-500/10 border border-red-500/30 rounded-2xl text-red-400">
          <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5 text-red-400" />
          <div className="flex flex-col gap-1">
            <span className="text-xs font-mono uppercase tracking-wider font-semibold text-red-300">
              Peringatan Format ATS
            </span>
            <p className="text-sm text-red-200/90 leading-relaxed">
              {result.criticalWarning}
            </p>
          </div>
        </div>
      )}

      {/* Top row: score + breakdown */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        <ScoreCard totalScore={result.totalScore} />
        <ScoreBreakdown
          parsingScore={result.parsingScore}
          completenessScore={result.completenessScore}
          keywordScore={result.keywordScore}
        />
      </div>

      {/* Issues */}
      <IssueCard issues={result.issues} />

      {/* Optimizer */}
      <OptimizerCard rewriteSuggestions={result.rewriteSuggestions} />

      {/* Reset button */}
      <div className="flex justify-end pt-2">
        <button
          onClick={onReset}
          className="flex items-center gap-2 px-6 py-3 bg-[var(--accent-green)] text-white rounded-full text-xs font-mono uppercase tracking-wider hover:brightness-110 active:scale-[0.98] transition-all"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          Review Another CV
        </button>
      </div>
    </div>
  );
}
