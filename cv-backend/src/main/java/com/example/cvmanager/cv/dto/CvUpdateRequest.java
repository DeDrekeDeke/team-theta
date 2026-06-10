package com.example.cvmanager.cv.dto;

import jakarta.validation.constraints.NotBlank;

public record CvUpdateRequest(
        @NotBlank String title,
        String summary) {
}
