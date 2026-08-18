package com.mann.cvreview.aianalysis.orchestration.dto;

import java.util.List;

import com.mann.cvreview.aianalysis.ai.dto.Issue;
import com.mann.cvreview.aianalysis.ai.dto.RewriteSuggestion;

// Main response DTO returning all analysis results to the client
public record AnalysisResponse(

        int totalScore,
        int parsingScore,
        int keywordScore,
        int completenessScore,
        boolean isCritical,
        String criticalWarning,
        List<Issue> issues,
        List<RewriteSuggestion> rewriteSuggestions
) {}