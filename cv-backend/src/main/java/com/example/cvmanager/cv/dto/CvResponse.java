package com.example.cvmanager.cv.dto;

import java.time.LocalDateTime;

public record CvResponse(
        Long id,
        Long ownerUserId,
        String ownerEmail,
        String title,
        String summary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime archivedAt) {
}
