package com.example.cvmanager.cv.mapper;

import com.example.cvmanager.cv.dto.CvResponse;
import com.example.cvmanager.cv.model.Cv;
import org.springframework.stereotype.Component;

@Component
public class CvMapper {

    public CvResponse toResponse(Cv cv) {
        return new CvResponse(
                cv.getId(),
                cv.getOwner().getId(),
                cv.getOwner().getEmail(),
                cv.getTitle(),
                cv.getUploadedHtmlFilePath(),
                cv.getCreatedAt(),
                cv.getUpdatedAt(),
                cv.getArchivedAt());
    }
}
