package com.example.cvmanager.cv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CvCreateRequest(
        @NotNull 
        Long ownerUserId,

        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank 
        @Size(max = 500)
        String uploadedHtmlFilePath
) {}
