package com.example.blood.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record HospitalDonationRecordRequest(
        @NotBlank(message = "Donor userId is required")
        String donorUserId,

        String requestId,

        @Min(value = 1, message = "Units must be at least 1")
        @Max(value = 5, message = "Units must not exceed 5")
        Integer units,

        LocalDate donatedAt
) {
}
