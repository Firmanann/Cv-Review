package com.mann.cvreview.aianalysis.extraction;

import com.mann.cvreview.aianalysis.extraction.dto.ExtractedContent;
import com.mann.cvreview.aianalysis.extraction.exception.TextExtractionException;
import com.mann.cvreview.aianalysis.extraction.service.TextExtractionService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class TextExtractionServiceTest {

    private TextExtractionService textExtractionService;

    @BeforeEach
    void setUp() {
        textExtractionService = new TextExtractionService();
    }

    private byte[] createSamplePdfBytes() throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText("EXPERIENCE");
                content.newLineAtOffset(0, -20);
                content.showText("Software Engineer with Java experience");
                content.endText();
            }

            doc.save(out);
            return out.toByteArray();
        }
    }

    private byte[] createSampleDocxBytes() throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun r = p.createRun();
            r.setText("EXPERIENCE\nSoftware Engineer with Spring Boot experience");
            r.setFontFamily("Arial");
            doc.write(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("Extract content successfully from a valid PDF")
    void extract_validPdf_returnsExtractedContent() throws IOException {
        byte[] pdfBytes = createSamplePdfBytes();
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", pdfBytes);

        ExtractedContent result = textExtractionService.extract(file);

        assertNotNull(result);
        assertNotNull(result.rawText());
        assertTrue(result.rawText().contains("EXPERIENCE"));
        assertNotNull(result.structuralInfo());
        assertFalse(result.structuralInfo().isMultiColumn());
    }

    @Test
    @DisplayName("Extract content successfully from a valid DOCX")
    void extract_validDocx_returnsExtractedContent() throws IOException {
        byte[] docxBytes = createSampleDocxBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cv.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
        );

        ExtractedContent result = textExtractionService.extract(file);

        assertNotNull(result);
        assertNotNull(result.rawText());
        assertTrue(result.rawText().contains("EXPERIENCE"));
        assertNotNull(result.structuralInfo());
    }

    @Test
    @DisplayName("Corrupt input file throws TextExtractionException")
    void extract_corruptFile_throwsTextExtractionException() {
        MockMultipartFile file = new MockMultipartFile("file", "bad.pdf", "application/pdf", "corrupt data".getBytes());

        assertThrows(TextExtractionException.class, () -> textExtractionService.extract(file));
    }
}
