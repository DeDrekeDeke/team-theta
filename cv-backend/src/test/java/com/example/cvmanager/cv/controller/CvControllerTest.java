package com.example.cvmanager.cv.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cvmanager.auth.security.SecurityConfig;
import com.example.cvmanager.common.exception.GlobalExceptionHandler;
import com.example.cvmanager.cv.service.CvService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
    void createCvRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/cvs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerUserId": 1,
                                  "title": "",
                                  "uploadedHtmlFilePath": "uploads/alice-cv.html"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0]", containsString("Title is required")));

        verifyNoInteractions(cvService);
    }

    @Test
    void uploadCvRejectsMissingFileWithValidationResponse() throws Exception {
        mockMvc.perform(multipart("/api/cvs/upload")
                        .param("ownerUserId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0]").value("file: is required"));

        verifyNoInteractions(cvService);
    }
}
