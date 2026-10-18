package com.example.blood.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DonorDonationClaimRequest(
        String hospitalId,

        @Size(max = 150, message = "Hospital name must not exceed 150 characters")
        String hospitalName,

        String requestId,

        @Min(value = 1, message = "Units must be at least 1")
        @Max(value = 5, message = "Units must not exceed 5")
        Integer units,

        @NotNull(message = "Donation date is required")
        LocalDate donatedAt
) {
}
