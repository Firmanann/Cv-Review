'use client';

import { useState, useEffect, useCallback } from 'react';
import CvReviewLayout from './components/CvReviewLayout';
import UploadScreen from './components/screens/UploadScreen';
import ReviewScreen from './components/screens/ReviewScreen';
import ResultScreen from './components/screens/ResultScreen';
import { analyzeCv } from './services/cvReviewService';
import { AnalysisResponse } from './types/cvReview.types';

// Entry point modul CV Review
// Manages state, API calls, and screen navigation (Upload → Process → Result)
export default function CvReviewModule() {
  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [progress, setProgress] = useState<number>(0);
  const [result, setResult] = useState<AnalysisResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  // Simulate progress animation while API is loading
  useEffect(() => {
    if (step !== 2 || !isLoading) return;

    setProgress(0);
    let current = 0;

    const interval = setInterval(() => {
      // Slow down as it approaches 90% (never reaches 100% until API responds)
      const increment = current < 30 ? 3 : current < 60 ? 2 : current < 85 ? 0.5 : 0;
      current = Math.min(current + increment, 90);
      setProgress(current);

      if (current >= 90) clearInterval(interval);
    }, 150);

    return () => clearInterval(interval);
  }, [step, isLoading]);

  // Handle form submission from Screen 1
  const handleSubmit = useCallback(async (file: File, jobDescription: string) => {
    setIsLoading(true);
    setError(null);
    setStep(2); // Switch to processing screen immediately

    try {
      const response = await analyzeCv(file, jobDescription);
      setResult(response);

      // Animate progress to 100% before showing results
      setProgress(100);
      await new Promise((resolve) => setTimeout(resolve, 800));

      setStep(3);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Something went wrong. Please try again.';
      setError(errorMessage);
      setStep(1); // Go back to upload on error
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Reset everything to start over
  const handleReset = useCallback(() => {
    setStep(1);
    setProgress(0);
    setResult(null);
    setError(null);
    setIsLoading(false);
  }, []);

  return (
    <CvReviewLayout>
      {/* Error banner */}
      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl text-sm text-red-700 flex items-center justify-between">
          <span>{error}</span>
          <button
            onClick={() => setError(null)}
            className="text-red-400 hover:text-red-600 text-xs font-mono"
          >
            DISMISS
          </button>
        </div>
      )}

      {step === 1 && (
        <UploadScreen onSubmit={handleSubmit} isLoading={isLoading} />
      )}
      {step === 2 && (
        <ReviewScreen progress={progress} />
      )}
      {step === 3 && result && (
        <ResultScreen result={result} onReset={handleReset} />
      )}
    </CvReviewLayout>
  );
}
