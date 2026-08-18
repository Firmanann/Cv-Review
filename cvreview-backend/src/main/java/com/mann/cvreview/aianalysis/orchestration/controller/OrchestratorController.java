package com.mann.cvreview.aianalysis.orchestration.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mann.cvreview.aianalysis.orchestration.dto.AnalysisResponse;
import com.mann.cvreview.aianalysis.orchestration.dto.UserInput;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cv")
@CrossOrigin(origins = "*")
public class OrchestratorController {

    // Dependency injection for orchestrator service
    private final com.mann.cvreview.aianalysis.orchestration.service.OrchestratorService orchestrator;

    public OrchestratorController(com.mann.cvreview.aianalysis.orchestration.service.OrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisResponse> analyzeCv(@Valid @ModelAttribute UserInput requestDto, HttpServletRequest request) {

        // Resolve client IP address for rate limiting
        String clientKey = resolveClientKey(request);

        // Execute full analysis pipeline through orchestrator
        AnalysisResponse response = orchestrator.process(
                requestDto.cvFile(),
                requestDto.jobDescription(),
                clientKey
        );

        return ResponseEntity.ok(response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}