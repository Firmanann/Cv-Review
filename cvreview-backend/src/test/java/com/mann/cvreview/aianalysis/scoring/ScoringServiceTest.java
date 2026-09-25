package com.mann.cvreview.aianalysis.scoring;

import com.mann.cvreview.aianalysis.scoring.service.ScoringService;
import com.mann.cvreview.util.config.ParsingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoringServiceTest {

    private ScoringService scoringService;

    @BeforeEach
    void setUp() {
        ParsingConfig config = new ParsingConfig();
        config.setWeightParsing(0.4);
        config.setWeightKeyword(0.4);
        config.setWeightCompleteness(0.2);
        scoringService = new ScoringService(config);
    }

    @Test
    @DisplayName("Calculate total score with custom weights correctly")
    void calculateTotalScore_withWeights_returnsCorrectWeightedAverage() {
        // (100 * 0.4) + (80 * 0.4) + (60 * 0.2) = 40 + 32 + 12 = 84
        int totalScore = scoringService.calculateTotalScore(100, 80, 60);
        assertEquals(84, totalScore);
    }

    @Test
    @DisplayName("Calculate total score when all sub-scores are zero")
    void calculateTotalScore_allZero_returnsZero() {
        int totalScore = scoringService.calculateTotalScore(0, 0, 0);
        assertEquals(0, totalScore);
    }

    @Test
    @DisplayName("Calculate total score when all sub-scores are 100")
    void calculateTotalScore_allPerfect_returns100() {
        int totalScore = scoringService.calculateTotalScore(100, 100, 100);
        assertEquals(100, totalScore);
    }

    @Test
    @DisplayName("Critical parsing score check when score is below 40")
    void isCriticalParsingScore_below40_returnsTrue() {
        assertTrue(scoringService.isCriticalParsingScore(39));
        assertTrue(scoringService.isCriticalParsingScore(0));
    }

    @Test
    @DisplayName("Critical parsing score check when score is 40 or above")
    void isCriticalParsingScore_40orAbove_returnsFalse() {
        assertFalse(scoringService.isCriticalParsingScore(40));
        assertFalse(scoringService.isCriticalParsingScore(80));
    }

    @Test
    @DisplayName("Build critical warning returns message when parsing score is critical")
    void buildCriticalWarning_criticalScore_returnsWarningString() {
        String warning = scoringService.buildCriticalWarning(30);
        assertNotNull(warning);
        assertTrue(warning.contains("ATS"));
    }

    @Test
    @DisplayName("Build critical warning returns null when parsing score is safe")
    void buildCriticalWarning_normalScore_returnsNull() {
        String warning = scoringService.buildCriticalWarning(50);
        assertNull(warning);
    }
}
