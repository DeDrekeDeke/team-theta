package com.example.cvmanager.auth.security;

public record AuthenticatedUser(
        Long userId,
        String email,
        String displayName,
        boolean admin) {
}
