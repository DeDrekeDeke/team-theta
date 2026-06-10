package com.example.cvmanager;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cvmanager.admin.controller.AdminSettingsController;
import com.example.cvmanager.admin.dto.AppSettingResponse;
import com.example.cvmanager.admin.dto.AppSettingUpdateRequest;
import com.example.cvmanager.admin.service.AdminSettingsService;
import com.example.cvmanager.ai.controller.AiController;
import com.example.cvmanager.ai.dto.AiSuggestionResponse;
import com.example.cvmanager.ai.model.AiActionType;
import com.example.cvmanager.ai.service.AiApplicationService;
import com.example.cvmanager.auth.controller.AuthController;
import com.example.cvmanager.auth.dto.LoginRequest;
import com.example.cvmanager.auth.dto.LoginResponse;
import com.example.cvmanager.auth.service.AuthService;
import com.example.cvmanager.common.exception.BadRequestException;
import com.example.cvmanager.common.exception.GlobalExceptionHandler;
import com.example.cvmanager.common.exception.NotFoundException;
import com.example.cvmanager.common.health.HealthController;
import com.example.cvmanager.common.security.AdminAccessService;
import com.example.cvmanager.cv.controller.CvController;
import com.example.cvmanager.cv.dto.CvCreateRequest;
import com.example.cvmanager.cv.dto.CvResponse;
import com.example.cvmanager.cv.dto.CvUpdateRequest;
import com.example.cvmanager.cv.service.CvService;
import com.example.cvmanager.user.controller.UserController;
import com.example.cvmanager.user.dto.UserResponse;
import com.example.cvmanager.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class ControllerWebTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private AuthService authService;
    private CvService cvService;
    private AdminSettingsService adminSettingsService;
    private UserService userService;
    private AdminAccessService adminAccessService;
    private AiApplicationService aiApplicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        cvService = mock(CvService.class);
        adminSettingsService = mock(AdminSettingsService.class);
        userService = mock(UserService.class);
        adminAccessService = mock(AdminAccessService.class);
        aiApplicationService = mock(AiApplicationService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(authService),
                        new CvController(cvService),
                        new AdminSettingsController(adminSettingsService),
                        new UserController(adminAccessService, userService),
                        new AiController(aiApplicationService),
                        new HealthController(),
                        new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void authControllerValidatesAndDelegatesLogin() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse(2L, "alice@example.com", "Alice", false, "demo-token-2"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "alice@example.com", "password", "user123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.token").value("demo-token-2"));

        verify(authService).login(new LoginRequest("alice@example.com", "user123"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "not-an-email", "password", "user123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0]", containsString("email")));
    }

    @Test
    void cvControllerDelegatesReadSearchHtmlLegacyAndUploadEndpoints() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        CvResponse cv = new CvResponse(10L, 2L, "alice@example.com", "Alice CV", "uploads/alice.html", now, now, null);
        when(cvService.listCvs()).thenReturn(List.of(cv));
        when(cvService.searchCvs("alice")).thenReturn(List.of(cv));
        when(cvService.getCv(10L)).thenReturn(cv);
        when(cvService.getUploadedHtml(10L)).thenReturn("<html>Alice</html>");
        when(cvService.buildLegacyPreview("CV:\nAlice")).thenReturn("<html><body>Alice</body></html>");

        MockMultipartFile html = new MockMultipartFile(
                "file",
                "alice.html",
                MediaType.TEXT_HTML_VALUE,
                "<html>Alice</html>".getBytes());
        when(cvService.uploadHtmlCv(eq(2L), eq("Uploaded"), any())).thenReturn(cv);

        mockMvc.perform(get("/api/cvs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Alice CV"));

        mockMvc.perform(get("/api/cvs/search").param("q", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));

        mockMvc.perform(get("/api/cvs/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerEmail").value("alice@example.com"));

        mockMvc.perform(get("/api/cvs/10/html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<html>Alice</html>"));

        mockMvc.perform(post("/api/cvs/legacy-preview")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("CV:\nAlice"))
                .andExpect(status().isOk())
                .andExpect(content().string("<html><body>Alice</body></html>"));

        mockMvc.perform(multipart("/api/cvs/upload")
                        .file(html)
                        .param("ownerUserId", "2")
                        .param("title", "Uploaded"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Alice CV"));
    }

    @Test
    void cvControllerDelegatesCreateUpdateAndValidationFailures() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        CvResponse cv = new CvResponse(11L, 2L, "alice@example.com", "New CV", "uploads/new.html", now, now, null);
        when(cvService.createCv(any(CvCreateRequest.class))).thenReturn(cv);
        when(cvService.updateCv(eq(11L), any(CvUpdateRequest.class))).thenReturn(cv);

        mockMvc.perform(post("/api/cvs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "ownerUserId", 2,
                                "title", "New CV",
                                "uploadedHtmlFilePath", "uploads/new.html"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11));

        mockMvc.perform(put("/api/cvs/11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "New CV",
                                "uploadedHtmlFilePath", "uploads/new.html"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New CV"));

        mockMvc.perform(post("/api/cvs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "ownerUserId", 2,
                                "title", "",
                                "uploadedHtmlFilePath", "uploads/new.html"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void adminUserAiAndHealthControllersDelegate() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        when(adminSettingsService.listSettings()).thenReturn(List.of(
                new AppSettingResponse("application.displayName", "CV Manager", "Shown to users")));
        when(adminSettingsService.updateSetting(eq("application.displayName"), any(AppSettingUpdateRequest.class)))
                .thenReturn(new AppSettingResponse("application.displayName", "Hiring CV Manager", "Shown to users"));
        when(userService.listUsers()).thenReturn(List.of(new UserResponse(2L, "alice@example.com", "Alice", false, now)));
        when(userService.getUser(2L)).thenReturn(new UserResponse(2L, "alice@example.com", "Alice", false, now));
        AiSuggestionResponse suggestion = new AiSuggestionResponse(
                5L,
                10L,
                AiActionType.IMPROVE_SUMMARY,
                "before",
                "after",
                "PENDING",
                now);
        when(aiApplicationService.improveSummary(10L)).thenReturn(suggestion);
        when(aiApplicationService.listSuggestions(10L)).thenReturn(List.of(suggestion));

        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("application.displayName"));

        mockMvc.perform(put("/api/admin/settings/application.displayName")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("value", "Hiring CV Manager"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("Hiring CV Manager"));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("alice@example.com"));

        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice"));

        mockMvc.perform(post("/api/cvs/10/ai-actions/improve-summary"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actionType").value("IMPROVE_SUMMARY"));

        mockMvc.perform(get("/api/cvs/10/ai-actions/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("cv-manager-backend"));
    }

    @Test
    void globalExceptionHandlerMapsApplicationAndUnexpectedErrors() throws Exception {
        when(cvService.getCv(404L)).thenThrow(new NotFoundException("CV not found", "CV_NOT_FOUND"));
        when(cvService.getCv(400L)).thenThrow(new BadRequestException("Bad CV", "CV_BAD"));

        mockMvc.perform(get("/api/cvs/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CV_NOT_FOUND"));

        mockMvc.perform(get("/api/cvs/400"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CV_BAD"));

        mockMvc.perform(get("/api/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @org.springframework.web.bind.annotation.RestController
    private static class ThrowingController {

        @org.springframework.web.bind.annotation.GetMapping("/api/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("boom");
        }
    }
}
