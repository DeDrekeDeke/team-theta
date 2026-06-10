package com.example.cvmanager.cv.controller;

import com.example.cvmanager.cv.dto.CvCreateRequest;
import com.example.cvmanager.cv.dto.CvResponse;
import com.example.cvmanager.cv.dto.CvUpdateRequest;
import com.example.cvmanager.cv.service.CvService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cvs")
public class CvController {

    private final CvService cvService;

    public CvController(CvService cvService) {
        this.cvService = cvService;
    }

    @GetMapping
    public List<CvResponse> listCvs() {
        return cvService.listCvs();
    }

    @GetMapping("/search")
    public List<CvResponse> searchCvs(@RequestParam String q) {
        return cvService.searchCvs(q);
    }

    @GetMapping("/{id}")
    public CvResponse getCv(@PathVariable Long id) {
        return cvService.getCv(id);
    }

    @GetMapping(value = "/{id}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getCvHtml(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(cvService.getUploadedHtml(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CvResponse createCv(@Valid @RequestBody CvCreateRequest request) {
        return cvService.createCv(request);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CvResponse uploadCv(
            @RequestParam Long ownerUserId,
            @RequestParam(required = false) String title,
            @RequestParam("file") MultipartFile file) {
        return cvService.uploadHtmlCv(ownerUserId, title, file);
    }

    @PostMapping(value = "/legacy-preview", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public String legacyPreview(@RequestBody String input) {
        return cvService.buildLegacyPreview(input);
    }

    @PutMapping("/{id}")
    public CvResponse updateCv(@PathVariable Long id, @Valid @RequestBody CvUpdateRequest request) {
        return cvService.updateCv(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveCv(@PathVariable Long id) {
        cvService.archiveCv(id);
    }
}
