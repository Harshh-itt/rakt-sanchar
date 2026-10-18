package com.example.blood.dto;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.DonorResponseStatus;

import java.time.Instant;

public record MatchedDonorResponse(
        String donorProfileId,
        String donorUserId,
        String name,
        BloodGroup bloodGroup,
        String city,
        String phone,
        boolean exactMatch,
        Double distanceKm,
        boolean available,
        boolean eligible,
        DonorResponseStatus responseStatus,
        Instant respondedAt
) {
}
