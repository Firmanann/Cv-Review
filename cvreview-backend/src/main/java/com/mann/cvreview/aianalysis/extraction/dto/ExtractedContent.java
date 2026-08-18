package com.mann.cvreview.aianalysis.extraction.dto;

// DTO wrapping raw extracted text and structural layout info
public record ExtractedContent(
        String rawText,
        StructuralInfo structuralInfo
) {}