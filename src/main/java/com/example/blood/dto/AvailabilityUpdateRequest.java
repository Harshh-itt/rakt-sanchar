package com.example.blood.dto;

import jakarta.validation.constraints.NotNull;

public record AvailabilityUpdateRequest(
        @NotNull(message = "Availability flag is required")
        Boolean available
) {
}
