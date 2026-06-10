package com.example.cvmanager.cv.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cvmanager.auth.security.SecurityConfig;
import com.example.cvmanager.common.exception.GlobalExceptionHandler;
import com.example.cvmanager.cv.dto.CvCreateRequest;
import com.example.cvmanager.cv.dto.CvResponse;
import com.example.cvmanager.cv.dto.CvUpdateRequest;
import com.example.cvmanager.cv.service.CvService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CvController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class CvControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CvService cvService;

    @Test
    void listCvsReturnsCurrentCvs() throws Exception {
        when(cvService.listCvs()).thenReturn(List.of(response(1L, 2L, "Alice Demo CV")));

        mockMvc.perform(get("/api/cvs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].ownerUserId").value(2))
                .andExpect(jsonPath("$[0].title").value("Alice Demo CV"));
    }

    @Test
    void searchCvsPassesSpecialCharactersToService() throws Exception {
        when(cvService.searchCvs("O'Connor %_;--")).thenReturn(List.of(response(3L, 2L, "O'Connor CV")));

        mockMvc.perform(get("/api/cvs/search").param("q", "O'Connor %_;--"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("O'Connor CV"));

        verify(cvService).searchCvs("O'Connor %_;--");
    }

    @Test
    void createCvReturnsCreatedResponse() throws Exception {
        when(cvService.createCv(any(CvCreateRequest.class))).thenReturn(response(4L, 2L, "New CV"));

        mockMvc.perform(post("/api/cvs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerUserId": 2,
                                  "title": "New CV",
                                  "uploadedHtmlFilePath": "uploads/new.html"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.ownerUserId").value(2))
                .andExpect(jsonPath("$.title").value("New CV"));
    }

    @Test
    void createCvRejectsBlankTitleBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/cvs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerUserId": 2,
                                  "title": "",
                                  "uploadedHtmlFilePath": "uploads/new.html"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(cvService);
    }

    @Test
    void uploadCvRoutesMultipartUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "alice.html",
                MediaType.TEXT_HTML_VALUE,
                "<html>Alice CV</html>".getBytes());
        when(cvService.uploadHtmlCv(eq(2L), eq("Alice Upload"), any()))
                .thenReturn(response(5L, 2L, "Alice Upload"));

        mockMvc.perform(multipart("/api/cvs/upload")
                        .file(file)
                        .param("ownerUserId", "2")
                        .param("title", "Alice Upload"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.title").value("Alice Upload"));
    }

    @Test
    void updateCvRoutesRequestBody() throws Exception {
        when(cvService.updateCv(eq(7L), any(CvUpdateRequest.class)))
                .thenReturn(response(7L, 2L, "Updated CV"));

        mockMvc.perform(put("/api/cvs/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated CV",
                                  "uploadedHtmlFilePath": "uploads/updated.html"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Updated CV"));
    }

    @Test
    void archiveCvReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/cvs/7"))
                .andExpect(status().isNoContent());

        verify(cvService).archiveCv(7L);
    }

    @Test
    void legacyPreviewReturnsHtml() throws Exception {
        when(cvService.buildLegacyPreview("CV:\nAlice\nJava"))
                .thenReturn("<html><body><h1>Alice</h1></body></html>");

        mockMvc.perform(post("/api/cvs/legacy-preview")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("CV:\nAlice\nJava"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<html><body><h1>Alice</h1></body></html>"));
    }

    private CvResponse response(Long id, Long ownerUserId, String title) {
        return new CvResponse(
                id,
                ownerUserId,
                "alice@example.com",
                title,
                "uploads/alice.html",
                LocalDateTime.of(2026, 6, 10, 12, 0),
                LocalDateTime.of(2026, 6, 10, 12, 30),
                null);
    }
}
