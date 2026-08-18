package com.mann.cvreview.aianalysis.ai.dto;

// DTO representing a detected issue with category, description, and severity
public record Issue(
        String category,    // e.g. "layout", "font", "contact", "integrity"
        String description, // Human-readable issue description
        String severity     // e.g. "high", "medium", "low"
) {}