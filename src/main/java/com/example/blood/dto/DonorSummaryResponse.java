package com.example.blood.dto;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.EarnedBadge;

import java.time.LocalDate;
import java.util.List;

public record DonorSummaryResponse(
        String donorUserId,
        String donorName,
        BloodGroup bloodGroup,
        int totalVerifiedDonations,
        int totalVerifiedUnits,
        int pendingClaimsCount,
        LocalDate lastDonationDate,
        boolean eligible,
        LocalDate nextEligibleDate,
        String eligibilityReason,
        List<EarnedBadge> badges
) {
}
