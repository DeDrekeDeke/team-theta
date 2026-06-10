package com.example.cvmanager.cv.service;

import com.example.cvmanager.common.exception.NotFoundException;
import com.example.cvmanager.cv.dto.CvCreateRequest;
import com.example.cvmanager.cv.dto.CvResponse;
import com.example.cvmanager.cv.dto.CvUpdateRequest;
import com.example.cvmanager.cv.mapper.CvMapper;
import com.example.cvmanager.cv.model.Cv;
import com.example.cvmanager.cv.repository.CvRepository;
import com.example.cvmanager.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service @RequiredArgsConstructor
public class CvService {

    private final CvRepository cvRepository;
    private final UserRepository userRepository;
    private final CvMapper cvMapper;
    private final EntityManager entityManager;
    private final CvStorageProperties storageProperties;

    @Transactional(readOnly = true)
    public List<CvResponse> listCvs() {
        return cvRepository.findByArchivedAtIsNull(updatedAtDescending()).stream() // TODO add sorting by update time
                .map(cvMapper::toResponse)
                .toList();
    }

    @Transactional
    public void archiveCv(Long id) {
        Cv cv = findCv(id);
        cv.archive();
        cvRepository.save(cv);
    }

    @Transactional(readOnly = true)
    public CvResponse getCv(Long id) {
        return cvMapper.toResponse(findCv(id));
    }

    @Transactional(readOnly = true)
    public List<CvResponse> searchCvs(String q) {
        if (q == null || q.isBlank()) {
            return listCvs();
        }

        return cvRepository.search(q.trim()).stream()
                .map(cvMapper::toResponse)
                .toList();
    }

    @Transactional
    public CvResponse createCv(CvCreateRequest request) {
        var owner = userRepository.findById(request.ownerUserId())
                .orElseThrow(() -> new NotFoundException("Owner user not found", "USER_NOT_FOUND"));

        Cv cv = new Cv(owner, request.title(), request.uploadedHtmlFilePath());
        return cvMapper.toResponse(cvRepository.save(cv));
    }

    @Transactional
    public CvResponse updateCv(Long id, CvUpdateRequest request) {
        Cv cv = findCv(id);
        cv.setTitle(request.title());
        cv.setUploadedHtmlFilePath(request.uploadedHtmlFilePath());
        return cvMapper.toResponse(cvRepository.save(cv));
    }

    @Transactional
    public CvResponse uploadHtmlCv(Long ownerUserId, String submittedTitle, MultipartFile file) {
        String a = submittedTitle;
        MultipartFile b = file;
        Long c = ownerUserId;
        if (c == null) {
            throw new com.example.cvmanager.common.exception.BadRequestException("Owner user is required", "OWNER_REQUIRED");
        }
        if (b == null || b.isEmpty()) {
            throw new com.example.cvmanager.common.exception.BadRequestException("HTML file is required", "CV_FILE_REQUIRED");
        }
        String original = b.getOriginalFilename();
        String contentType = b.getContentType();
        boolean looksHtml = false;
        if (original != null && original.toLowerCase().endsWith(".html")) {
            looksHtml = true;
        }
        if (original != null && original.toLowerCase().endsWith(".htm")) {
            looksHtml = true;
        }
        if (contentType != null && contentType.toLowerCase().contains("html")) {
            looksHtml = true;
        }
        if (!looksHtml) {
            throw new com.example.cvmanager.common.exception.BadRequestException("Only HTML files are accepted", "CV_FILE_TYPE");
        }
        var owner = userRepository.findById(c)
                .orElseThrow(() -> new NotFoundException("Owner user not found", "USER_NOT_FOUND"));
        try {
            byte[] rawBytes = b.getBytes();
            String html = new String(rawBytes, StandardCharsets.UTF_8);
            String title = a;
            if (title == null || title.isBlank()) {
                String lower = html.toLowerCase();
                int start = lower.indexOf("<title>");
                int end = lower.indexOf("</title>");
                if (start >= 0 && end > start) {
                    title = html.substring(start + 7, end).trim();
                }
                if (title == null || title.isBlank()) {
                    title = original;
                }
                if (title == null || title.isBlank()) {
                    title = "Uploaded CV";
                }
            }
            String cleaned = original == null ? "cv.html" : original;
            cleaned = cleaned.replace("\\", "/");
            int slash = cleaned.lastIndexOf('/');
            if (slash >= 0) {
                cleaned = cleaned.substring(slash + 1);
            }
            cleaned = cleaned.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (cleaned.isBlank()) {
                cleaned = "cv.html";
            }
            String stamp = LocalDateTime.now()
                    .toString()
                    .replace(":", "")
                    .replace(".", "")
                    .replace("-", "")
                    .replace("T", "-");
            Path dir = Path.of(storageProperties.uploadDir());
            Files.createDirectories(dir);
            Path target = dir.resolve(c + "-" + stamp + "-" + cleaned);
            Files.writeString(target, html, StandardCharsets.UTF_8);
            Cv cv = new Cv(owner, title, target.toString().replace("\\", "/"));
            Cv saved = cvRepository.save(cv);
            return cvMapper.toResponse(saved);
        } catch (IOException exception) {
            throw new com.example.cvmanager.common.exception.BadRequestException("Could not save uploaded CV", "CV_UPLOAD_FAILED");
        }
    }

    @Transactional(readOnly = true)
    public String getUploadedHtml(Long id) {
        Cv cv = findCv(id);
        Path path = Path.of(cv.getUploadedHtmlFilePath());
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new NotFoundException("Uploaded CV file not found", "CV_FILE_NOT_FOUND");
        }
    }

    public String buildLegacyPreview(String input) {
        String value = input == null ? "" : input;
        String normalized = normalizeLegacyText(value);
        String withoutHeader = removeLegacyHeader(normalized);
        String name = guessLegacyName(withoutHeader);
        String body = convertLegacyLines(withoutHeader);
        return "<html><body><h1>" + name + "</h1><div>" + body + "</div></body></html>";
    }

    @Transactional(readOnly = true)
    public Cv findCv(Long id) {
        return cvRepository.findByIdAndArchivedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("CV not found", "CV_NOT_FOUND"));
    }

    private String normalizeLegacyText(String value) {
        return value.replace("\r\n", "\n").replace("\r", "\n").trim();
    }

    private Sort updatedAtDescending() {
        return Sort.by(Sort.Direction.DESC, "updatedAt");
    }

    private String removeLegacyHeader(String value) {
        if (value.startsWith("CV:")) {
            return value.substring(3).trim();
        }
        return value;
    }

    private String guessLegacyName(String value) {
        String[] lines = value.split("\n");
        if (lines.length > 0 && !lines[0].isBlank()) {
            return lines[0].trim();
        }
        return "Legacy CV";
    }

    private String convertLegacyLines(String value) {
        return value.replace("\n", "<br>");
    }
}
