package com.example.cvmanager.cv.service;

import com.example.cvmanager.common.exception.BadRequestException;
import com.example.cvmanager.common.exception.NotFoundException;
import com.example.cvmanager.cv.dto.CvCreateRequest;
import com.example.cvmanager.cv.dto.CvResponse;
import com.example.cvmanager.cv.dto.CvUpdateRequest;
import com.example.cvmanager.cv.mapper.CvMapper;
import com.example.cvmanager.cv.model.Cv;
import com.example.cvmanager.cv.repository.CvRepository;
import com.example.cvmanager.user.repository.UserRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CvService {

    private final CvRepository cvRepository;
    private final UserRepository userRepository;
    private final CvMapper cvMapper;
    private final CvStorageProperties storageProperties;

    private static final int MAX_TITLE_LENGTH = 255;
    private static final long MAX_HTML_UPLOAD_BYTES = 1_000_000;
    private static final Pattern UNSAFE_HTML_PATTERN = Pattern.compile(
            "(?is)<\\s*(script|iframe|object|embed|base|form|input|button|textarea|select|option|meta|link)\\b"
                    + "|\\son[a-z0-9_-]+\\s*="
                    + "|\\ssrcdoc\\s*="
                    + "|(?:href|src|xlink:href)\\s*=\\s*(['\"]?)\\s*(javascript:|data:text/html|vbscript:)");

    public CvService(
            CvRepository cvRepository,
            UserRepository userRepository,
            CvMapper cvMapper,
            CvStorageProperties storageProperties) {
        this.cvRepository = cvRepository;
        this.userRepository = userRepository;
        this.cvMapper = cvMapper;
        this.storageProperties = storageProperties;
    }

    @Transactional(readOnly = true)
    public List<CvResponse> listCvs() {
        return cvRepository.findByArchivedAtIsNull(updatedAtDescending()).stream()
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
        String title = submittedTitle;
        MultipartFile upload = file;
        Long ownerId = ownerUserId;
        if (ownerId == null) {
            throw new BadRequestException("Owner user is required", "OWNER_REQUIRED");
        }
        if (ownerId <= 0) {
            throw new BadRequestException("Owner user id must be positive", "OWNER_INVALID");
        }
        if (upload == null || upload.isEmpty()) {
            throw new BadRequestException("HTML file is required", "CV_FILE_REQUIRED");
        }
        if (upload.getSize() > MAX_HTML_UPLOAD_BYTES) {
            throw new BadRequestException("HTML file is too large", "CV_FILE_TOO_LARGE");
        }
        String original = upload.getOriginalFilename();
        String contentType = upload.getContentType();
        boolean looksHtml = false;
        if (original != null && original.toLowerCase(Locale.ROOT).endsWith(".html")) {
            looksHtml = true;
        }
        if (original != null && original.toLowerCase(Locale.ROOT).endsWith(".htm")) {
            looksHtml = true;
        }
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("html")) {
            looksHtml = true;
        }
        if (!looksHtml) {
            throw new BadRequestException("Only HTML files are accepted", "CV_FILE_TYPE");
        }
        var owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner user not found", "USER_NOT_FOUND"));
        try {
            byte[] rawBytes = upload.getBytes();
            String html = new String(rawBytes, StandardCharsets.UTF_8);
            rejectUnsafeHtml(html);
            if (title == null || title.isBlank()) {
                String lower = html.toLowerCase(Locale.ROOT);
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
            title = title.trim();
            if (title.length() > MAX_TITLE_LENGTH) {
                throw new BadRequestException(
                    "CV title must be 255 characters or fewer",
                    "CV_TITLE_TOO_LONG"
                );
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
            Path target = dir.resolve(ownerId + "-" + stamp + "-" + cleaned);
            Files.writeString(target, html, StandardCharsets.UTF_8);
            Cv cv = new Cv(owner, title, target.toString().replace("\\", "/"));
            Cv saved = cvRepository.save(cv);
            return cvMapper.toResponse(saved);
        } catch (IOException exception) {
            throw new BadRequestException("Could not save uploaded CV", "CV_UPLOAD_FAILED");
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
        String name = escapeHtml(guessLegacyName(withoutHeader));
        String body = convertLegacyLines(escapeHtml(withoutHeader));
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

    private void rejectUnsafeHtml(String html) {
        if (UNSAFE_HTML_PATTERN.matcher(html).find()) {
            throw new BadRequestException("HTML file contains unsafe content", "CV_FILE_UNSAFE");
        }
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

    private String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
