package com.example.cvmanager.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.cvmanager.admin.service.AdminProperties;
import com.example.cvmanager.auth.dto.LoginRequest;
import com.example.cvmanager.common.exception.BadRequestException;
import com.example.cvmanager.common.security.AsIsSecurityProperties;
import com.example.cvmanager.user.model.UserAccount;
import com.example.cvmanager.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(
                userRepository,
                new AsIsSecurityProperties("demo-token"),
                new AdminProperties("admin@example.com", "admin123"),
                passwordEncoder);
    }

    @Test
    void loginAcceptsOriginalPasswordAgainstStoredHash() {
        UserAccount user = new UserAccount(
                "alice@example.com",
                "Alice Student",
                passwordEncoder.encode("user123"),
                false);

        when(userRepository.findByEmailIgnoreCase("alice@example.com"))
                .thenReturn(Optional.of(user));

        var response = authService.login(new LoginRequest("alice@example.com", "user123"));

        assertEquals("alice@example.com", response.email());
        assertEquals("Alice Student", response.displayName());
        assertFalse(response.admin());
    }

    @Test
    void loginRejectsWrongPassword() {
        UserAccount user = new UserAccount(
                "alice@example.com",
                "Alice Student",
                passwordEncoder.encode("user123"),
                false);

        when(userRepository.findByEmailIgnoreCase("alice@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                BadRequestException.class,
                () -> authService.login(new LoginRequest("alice@example.com", "wrong-password")));
    }
}
