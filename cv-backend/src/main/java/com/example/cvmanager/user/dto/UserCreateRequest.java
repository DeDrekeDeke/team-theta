package com.example.cvmanager.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank 
        @Email 
        @Size(max = 255) 
        String email,

        @NotBlank 
        @Size(max = 255) 
        String displayName,

        @NotBlank 
        @Size(min = 6, max = 255) 
        String password
) {}
