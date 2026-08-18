package com.mann.cvreview.aianalysis.parsing.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.mann.cvreview.aianalysis.ai.dto.Issue;
import com.mann.cvreview.aianalysis.extraction.dto.StructuralInfo;
import com.mann.cvreview.aianalysis.parsing.dto.ParsingResult;
import com.mann.cvreview.util.config.ParsingConfig;

@Service
public class ParsingService {

    private final ParsingConfig config;

    public ParsingService(ParsingConfig config) {
        this.config = config;
    }

    public ParsingResult evaluate(StructuralInfo info, String rawText) {

        List<Issue> issues = new ArrayList<>();

        // 1. Check layout issues (multi-column, table, textbox, image)
        if (info.isMultiColumn()) {
            issues.add(new Issue("layout", "CV uses multi-column layout which risks ATS parsing failure", "high"));
        }
        if (info.hasTable()) {
            issues.add(new Issue("layout", "Tables detected which may cause ATS to read text out of order", "high"));
        }
        if (info.hasTextBox()) {
            issues.add(new Issue("layout", "Text boxes detected, content inside is often ignored by ATS parsers", "high"));
        }
        if (info.hasImage()) {
            issues.add(new Issue("layout", "Images or visual graphics detected which cannot be read by ATS", "medium"));
        }

        // 2. Validate fonts against ATS-safe whitelist
        if (config.getFontWhitelist() != null && !config.getFontWhitelist().isEmpty() && info.fontsUsed() != null && !info.fontsUsed().isEmpty()) {
            List<String> whitelistUpper = config.getFontWhitelist().stream().map(String::toUpperCase).toList();
            boolean hasNonStandardFont = info.fontsUsed().stream()
                    .anyMatch(f -> whitelistUpper.stream().noneMatch(w -> f.toUpperCase().contains(w) || w.contains(f.toUpperCase())));
            if (hasNonStandardFont) {
                issues.add(new Issue("font", "Use ATS-safe standard fonts like Arial, Calibri, Times New Roman, or Georgia", "medium"));
            }
        }

        // 3. Check contact info placement in header/footer
        if (info.contactInfoInHeaderFooter()) {
            issues.add(new Issue("contact", "Contact info is in header/footer area, may be lost during ATS parsing", "high"));
        }

        // 4. Check text integrity (minimum text length)
        if (info.extractedTextLength() < config.getMinTextLength()) {
            issues.add(new Issue("integrity", "Extracted text is too short, indicates a scanned or image-based CV", "high"));
        }

        // 5. Check section heading structure (Harvard principle & ATS standard)
        if (config.getHeadingWhitelist() != null && !config.getHeadingWhitelist().isEmpty()) {
            List<String> detected = info.detectedHeadings() != null ? info.detectedHeadings() : List.of();
            boolean hasRecognizedSection = config.getHeadingWhitelist().stream()
                    .anyMatch(req -> detected.stream().anyMatch(d -> d.contains(req.toUpperCase())));

            if (!hasRecognizedSection && info.extractedTextLength() >= config.getMinTextLength()) {
                issues.add(new Issue("structure", "Standard section headings (e.g. Experience, Education, Skills) are not detected; ATS parsers may fail to categorize your CV sections", "high"));
            }
        }

        // Calculate final parsing score based on detected issues
        int score = calculateParsingScore(issues);

        return new ParsingResult(score, issues);
    }

    // Calculate score by deducting penalty points per issue severity
    private int calculateParsingScore(List<Issue> issues) {
        int score = 100;
        for (Issue issue : issues) {
            score -= switch (issue.severity().toLowerCase()) {
                case "high" -> 20;   // High severity: -20 points
                case "medium" -> 10; // Medium severity: -10 points
                default -> 5;        // Low severity: -5 points
            };
        }
        return Math.max(score, 0); // Ensure score does not go below 0
    }
}