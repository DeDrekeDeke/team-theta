package com.example.cvmanager.auth.security;

import com.example.cvmanager.auth.config.AuthProperties;
import com.example.cvmanager.user.model.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ADMIN_CLAIM = "admin";
    private static final String DISPLAY_NAME_CLAIM = "displayName";
    private static final String EMAIL_CLAIM = "email";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private final AuthProperties authProperties;
    private final SecretKey signingKey;

    public JwtService(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.signingKey = Keys.hmacShaKeyFor(authProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(UserAccount user, boolean admin) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(authProperties.accessTokenTtlMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .issuer(authProperties.issuer())
                .subject(String.valueOf(user.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .claim(EMAIL_CLAIM, user.getEmail())
                .claim(DISPLAY_NAME_CLAIM, user.getDisplayName())
                .claim(ADMIN_CLAIM, admin)
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = parseToken(token, ACCESS_TOKEN_TYPE);
        return new AuthenticatedUser(
                Long.parseLong(claims.getSubject()),
                claims.get(EMAIL_CLAIM, String.class),
                claims.get(DISPLAY_NAME_CLAIM, String.class),
                Boolean.TRUE.equals(claims.get(ADMIN_CLAIM, Boolean.class)));
    }

    private Claims parseToken(String token, String expectedTokenType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(authProperties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            if (!expectedTokenType.equals(tokenType)) {
                throw new JwtException("Invalid token type");
            }
            return claims;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new JwtException("Invalid authentication token", exception);
        }
    }
}
