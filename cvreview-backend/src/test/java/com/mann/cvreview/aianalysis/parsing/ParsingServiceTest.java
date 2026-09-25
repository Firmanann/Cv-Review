package com.mann.cvreview.aianalysis.parsing;

import com.mann.cvreview.aianalysis.extraction.dto.StructuralInfo;
import com.mann.cvreview.aianalysis.parsing.dto.ParsingResult;
import com.mann.cvreview.aianalysis.parsing.service.ParsingService;
import com.mann.cvreview.util.config.ParsingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParsingServiceTest {

    private ParsingService parsingService;

    @BeforeEach
    void setUp() {
        ParsingConfig config = new ParsingConfig();
        config.setHeadingWhitelist(List.of("EXPERIENCE", "EDUCATION", "SKILLS", "SUMMARY", "CERTIFICATIONS"));
        config.setFontWhitelist(List.of("Arial", "Calibri", "Times New Roman", "Georgia"));
        config.setMinTextLength(200);
        parsingService = new ParsingService(config);
    }

    private StructuralInfo createCleanStructuralInfo(int length, List<String> headings) {
        return new StructuralInfo(
                false, // isMultiColumn
                false, // hasTable
                false, // hasTextBox
                false, // hasImage
                List.of("Arial"), // fontsUsed
                headings, // detectedHeadings
                false, // contactInfoInHeaderFooter
                length
        );
    }

    @Test
    @DisplayName("Evaluate clean single-column CV with valid headings returns score 100")
    void evaluate_cleanSingleColumnNoIssues_returnsScore100() {
        StructuralInfo info = createCleanStructuralInfo(300, List.of("EXPERIENCE", "EDUCATION", "SKILLS"));
        ParsingResult result = parsingService.evaluate(info, "Dummy CV raw text with enough length...");

        assertEquals(100, result.parsingScore());
        assertTrue(result.issues().isEmpty());
    }

    @Test
    @DisplayName("Evaluate multi-column layout deducts high penalty (-20)")
    void evaluate_multiColumnLayout_deductsHighPenalty() {
        StructuralInfo info = new StructuralInfo(
                true, false, false, false, List.of("Arial"), List.of("EXPERIENCE"), false, 300
        );
        ParsingResult result = parsingService.evaluate(info, "text");

        assertEquals(80, result.parsingScore());
        assertEquals(1, result.issues().size());
        assertEquals("layout", result.issues().get(0).category());
        assertEquals("high", result.issues().get(0).severity());
    }

    @Test
    @DisplayName("Evaluate tables, textboxes, images deduct penalties accordingly")
    void evaluate_multipleLayoutIssues_deductsCumulativePenalties() {
        // multiColumn (-20), hasTable (-20), hasTextBox (-20), hasImage (-10) => 100 - 70 = 30
        StructuralInfo info = new StructuralInfo(
                true, true, true, true, List.of("Arial"), List.of("EXPERIENCE"), false, 300
        );
        ParsingResult result = parsingService.evaluate(info, "text");

        assertEquals(30, result.parsingScore());
        assertEquals(4, result.issues().size());
    }

    @Test
    @DisplayName("Evaluate non-whitelisted font deducts medium penalty (-10)")
    void evaluate_nonStandardFont_deductsMediumPenalty() {
        StructuralInfo info = new StructuralInfo(
                false, false, false, false, List.of("Comic Sans"), List.of("EXPERIENCE"), false, 300
        );
        ParsingResult result = parsingService.evaluate(info, "text");

        assertEquals(90, result.parsingScore());
        assertTrue(result.issues().stream().anyMatch(i -> i.category().equals("font")));
    }

    @Test
    @DisplayName("Evaluate contact info in header/footer deducts high penalty (-20)")
    void evaluate_contactInHeaderFooter_deductsHighPenalty() {
        StructuralInfo info = new StructuralInfo(
                false, false, false, false, List.of("Calibri"), List.of("EXPERIENCE"), true, 300
        );
        ParsingResult result = parsingService.evaluate(info, "text");

        assertEquals(80, result.parsingScore());
        assertTrue(result.issues().stream().anyMatch(i -> i.category().equals("contact")));
    }

    @Test
    @DisplayName("Evaluate text shorter than minimum length deducts high penalty (-20)")
    void evaluate_textTooShort_deductsHighPenalty() {
        StructuralInfo info = createCleanStructuralInfo(50, List.of("EXPERIENCE"));
        ParsingResult result = parsingService.evaluate(info, "Short text");

        assertEquals(80, result.parsingScore());
        assertTrue(result.issues().stream().anyMatch(i -> i.category().equals("integrity")));
    }

    @Test
    @DisplayName("Evaluate missing recognized headings deducts high penalty (-20)")
    void evaluate_noRecognizedHeadings_deductsHighPenalty() {
        StructuralInfo info = createCleanStructuralInfo(300, List.of("HOBBIES", "ABOUT ME"));
        ParsingResult result = parsingService.evaluate(info, "Text");

        assertEquals(80, result.parsingScore());
        assertTrue(result.issues().stream().anyMatch(i -> i.category().equals("structure")));
    }

    @Test
    @DisplayName("Parsing score does not go below zero even with excessive issues")
    void evaluate_excessiveIssues_scoreDoesNotGoBelowZero() {
        // 6 high issues (-120) + 1 medium issue (-10) = -130 -> should floor at 0
        StructuralInfo info = new StructuralInfo(
                true, true, true, true, List.of("Comic Sans"), List.of("HOBBIES"), true, 50
        );
        ParsingResult result = parsingService.evaluate(info, "Short text");

        assertEquals(0, result.parsingScore());
    }
}
