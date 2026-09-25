package com.mann.cvreview.aianalysis.orchestration;

import com.mann.cvreview.aianalysis.ai.dto.AiAnalysisResult;
import com.mann.cvreview.aianalysis.ai.dto.Issue;
import com.mann.cvreview.aianalysis.ai.dto.RewriteSuggestion;
import com.mann.cvreview.aianalysis.ai.service.AiAnalysisService;
import com.mann.cvreview.aianalysis.extraction.dto.ExtractedContent;
import com.mann.cvreview.aianalysis.extraction.dto.StructuralInfo;
import com.mann.cvreview.aianalysis.extraction.service.TextExtractionService;
import com.mann.cvreview.aianalysis.filevalidation.service.FileValidationService;
import com.mann.cvreview.aianalysis.orchestration.dto.AnalysisResponse;
import com.mann.cvreview.aianalysis.orchestration.service.OrchestratorService;
import com.mann.cvreview.aianalysis.parsing.dto.ParsingResult;
import com.mann.cvreview.aianalysis.parsing.service.ParsingService;
import com.mann.cvreview.aianalysis.scoring.service.ScoringService;
import com.mann.cvreview.ratelimit.service.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrchestratorServiceTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private FileValidationService fileValidationService;

    @Mock
    private TextExtractionService textExtractionService;

    @Mock
    private ParsingService parsingService;

    @Mock
    private AiAnalysisService aiAnalysisService;

    @Mock
    private ScoringService scoringService;

    @InjectMocks
    private OrchestratorService orchestratorService;

    @Test
    @DisplayName("process successfully coordinates all services and aggregates results")
    void process_successfulFlow_aggregatesResults() {
        MockMultipartFile file = new MockMultipartFile("cvFile", "cv.pdf", "application/pdf", "dummy content".getBytes());
        String jd = "Software Engineer position with Java and Spring Boot requirements.";
        String clientKey = "127.0.0.1";

        StructuralInfo structuralInfo = new StructuralInfo(
                false, false, false, false, List.of("Arial"), List.of("EXPERIENCE"), false, 300
        );
        ExtractedContent extractedContent = new ExtractedContent("dummy raw text", structuralInfo);

        Issue layoutIssue = new Issue("layout", "Multi-column detected", "high");
        ParsingResult parsingResult = new ParsingResult(80, List.of(layoutIssue));

        Issue keywordIssue = new Issue("keyword", "Missing Docker", "medium");
        RewriteSuggestion suggestion = new RewriteSuggestion("Led team", "Led a team of 5", "Added metrics");
        AiAnalysisResult aiResult = new AiAnalysisResult(
                85, 90, List.of("Java"), List.of("Docker"), List.of(keywordIssue), List.of(suggestion)
        );

        given(textExtractionService.extract(file)).willReturn(extractedContent);
        given(parsingService.evaluate(structuralInfo, "dummy raw text")).willReturn(parsingResult);
        given(aiAnalysisService.analyze("dummy raw text", jd)).willReturn(aiResult);
        given(scoringService.calculateTotalScore(80, 85, 90)).willReturn(84);
        given(scoringService.isCriticalParsingScore(80)).willReturn(false);
        given(scoringService.buildCriticalWarning(80)).willReturn(null);

        AnalysisResponse response = orchestratorService.process(file, jd, clientKey);

        assertNotNull(response);
        assertEquals(84, response.totalScore());
        assertEquals(80, response.parsingScore());
        assertEquals(85, response.keywordScore());
        assertEquals(90, response.completenessScore());
        assertFalse(response.isCritical());
        assertNull(response.criticalWarning());
        assertEquals(2, response.issues().size());
        assertEquals(1, response.rewriteSuggestions().size());

        verify(rateLimitService).checkLimit(clientKey);
        verify(fileValidationService).validate(file, jd);
        verify(textExtractionService).extract(file);
        verify(parsingService).evaluate(structuralInfo, "dummy raw text");
        verify(aiAnalysisService).analyze("dummy raw text", jd);
        verify(scoringService).calculateTotalScore(80, 85, 90);
    }

    @Test
    @DisplayName("process correctly flags critical warning when parsing score is critical")
    void process_criticalParsingScore_setsCriticalFlag() {
        MockMultipartFile file = new MockMultipartFile("cvFile", "cv.pdf", "application/pdf", "dummy content".getBytes());
        String jd = "Software Engineer position with Java and Spring Boot requirements.";
        String clientKey = "127.0.0.1";

        StructuralInfo structuralInfo = new StructuralInfo(
                true, true, true, false, List.of("Arial"), List.of(), false, 50
        );
        ExtractedContent extractedContent = new ExtractedContent("short text", structuralInfo);
        ParsingResult parsingResult = new ParsingResult(30, List.of());
        AiAnalysisResult aiResult = new AiAnalysisResult(70, 60, List.of(), List.of(), null, List.of());

        given(textExtractionService.extract(file)).willReturn(extractedContent);
        given(parsingService.evaluate(structuralInfo, "short text")).willReturn(parsingResult);
        given(aiAnalysisService.analyze("short text", jd)).willReturn(aiResult);
        given(scoringService.calculateTotalScore(30, 70, 60)).willReturn(52);
        given(scoringService.isCriticalParsingScore(30)).willReturn(true);
        given(scoringService.buildCriticalWarning(30)).willReturn("CV format is not ATS friendly");

        AnalysisResponse response = orchestratorService.process(file, jd, clientKey);

        assertNotNull(response);
        assertTrue(response.isCritical());
        assertEquals("CV format is not ATS friendly", response.criticalWarning());
    }
}
