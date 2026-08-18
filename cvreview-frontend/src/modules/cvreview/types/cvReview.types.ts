// Matches backend: com.mann.cvreview.aianalysis.dto.Issue
export interface Issue {
  category: string;     // e.g. "layout", "font", "contact", "integrity"
  description: string;  // Human-readable issue description
  severity: string;     // e.g. "high", "medium", "low"
}

// Matches backend: com.mann.cvreview.aianalysis.dto.RewriteSuggestion
export interface RewriteSuggestion {
  original: string;     // Original sentence from CV
  suggestion: string;   // Suggested rewritten sentence
  reason: string;       // Reason for the rewrite
}

// Matches backend: com.mann.cvreview.orchestration.dto.AnalysisResponse
export interface AnalysisResponse {
  totalScore: number;
  parsingScore: number;
  keywordScore: number;
  completenessScore: number;
  isCritical?: boolean;
  criticalWarning?: string | null;
  issues: Issue[];
  rewriteSuggestions: RewriteSuggestion[];
}

// Matches backend: GlobalExceptionHandler.ErrorResponse
export interface ErrorResponse {
  message: string;
}
