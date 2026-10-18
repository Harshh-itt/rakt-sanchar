package com.example.blood.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LanguageUpdateRequest(
        @NotBlank(message = "Preferred language is required")
        @Pattern(regexp = "^(en|hi)$", message = "Preferred language must be 'en' or 'hi'")
        String preferredLanguage
) {
}
