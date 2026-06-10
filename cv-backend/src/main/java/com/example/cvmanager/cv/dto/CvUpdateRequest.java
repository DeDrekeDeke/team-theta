package com.example.cvmanager.cv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CvUpdateRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank 
        @Size(max = 500)
        String uploadedHtmlFilePath
) {}
