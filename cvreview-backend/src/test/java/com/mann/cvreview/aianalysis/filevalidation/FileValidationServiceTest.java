package com.mann.cvreview.aianalysis.filevalidation;

import com.mann.cvreview.aianalysis.filevalidation.exception.InvalidInputException;
import com.mann.cvreview.aianalysis.filevalidation.service.FileValidationService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FileValidationServiceTest {

    private FileValidationService fileValidationService;
    private String validJd;

    @BeforeEach
    void setUp() {
        fileValidationService = new FileValidationService();
        validJd = "We are looking for a Senior Software Engineer with strong Java Spring Boot skills and Redis experience. " +
                "The candidate should have at least 5 years of professional experience building web applications.";
    }

    private byte[] createValidPdfBytes() throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("Null file throws InvalidInputException")
    void validate_nullFile_throwsInvalidInputException() {
        InvalidInputException ex = assertThrows(InvalidInputException.class, () ->
                fileValidationService.validate(null, validJd)
        );
        assertEquals("CV file is required", ex.getMessage());
    }

    @Test
    @DisplayName("Empty file throws InvalidInputException")
    void validate_emptyFile_throwsInvalidInputException() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[0]);
        InvalidInputException ex = assertThrows(InvalidInputException.class, () ->
                fileValidationService.validate(file, validJd)
        );
        assertEquals("CV file is required", ex.getMessage());
    }

    @Test
    @DisplayName("Unsupported file type (e.g. image/png) throws InvalidInputException")
    void validate_unsupportedMimeType_throwsInvalidInputException() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.png", "image/png", new byte[]{1, 2, 3});
        InvalidInputException ex = assertThrows(InvalidInputException.class, () ->
                fileValidationService.validate(file, validJd)
        );
        assertEquals("Unsupported file format, use PDF or DOCX", ex.getMessage());
    }

    @Test
    @DisplayName("File exceeding 5MB throws InvalidInputException")
    void validate_fileTooLarge_throwsInvalidInputException() {
        byte[] largeBytes = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", largeBytes);
        InvalidInputException ex = assertThrows(InvalidInputException.class, () ->
                fileValidationService.validate(file, validJd)
        );
        assertEquals("File size must not exceed 5MB", ex.getMessage());
    }

    @Test
    @DisplayName("Job description shorter than 50 characters throws InvalidInputException")
    void validate_jobDescriptionTooShort_throwsInvalidInputException() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[]{1, 2, 3});
        InvalidInputException ex = assertThrows(InvalidInputException.class, () ->
                fileValidationService.validate(file, "Too short JD")
        );
        assertEquals("Job description is too short, minimum 50 characters", ex.getMessage());
    }

    @Test
    @DisplayName("Job description exceeding 5000 characters throws InvalidInputException")
    void validate_jobDescriptionTooLong_throwsInvalidInputException() {
        String longJd = "A".repeat(5001);
        MockMultipartFile file = new MockMultipartFile("file", "cv.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[]{1, 2, 3});
        InvalidInputException ex = assertThrows(InvalidInputException.class, () ->
                fileValidationService.validate(file, longJd)
        );
        assertEquals("Job description is too long, maximum 5000 characters", ex.getMessage());
    }

    @Test
    @DisplayName("Valid PDF file and job description passes validation")
    void validate_validPdfAndJd_doesNotThrow() throws IOException {
        byte[] pdfBytes = createValidPdfBytes();
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", pdfBytes);

        assertDoesNotThrow(() -> fileValidationService.validate(file, validJd));
    }

    @Test
    @DisplayName("Corrupted PDF file throws InvalidInputException")
    void validate_corruptPdf_throwsInvalidInputException() {
        byte[] corruptBytes = "NOT A REAL PDF CONTENT".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", corruptBytes);

        InvalidInputException ex = assertThrows(InvalidInputException.class, () ->
                fileValidationService.validate(file, validJd)
        );
        assertTrue(ex.getMessage().contains("Failed to read PDF file"));
    }
}
