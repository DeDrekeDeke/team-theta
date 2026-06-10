package com.example.cvmanager.cv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CvCreateRequest(
        @NotNull Long ownerUserId,
        @NotBlank String title,
        String summary) {
}
