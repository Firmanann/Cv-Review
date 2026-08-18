package com.mann.cvreview.aianalysis.orchestration.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserInput(

        @NotNull(message = "CV file is required")
        MultipartFile cvFile,

        @NotBlank(message = "Job description must not be empty")
        @Size(min = 50, message = "Job description is too short, minimum 50 characters")
        String jobDescription
) {}
