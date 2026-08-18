package com.mann.cvreview.aianalysis.ai.dto;

import java.util.List;

// DTO holding semantic and keyword analysis results from AI
public record AiAnalysisResult(
        int keywordScore,
        int completenessScore,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        List<Issue> keywordIssues,
        List<RewriteSuggestion> rewriteSuggestions
) {}