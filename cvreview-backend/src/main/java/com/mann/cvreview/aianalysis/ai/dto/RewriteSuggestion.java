package com.mann.cvreview.aianalysis.ai.dto;

// DTO for AI-generated bullet point rewrite suggestions
public record RewriteSuggestion(
        String original,   // Original sentence from CV
        String suggestion, // Suggested rewritten sentence
        String reason      // Reason for the rewrite
) {}