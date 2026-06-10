package com.example.cvmanager.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.cvmanager.admin.dto.AppSettingUpdateRequest;
import com.example.cvmanager.admin.model.AppSetting;
import com.example.cvmanager.admin.repository.AppSettingRepository;
import com.example.cvmanager.common.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class AdminSettingsServiceTest {

    private final AppSettingRepository repository = mock(AppSettingRepository.class);
    private final AdminSettingsService service = new AdminSettingsService(repository);

    @Test
    void listSettingsMapsSettingsSortedByKey() {
        when(repository.findAll(Sort.by("key"))).thenReturn(List.of(
                new AppSetting("application.displayName", "CV Manager", "Shown to users")));

        var settings = service.listSettings();

        assertThat(settings).hasSize(1);
        assertThat(settings.get(0).key()).isEqualTo("application.displayName");
        assertThat(settings.get(0).value()).isEqualTo("CV Manager");
    }

    @Test
    void updateSettingPersistsNewValue() {
        AppSetting setting = new AppSetting("application.displayName", "CV Manager", "Shown to users");
        when(repository.findById("application.displayName")).thenReturn(Optional.of(setting));
        when(repository.save(setting)).thenReturn(setting);

        var response = service.updateSetting("application.displayName", new AppSettingUpdateRequest("Hiring CV Manager"));

        assertThat(response.value()).isEqualTo("Hiring CV Manager");
        verify(repository).save(setting);
    }

    @Test
    void updateSettingThrowsWhenMissing() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSetting("missing", new AppSettingUpdateRequest("value")))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("SETTING_NOT_FOUND"));
    }
}
