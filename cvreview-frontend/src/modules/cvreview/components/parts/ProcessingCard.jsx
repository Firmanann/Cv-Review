'use client';

import { useState, useEffect } from 'react';
import { CheckCircle2, Zap, Circle } from 'lucide-react';

// Individual process step row
function ProcessStep({ icon, label, status }) {
  const statusStyles = {
    done: 'bg-green-50 border-green-200/60 text-green-700',
    running: 'bg-[var(--card-white)] border-stone-200/60 text-stone-700',
    pending: 'bg-[var(--card-white)] border-stone-100 text-stone-300',
  };

  const statusLabels = {
    done: null,
    running: <span className="text-[10px] font-mono tracking-wider text-[var(--accent-orange)]">RUNNING</span>,
    pending: <span className="text-[10px] font-mono tracking-wider text-stone-300">PENDING</span>,
  };

  const icons = {
    done: <CheckCircle2 className="w-4 h-4 text-green-500" />,
    running: <Zap className="w-4 h-4 text-[var(--accent-orange)]" />,
    pending: <Circle className="w-4 h-4 text-stone-200" />,
  };

  return (
    <div className={`flex items-center justify-between px-4 py-3.5 rounded-xl border transition-all ${statusStyles[status]}`}>
      <div className="flex items-center gap-3">
        {icons[status]}
        <span className="text-xs font-mono tracking-wide">{label}</span>
      </div>
      {statusLabels[status]}
    </div>
  );
}

export default function ProcessingCard({ progress = 0 }) {
  // Determine step statuses based on progress
  const getStepStatus = (stepThreshold) => {
    if (progress >= stepThreshold + 33) return 'done';
    if (progress >= stepThreshold) return 'running';
    return 'pending';
  };

  return (
    <div>
      {/* Section label */}
      <div className="flex items-center justify-between mb-2">
        <span className="font-mono text-[10px] tracking-[0.2em] uppercase text-stone-500">
          Processing Status
        </span>
      </div>
      <div className="flex items-center justify-between mb-5">
        <p className="text-sm text-stone-600">Real-time analysis in progress.</p>
        <CheckCircle2 className="w-4 h-4 text-stone-300" />
      </div>

      {/* Card */}
      <div className="bg-[var(--card-white)] rounded-2xl border border-stone-200/60 shadow-sm p-5">
        {/* Header */}
        <div className="flex items-center justify-between mb-5">
          <span className="font-mono text-[10px] tracking-[0.15em] uppercase text-stone-500">
            ATS_CV_REVIEWER_V1.0
          </span>
          <div className="flex items-center gap-2">
            <span className="font-mono text-[10px] tracking-[0.15em] uppercase text-[var(--accent-orange)]">
              Processing
            </span>
            <span className="w-2 h-2 rounded-full bg-[var(--accent-orange)] animate-pulse" />
          </div>
        </div>

        {/* Progress */}
        <div className="mb-6">
          <div className="flex items-baseline gap-1 mb-2">
            <span className="font-serif text-4xl font-bold text-stone-800">{Math.round(progress)}%</span>
            <span className="text-[10px] font-mono tracking-[0.15em] uppercase text-stone-400 ml-1">
              Complete
            </span>
          </div>
          <div className="w-full h-2 bg-stone-100 rounded-full overflow-hidden">
            <div
              className="h-full bg-[var(--accent-orange)] rounded-full transition-all duration-500 ease-out"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>

        {/* Steps */}
        <div className="flex flex-col gap-2.5">
          <ProcessStep
            label="Extracting Text & Metadata"
            status={getStepStatus(0)}
          />
          <ProcessStep
            label="Semantic Analysis & Structuring"
            status={getStepStatus(33)}
          />
          <ProcessStep
            label="Generating Insights Report"
            status={getStepStatus(66)}
          />
        </div>
      </div>
    </div>
  );
}
