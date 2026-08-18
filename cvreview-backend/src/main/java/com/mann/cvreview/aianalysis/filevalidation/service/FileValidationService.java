package com.mann.cvreview.aianalysis.filevalidation.service;

import java.util.Set;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mann.cvreview.aianalysis.filevalidation.exception.InvalidInputException;

@Service
public class FileValidationService {

    // Allowed file MIME types (PDF & DOCX)
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    // Maximum file size limit: 5MB in bytes
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    // Maximum job description length (prevent token exhaustion)
    private static final int MAX_JD_LENGTH = 5000;

    public void validate(MultipartFile file, String jd) {
        // 1. Check if file exists and is not empty
        if (file == null || file.isEmpty()) {
            throw new InvalidInputException("CV file is required");
        }

        // 2. Validate file MIME type
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new InvalidInputException("Unsupported file format, use PDF or DOCX");
        }

        // 3. Validate file size does not exceed limit
        if (file.getSize() > MAX_SIZE) {
            throw new InvalidInputException("File size must not exceed 5MB");
        }

        // 4. Validate job description length range (50 - 5000 characters)
        if (jd == null || jd.trim().length() < 50) {
            throw new InvalidInputException("Job description is too short, minimum 50 characters");
        }
        if (jd.trim().length() > MAX_JD_LENGTH) {
            throw new InvalidInputException("Job description is too long, maximum 5000 characters");
        }

        // 5. If PDF, check if password-protected or corrupt early
        if ("application/pdf".equals(file.getContentType())) {
            try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
                if (doc.isEncrypted()) {
                    throw new InvalidInputException("PDF file is password-protected, please upload an unprotected PDF");
                }
            } catch (InvalidInputException e) {
                throw e;
            } catch (Exception e) {
                throw new InvalidInputException("Failed to read PDF file, please ensure the file is not corrupted");
            }
        }
    }
}