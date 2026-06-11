package com.example.cvmanager.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
         @NotBlank(message = "Email is required")
        @Size(max = 100, message = "Email must be 100 characters or fewer")
        @Email(message = "Enter a valid email address")
        String email,

        @NotBlank(message = "Display name is required")
        @Size(max = 100, message = "Display name must be 100 characters or fewer")
        String displayName,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        @Pattern(regexp = ".*\\S.*", message = "Password must contain at least one non-whitespace character")
        String password,
        boolean admin) {}
