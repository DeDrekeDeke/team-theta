package com.example.cvmanager.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cvmanager.admin.dto.AppSettingResponse;
import com.example.cvmanager.admin.dto.AppSettingUpdateRequest;
import com.example.cvmanager.admin.service.AdminSettingsService;
import com.example.cvmanager.auth.security.SecurityConfig;
import com.example.cvmanager.common.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminSettingsController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AdminSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminSettingsService adminSettingsService;

    @Test
    void listSettingsReturnsConfiguredSettings() throws Exception {
        when(adminSettingsService.listSettings()).thenReturn(List.of(
                new AppSettingResponse("application.displayName", "CV Manager", "Display name")));

        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("application.displayName"))
                .andExpect(jsonPath("$[0].value").value("CV Manager"));
    }

    @Test
    void updateSettingReturnsUpdatedSetting() throws Exception {
        when(adminSettingsService.updateSetting(eq("application.displayName"), any(AppSettingUpdateRequest.class)))
                .thenReturn(new AppSettingResponse("application.displayName", "Candidate Portal", "Display name"));

        mockMvc.perform(put("/api/admin/settings/application.displayName")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "value": "Candidate Portal"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("application.displayName"))
                .andExpect(jsonPath("$.value").value("Candidate Portal"));
    }

    @Test
    void updateSettingRejectsBlankValueBeforeCallingService() throws Exception {
        mockMvc.perform(put("/api/admin/settings/application.displayName")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "value": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(adminSettingsService);
    }
}
