package com.mann.cvreview.util.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mann.cvreview.aianalysis.ai.exception.AiResponseParsingException;
import com.mann.cvreview.aianalysis.extraction.exception.TextExtractionException;
import com.mann.cvreview.aianalysis.filevalidation.exception.InvalidInputException;
import com.mann.cvreview.ratelimit.exception.RateLimitExceededException;
import com.mann.cvreview.util.response.ApiResponse;

@RestControllerAdvice
public class GlobalException {

    private static final Logger log = LoggerFactory.getLogger(GlobalException.class);

    // 1. Tangani Semua Business Exception (Logika Bisnis Custom)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("Business exception occurred: {} - {}", e.getErrorCode().name(), e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(e.getMessage()));
    }

    // 2. Tangani Error Validasi DTO (@Valid / @NotBlank / @Email / dsb)
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        if (errorMessage.isBlank()) {
            errorMessage = ErrorCode.VALIDATION_ERROR.getMessage();
        }

        log.warn("Validation failed: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMessage));
    }

    // 3. Tangani Rate Limit Exceeded
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimit(RateLimitExceededException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(e.getMessage()));
    }

    // 4. Tangani Invalid File Input
    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidInput(InvalidInputException e) {
        log.warn("Invalid input: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    // 5. Tangani Text Extraction Failure
    @ExceptionHandler(TextExtractionException.class)
    public ResponseEntity<ApiResponse<Void>> handleExtraction(TextExtractionException e) {
        log.error("Text extraction failed: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(e.getMessage()));
    }

    // 6. Tangani AI Parsing Failure
    @ExceptionHandler(AiResponseParsingException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiParsing(AiResponseParsingException e) {
        log.error("AI response parsing error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("Layanan analisis AI mengalami gangguan, silakan coba lagi"));
    }

    // 7. Fallback untuk Error Tak Terduga (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("Unhandled server exception: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}