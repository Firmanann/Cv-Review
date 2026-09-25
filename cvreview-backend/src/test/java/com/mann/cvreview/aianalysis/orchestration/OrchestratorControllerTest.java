package com.mann.cvreview.aianalysis.orchestration;

import com.mann.cvreview.aianalysis.filevalidation.exception.InvalidInputException;
import com.mann.cvreview.aianalysis.orchestration.controller.OrchestratorController;
import com.mann.cvreview.aianalysis.orchestration.dto.AnalysisResponse;
import com.mann.cvreview.aianalysis.orchestration.service.OrchestratorService;
import com.mann.cvreview.ratelimit.exception.RateLimitExceededException;
import com.mann.cvreview.util.exception.GlobalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrchestratorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrchestratorService orchestratorService;

    @InjectMocks
    private OrchestratorController orchestratorController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orchestratorController)
                .setControllerAdvice(new GlobalException())
                .build();
    }

    @Test
    @DisplayName("POST /api/cv/analyze returns 200 and AnalysisResponse on valid request")
    void analyzeCv_validRequest_returns200AndResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile("cvFile", "cv.pdf", "application/pdf", "dummy pdf content".getBytes());
        String jd = "We are hiring a Java Developer with Spring Boot experience. Must have 5 years experience.";

        AnalysisResponse expectedResponse = new AnalysisResponse(
                85, 90, 80, 85, false, null, List.of(), List.of()
        );

        given(orchestratorService.process(any(), any(), any())).willReturn(expectedResponse);

        mockMvc.perform(multipart("/api/cv/analyze")
                        .file(file)
                        .param("jobDescription", jd))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(85))
                .andExpect(jsonPath("$.parsingScore").value(90))
                .andExpect(jsonPath("$.keywordScore").value(80))
                .andExpect(jsonPath("$.isCritical").value(false));
    }

    @Test
    @DisplayName("POST /api/cv/analyze returns 400 when validation fails")
    void analyzeCv_validationFails_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("cvFile", "cv.pdf", "application/pdf", "dummy".getBytes());
        String shortJd = "Too short";

        org.mockito.Mockito.lenient().doThrow(new InvalidInputException("Job description is too short, minimum 50 characters"))
                .when(orchestratorService).process(any(), any(), any());

        mockMvc.perform(multipart("/api/cv/analyze")
                        .file(file)
                        .param("jobDescription", shortJd))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Job description is too short, minimum 50 characters"));
    }

    @Test
    @DisplayName("POST /api/cv/analyze returns 429 when rate limit is exceeded")
    void analyzeCv_rateLimitExceeded_returns429() throws Exception {
        MockMultipartFile file = new MockMultipartFile("cvFile", "cv.pdf", "application/pdf", "dummy".getBytes());
        String jd = "Valid job description length for testing rate limit exception handling.";

        given(orchestratorService.process(any(), any(), any()))
                .willThrow(new RateLimitExceededException("Daily analysis limit reached, please try again tomorrow"));

        mockMvc.perform(multipart("/api/cv/analyze")
                        .file(file)
                        .param("jobDescription", jd))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Daily analysis limit reached, please try again tomorrow"));
    }
}
