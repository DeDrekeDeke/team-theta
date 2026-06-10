package com.example.cvmanager.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.cvmanager.admin.dto.AppSettingUpdateRequest;
import com.example.cvmanager.admin.model.AppSetting;
import com.example.cvmanager.admin.repository.AppSettingRepository;
import com.example.cvmanager.common.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;

class AdminSettingsServiceTest {

    private AppSettingRepository appSettingRepository;
    private AdminSettingsService adminSettingsService;

    @BeforeEach
    void setUp() {
        appSettingRepository = mock(AppSettingRepository.class);
        adminSettingsService = new AdminSettingsService(appSettingRepository);
    }

    @Test
    void listSettingsReturnsSettingsSortedByKey() {
        when(appSettingRepository.findAll(any(Sort.class))).thenReturn(List.of(
                new AppSetting("ai.mockProviderEnabled", "true", "Mock AI enabled"),
                new AppSetting("application.displayName", "CV Manager", "Display name")));

        var response = adminSettingsService.listSettings();

        assertEquals(2, response.size());
        assertEquals("ai.mockProviderEnabled", response.get(0).key());
        assertEquals("true", response.get(0).value());

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(appSettingRepository).findAll(sortCaptor.capture());
        assertEquals(Sort.Direction.ASC, sortCaptor.getValue().getOrderFor("key").getDirection());
    }

    @Test
    void updateSettingPersistsNewValue() {
        AppSetting setting = new AppSetting("application.displayName", "CV Manager", "Display name");
        when(appSettingRepository.findById("application.displayName")).thenReturn(Optional.of(setting));
        when(appSettingRepository.save(setting)).thenReturn(setting);

        var response = adminSettingsService.updateSetting(
                "application.displayName",
                new AppSettingUpdateRequest("Candidate Portal"));

        assertEquals("application.displayName", response.key());
        assertEquals("Candidate Portal", response.value());
        verify(appSettingRepository).save(setting);
    }

    @Test
    void updateSettingRejectsUnknownKey() {
        when(appSettingRepository.findById("missing")).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> adminSettingsService.updateSetting("missing", new AppSettingUpdateRequest("value")));

        assertEquals("SETTING_NOT_FOUND", exception.getCode());
    }
}
