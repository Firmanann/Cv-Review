package com.mann.cvreview.aianalysis.scoring.service;

import org.springframework.stereotype.Service;

import com.mann.cvreview.util.config.ParsingConfig;

@Service
public class ScoringService {

    private static final int CRITICAL_PARSING_THRESHOLD = 40;

    private final ParsingConfig config;

    public ScoringService(ParsingConfig config) {
        this.config = config;
    }

    public int calculateTotalScore(int parsingScore, int keywordScore, int completenessScore) {
        // Load scoring weights from application.properties
        double weightParsing = config.getWeightParsing();           // e.g. 0.4
        double weightKeyword = config.getWeightKeyword();           // e.g. 0.4
        double weightCompleteness = config.getWeightCompleteness(); // e.g. 0.2

        // Calculate weighted total score
        double total = (parsingScore * weightParsing)
                + (keywordScore * weightKeyword)
                + (completenessScore * weightCompleteness);

        // Round to nearest integer
        return (int) Math.round(total);
    }

    public boolean isCriticalParsingScore(int parsingScore) {
        return parsingScore < CRITICAL_PARSING_THRESHOLD;
    }

    public String buildCriticalWarning(int parsingScore) {
        if (isCriticalParsingScore(parsingScore)) {
            return "Format dokumen CV berisiko tinggi gagal dibaca oleh sistem ATS. Perbaiki masalah layout dan struktur terlebih dahulu agar CV dapat lolos seleksi otomatis.";
        }
        return null;
    }
}