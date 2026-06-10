package com.example.cvmanager.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String issuer,
        String secret,
        long accessTokenTtlMinutes) {

    public AuthProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "cv-manager";
        }
        if (secret == null || secret.isBlank()) {
            secret = "change-this-development-secret-key-to-at-least-32-bytes";
        }
        if (accessTokenTtlMinutes <= 0) {
            accessTokenTtlMinutes = 15;
        }
    }
}
