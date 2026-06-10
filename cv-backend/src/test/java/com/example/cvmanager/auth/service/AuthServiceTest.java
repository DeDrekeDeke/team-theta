package com.example.cvmanager.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.cvmanager.admin.service.AdminProperties;
import com.example.cvmanager.auth.dto.LoginRequest;
import com.example.cvmanager.common.exception.BadRequestException;
import com.example.cvmanager.common.security.AsIsSecurityProperties;
import com.example.cvmanager.user.model.UserAccount;
import com.example.cvmanager.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthService authService = new AuthService(
            userRepository,
            new AsIsSecurityProperties("demo-token"),
            new AdminProperties("admin@example.com", "admin123"));

    @Test
    void loginReturnsDemoTokenForValidCredentials() {
        UserAccount user = user(2L, "alice@example.com", "Alice Student", "user123", false);
        when(userRepository.findByEmailIgnoreCase("ALICE@example.com")).thenReturn(Optional.of(user));

        var response = authService.login(new LoginRequest("ALICE@example.com", "user123"));

        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.displayName()).isEqualTo("Alice Student");
        assertThat(response.admin()).isFalse();
        assertThat(response.token()).isEqualTo("demo-token-2");
    }

    @Test
    void loginMarksConfiguredAdminEmailAsAdmin() {
        UserAccount admin = user(1L, "ADMIN@example.com", "Admin", "admin123", false);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        var response = authService.login(new LoginRequest("admin@example.com", "admin123"));

        assertThat(response.admin()).isTrue();
    }

    @Test
    void loginRejectsUnknownEmailAndWrongPasswordWithSameCode() {
        UserAccount user = user(2L, "alice@example.com", "Alice Student", "user123", false);
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@example.com", "user123")))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("AUTH_INVALID"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrong")))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("AUTH_INVALID"));
    }

    private UserAccount user(Long id, String email, String displayName, String password, boolean admin) {
        UserAccount user = new UserAccount(email, displayName, password, admin);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
