package com.example.blood.dto;

import com.example.blood.model.DonationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DonationVerificationRequest(
        @NotNull(message = "Verification status is required")
        DonationStatus status,

        @Size(max = 300, message = "Rejection reason must not exceed 300 characters")
        String rejectionReason
) {
}
