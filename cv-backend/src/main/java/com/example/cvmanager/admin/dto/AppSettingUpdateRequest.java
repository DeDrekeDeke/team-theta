package com.example.cvmanager.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppSettingUpdateRequest(
    @NotBlank(message = "Value is required")
    @Size(max = 1000, message = "Value must be 1000 characters or fewer")
    String value
) {}
