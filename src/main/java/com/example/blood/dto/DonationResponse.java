package com.example.blood.dto;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.DonationStatus;

import java.time.Instant;
import java.time.LocalDate;

public record DonationResponse(
        String id,
        String donorUserId,
        String donorName,
        String hospitalId,
        String hospitalName,
        BloodGroup bloodGroup,
        int units,
        LocalDate donatedAt,
        DonationStatus status,
        String requestId,
        String rejectionReason,
        Instant createdAt,
        Instant verifiedAt,
        String verifiedByUserId
) {
}
