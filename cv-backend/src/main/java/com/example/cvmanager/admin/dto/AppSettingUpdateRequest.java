package com.example.cvmanager.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppSettingUpdateRequest(
    @NotBlank 
    @Size(max = 1000) 
    String value
) {}
