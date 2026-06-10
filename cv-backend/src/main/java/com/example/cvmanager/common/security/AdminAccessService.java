package com.example.cvmanager.common.security;

import com.example.cvmanager.admin.service.AdminProperties;
import com.example.cvmanager.common.exception.ForbiddenException;
import com.example.cvmanager.common.exception.UnauthorizedException;
import com.example.cvmanager.user.model.UserAccount;
import com.example.cvmanager.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccessService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final AsIsSecurityProperties securityProperties;
    private final AdminProperties adminProperties;

    public AdminAccessService(
            UserRepository userRepository,
            AsIsSecurityProperties securityProperties,
            AdminProperties adminProperties) {
        this.userRepository = userRepository;
        this.securityProperties = securityProperties;
        this.adminProperties = adminProperties;
    }

    @Transactional(readOnly = true)
    public void requireAdmin(String authorizationHeader) {
        UserAccount user = authenticate(authorizationHeader);
        if (!user.isAdmin() && !adminProperties.email().equalsIgnoreCase(user.getEmail())) {
            throw new ForbiddenException("Admin access is required", "ADMIN_REQUIRED");
        }
    }

    private UserAccount authenticate(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new UnauthorizedException("Authorization header is required", "AUTH_REQUIRED");
        }

        if (!authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new UnauthorizedException("Authorization header must use Bearer token", "AUTH_INVALID");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        String tokenPrefix = securityProperties.demoTokenPrefix() + "-";
        if (!token.startsWith(tokenPrefix)) {
            throw new UnauthorizedException("Invalid authentication token", "AUTH_INVALID");
        }

        long userId;
        try {
            userId = Long.parseLong(token.substring(tokenPrefix.length()));
        } catch (NumberFormatException exception) {
            throw new UnauthorizedException("Invalid authentication token", "AUTH_INVALID");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Invalid authentication token", "AUTH_INVALID"));
    }
}
