package com.example.cvmanager;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cvmanager.admin.dto.AppSettingResponse;
import com.example.cvmanager.admin.dto.AppSettingUpdateRequest;
import com.example.cvmanager.admin.model.AppSetting;
import com.example.cvmanager.admin.service.AdminProperties;
import com.example.cvmanager.ai.dto.AiSuggestionResponse;
import com.example.cvmanager.ai.model.AiActionType;
import com.example.cvmanager.ai.model.AiSuggestion;
import com.example.cvmanager.auth.dto.LoginRequest;
import com.example.cvmanager.auth.dto.LoginResponse;
import com.example.cvmanager.common.exception.BadRequestException;
import com.example.cvmanager.common.exception.ErrorResponse;
import com.example.cvmanager.common.exception.NotFoundException;
import com.example.cvmanager.common.pagination.PageResponse;
import com.example.cvmanager.common.security.AsIsSecurityProperties;
import com.example.cvmanager.common.validation.ValidationGroups;
import com.example.cvmanager.cv.dto.CvCreateRequest;
import com.example.cvmanager.cv.dto.CvResponse;
import com.example.cvmanager.cv.dto.CvUpdateRequest;
import com.example.cvmanager.cv.model.Cv;
import com.example.cvmanager.cv.service.CvStorageProperties;
import com.example.cvmanager.user.dto.UserResponse;
import com.example.cvmanager.user.model.UserAccount;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ValueObjectAndModelTest {

    @Test
    void propertyRecordsApplyDefaults() {
        assertThat(new AsIsSecurityProperties(null).demoTokenPrefix()).isEqualTo("demo-token");
        assertThat(new AsIsSecurityProperties("custom").demoTokenPrefix()).isEqualTo("custom");
        assertThat(new AdminProperties("", null))
                .isEqualTo(new AdminProperties("admin@example.com", "admin123"));
        assertThat(new CvStorageProperties(" ").uploadDir()).isEqualTo("uploads");
        assertThat(new CvStorageProperties("custom-uploads").uploadDir()).isEqualTo("custom-uploads");
    }

    @Test
    void responseAndRequestRecordsExposeValues() {
        LocalDateTime now = LocalDateTime.now();
        CvResponse cv = new CvResponse(1L, 2L, "alice@example.com", "Title", "file.html", now, now, null);
        UserResponse user = new UserResponse(2L, "alice@example.com", "Alice", false, now);
        AiSuggestionResponse suggestion = new AiSuggestionResponse(
                3L,
                1L,
                AiActionType.IMPROVE_SUMMARY,
                "before",
                "after",
                "PENDING",
                now);
        PageResponse<String> page = new PageResponse<>(List.of("one"), 0, 10, 1, 1);

        assertThat(new LoginRequest("alice@example.com", "user123").email()).isEqualTo("alice@example.com");
        assertThat(new LoginResponse(2L, "alice@example.com", "Alice", false, "token").token()).isEqualTo("token");
        assertThat(new CvCreateRequest(2L, "Title", "file.html").ownerUserId()).isEqualTo(2L);
        assertThat(new CvUpdateRequest("Title", "file.html").title()).isEqualTo("Title");
        assertThat(cv.ownerEmail()).isEqualTo("alice@example.com");
        assertThat(user.displayName()).isEqualTo("Alice");
        assertThat(new AppSettingResponse("key", "value", "description").key()).isEqualTo("key");
        assertThat(new AppSettingUpdateRequest("value").value()).isEqualTo("value");
        assertThat(suggestion.actionType()).isEqualTo(AiActionType.IMPROVE_SUMMARY);
        assertThat(page.items()).containsExactly("one");
        assertThat(AiActionType.values()).contains(AiActionType.EVALUATE_FIT);
        assertThat(ValidationGroups.Create.class).isInterface();
        assertThat(ValidationGroups.Update.class).isInterface();
    }

    @Test
    void exceptionResponsesExposeCodesAndDetails() {
        ErrorResponse response = ErrorResponse.of("Denied", "DENIED", List.of("detail"));

        assertThat(response.code()).isEqualTo("DENIED");
        assertThat(response.details()).containsExactly("detail");
        assertThat(response.timestamp()).isNotNull();
        assertThat(new BadRequestException("Bad", "BAD").getCode()).isEqualTo("BAD");
        assertThat(new NotFoundException("Missing", "MISSING").getCode()).isEqualTo("MISSING");
    }

    @Test
    void entityModelsExposeMutableFieldsAndLifecycleDefaults() {
        UserAccount alice = new UserAccount("alice@example.com", "Alice", "user123", false);
        alice.setEmail("alice.changed@example.com");
        alice.setDisplayName("Alice Changed");
        alice.setPassword("new-password");
        alice.setAdmin(true);
        ReflectionTestUtils.invokeMethod(alice, "onCreate");

        UserAccount bob = new UserAccount("bob@example.com", "Bob", "user123", false);
        ReflectionTestUtils.setField(bob, "id", 3L);

        Cv cv = new Cv(alice, "Alice CV", "uploads/alice.html");
        cv.setOwner(bob);
        cv.setTitle("Bob CV");
        cv.setUploadedHtmlFilePath("uploads/bob.html");
        ReflectionTestUtils.invokeMethod(cv, "onCreate");
        LocalDateTime createdAt = cv.getCreatedAt();
        ReflectionTestUtils.invokeMethod(cv, "onUpdate");

        AppSetting setting = new AppSetting("application.displayName", "CV Manager", "Shown to users");
        setting.setValue("Hiring CV Manager");

        AiSuggestion suggestion = new AiSuggestion(cv, AiActionType.IMPROVE_SUMMARY, "before", "after", "PENDING");
        ReflectionTestUtils.invokeMethod(suggestion, "onCreate");

        assertThat(alice.getEmail()).isEqualTo("alice.changed@example.com");
        assertThat(alice.getDisplayName()).isEqualTo("Alice Changed");
        assertThat(alice.getPassword()).isEqualTo("new-password");
        assertThat(alice.isAdmin()).isTrue();
        assertThat(alice.getCreatedAt()).isNotNull();
        assertThat(cv.getOwner()).isSameAs(bob);
        assertThat(cv.getTitle()).isEqualTo("Bob CV");
        assertThat(cv.getUploadedHtmlFilePath()).isEqualTo("uploads/bob.html");
        assertThat(createdAt).isNotNull();
        assertThat(cv.getUpdatedAt()).isNotNull();
        assertThat(setting.getKey()).isEqualTo("application.displayName");
        assertThat(setting.getValue()).isEqualTo("Hiring CV Manager");
        assertThat(setting.getDescription()).isEqualTo("Shown to users");
        assertThat(suggestion.getCv()).isSameAs(cv);
        assertThat(suggestion.getOriginalText()).isEqualTo("before");
        assertThat(suggestion.getSuggestedText()).isEqualTo("after");
        assertThat(suggestion.getStatus()).isEqualTo("PENDING");
        assertThat(suggestion.getCreatedAt()).isNotNull();
    }
}
