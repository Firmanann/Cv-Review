package com.mann.cvreview.aianalysis.extraction.dto;

import java.util.List;

// DTO holding CV structural layout metadata for ATS parsing evaluation
public record StructuralInfo(

        boolean isMultiColumn,
        boolean hasTable,
        boolean hasTextBox,
        boolean hasImage,
        List<String> fontsUsed,
        List<String> detectedHeadings,
        boolean contactInfoInHeaderFooter,
        int extractedTextLength
) {}