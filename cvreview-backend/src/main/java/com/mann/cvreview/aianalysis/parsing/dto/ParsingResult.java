package com.mann.cvreview.aianalysis.parsing.dto;

import java.util.List;

import com.mann.cvreview.aianalysis.ai.dto.Issue;

// DTO holding parsing evaluation result (score + list of layout issues)
public record ParsingResult(
        int parsingScore,
        List<Issue> issues
) {}