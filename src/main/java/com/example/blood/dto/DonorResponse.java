package com.example.blood.dto;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.EarnedBadge;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DonorResponse(
        String id,
        String userId,
        String name,
        BloodGroup bloodGroup,
        String city,
        String pincode,
        String phone,
        Integer age,
        LocalDate dateOfBirth,
        Double latitude,
        Double longitude,
        boolean available,
        boolean medicalConditionsConfirmed,
        LocalDate declaredLastDonationDate,
        boolean eligible,
        LocalDate nextEligibleDate,
        String eligibilityReason,
        List<EarnedBadge> badges,
        Instant createdAt
) {
}
