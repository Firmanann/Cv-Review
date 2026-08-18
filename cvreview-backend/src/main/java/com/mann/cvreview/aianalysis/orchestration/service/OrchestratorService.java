package com.mann.cvreview.aianalysis.orchestration.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mann.cvreview.aianalysis.ai.dto.AiAnalysisResult;
import com.mann.cvreview.aianalysis.ai.dto.Issue;
import com.mann.cvreview.aianalysis.ai.service.AiAnalysisService;
import com.mann.cvreview.aianalysis.extraction.dto.ExtractedContent;
import com.mann.cvreview.aianalysis.extraction.service.TextExtractionService;
import com.mann.cvreview.aianalysis.filevalidation.service.FileValidationService;
import com.mann.cvreview.aianalysis.orchestration.dto.AnalysisResponse;
import com.mann.cvreview.aianalysis.parsing.dto.ParsingResult;
import com.mann.cvreview.aianalysis.parsing.service.ParsingService;
import com.mann.cvreview.aianalysis.scoring.service.ScoringService;
import com.mann.cvreview.ratelimit.service.RateLimitService;

@Service
public class OrchestratorService {

    private final RateLimitService rateLimitService;
    private final FileValidationService fileValidationService;
    private final TextExtractionService textExtractionService;
    private final ParsingService parsingService;
    private final AiAnalysisService aiAnalysisService;
    private final ScoringService scoringService;

    public OrchestratorService(
            RateLimitService rateLimitService,
            FileValidationService fileValidationService,
            TextExtractionService textExtractionService,
            ParsingService parsingService,
            AiAnalysisService aiAnalysisService,
            ScoringService scoringService
    ) {
        this.rateLimitService = rateLimitService;
        this.fileValidationService = fileValidationService;
        this.textExtractionService = textExtractionService;
        this.parsingService = parsingService;
        this.aiAnalysisService = aiAnalysisService;
        this.scoringService = scoringService;
    }

    public AnalysisResponse process(MultipartFile file, String jobDescription, String clientKey) {

        // 1. Enforce rate limit per client IP
        rateLimitService.checkLimit(clientKey);

        // 2. Validate uploaded file and job description
        fileValidationService.validate(file, jobDescription);

        // 3. Extract raw text and structural info from PDF/DOCX
        ExtractedContent extractedContent = textExtractionService.extract(file);

        // 4. Evaluate layout and font compliance (ATS parsing gate)
        ParsingResult parsingResult = parsingService.evaluate(
                extractedContent.structuralInfo(),
                extractedContent.rawText()
        );

        // 5. Perform semantic and keyword analysis via Groq AI
        AiAnalysisResult aiResult = aiAnalysisService.analyze(
                extractedContent.rawText(),
                jobDescription
        );

        // 6. Calculate weighted total score from all sub-scores
        int totalScore = scoringService.calculateTotalScore(
                parsingResult.parsingScore(),
                aiResult.keywordScore(),
                aiResult.completenessScore()
        );

        boolean isCritical = scoringService.isCriticalParsingScore(parsingResult.parsingScore());
        String criticalWarning = scoringService.buildCriticalWarning(parsingResult.parsingScore());

        // Merge all issues from layout parsing and keyword analysis
        List<Issue> allIssues = new ArrayList<>();
        allIssues.addAll(parsingResult.issues());
        if (aiResult.keywordIssues() != null) {
            allIssues.addAll(aiResult.keywordIssues());
        }

        // 7. Build and return the final analysis response DTO
        return new AnalysisResponse(
                totalScore,
                parsingResult.parsingScore(),
                aiResult.keywordScore(),
                aiResult.completenessScore(),
                isCritical,
                criticalWarning,
                allIssues,
                aiResult.rewriteSuggestions()
        );
    }
}