package com.example.blood.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BloodStockUpdateRequest(
        @NotNull(message = "Units count is required")
        @Min(value = 0, message = "Units must be >= 0")
        Integer units,

        @Min(value = 0, message = "Minimum threshold must be >= 0")
        Integer minThreshold
) {
}
