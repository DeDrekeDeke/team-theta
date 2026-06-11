package com.example.cvmanager.cv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CvUpdateRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be 255 characters or fewer")
        String title,

        @NotBlank(message = "Uploaded HTML file path is required")
        @Size(max = 500, message = "Uploaded HTML file path must be 500 characters or fewer")
        String uploadedHtmlFilePath
) {}
