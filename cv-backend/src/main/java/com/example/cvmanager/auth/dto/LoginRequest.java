package com.example.cvmanager.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank 
        @Email @Size(max = 255) 
        String email,

        @NotBlank 
        @Size(min = 6, max = 255) 
        String password
) {}
