package com.example.cvmanager.common.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.cvmanager.admin.service.AdminProperties;
import com.example.cvmanager.common.exception.ForbiddenException;
import com.example.cvmanager.common.exception.UnauthorizedException;
import com.example.cvmanager.user.model.UserAccount;
import com.example.cvmanager.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminAccessServiceTest {

    private UserRepository userRepository;
    private AdminAccessService adminAccessService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        adminAccessService = new AdminAccessService(
                userRepository,
                new AsIsSecurityProperties("demo-token"),
                new AdminProperties("admin@example.com", "admin123"));
    }

    @Test
    void requireAdminAcceptsAdminToken() {
        UserAccount adminUser = new UserAccount("admin@example.com", "Admin User", "hash", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertDoesNotThrow(() -> adminAccessService.requireAdmin("Bearer demo-token-1"));
    }

    @Test
    void requireAdminRejectsMissingAuthorizationHeader() {
        assertThrows(UnauthorizedException.class, () -> adminAccessService.requireAdmin(null));
    }

    @Test
    void requireAdminRejectsNonAdminUser() {
        UserAccount nonAdminUser = new UserAccount("alice@example.com", "Alice Student", "hash", false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(nonAdminUser));

        assertThrows(ForbiddenException.class, () -> adminAccessService.requireAdmin("Bearer demo-token-2"));
    }
}
