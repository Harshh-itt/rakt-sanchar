package com.example.blood.dto;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.DonorResponseStatus;

import java.time.Instant;

public record BloodRequestDonorResponseDto(
        String donorUserId,
        String donorProfileId,
        String donorName,
        BloodGroup bloodGroup,
        String city,
        String phone,
        DonorResponseStatus status,
        Instant respondedAt
) {
}
