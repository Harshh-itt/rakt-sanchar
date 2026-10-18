package com.example.blood.mapper;

import com.example.blood.dto.DonorResponse;
import com.example.blood.model.DonorProfile;
import com.example.blood.service.EligibilityService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DonorMapper {

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String trimmed = phone.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "******" + trimmed.substring(trimmed.length() - 4);
    }

    public DonorResponse toResponse(
            DonorProfile profile,
            boolean revealPrivateFields,
            EligibilityService.EligibilityStatus eligibility
    ) {
        return toResponse(
                profile,
                revealPrivateFields,
                eligibility.eligible(),
                eligibility.nextEligibleDate(),
                eligibility.reason(),
                eligibility.ageYears(),
                eligibility.effectiveLastDonationDate()
        );
    }

    public DonorResponse toResponse(
            DonorProfile profile,
            boolean revealPrivateFields,
            boolean eligible,
            LocalDate nextEligibleDate,
            String eligibilityReason
    ) {
        return toResponse(
                profile,
                revealPrivateFields,
                eligible,
                nextEligibleDate,
                eligibilityReason,
                profile.getAge(),
                profile.getDeclaredLastDonationDate()
        );
    }

    public DonorResponse toResponse(
            DonorProfile profile,
            boolean revealPrivateFields,
            boolean eligible,
            LocalDate nextEligibleDate,
            String eligibilityReason,
            Integer resolvedAge,
            LocalDate effectiveLastDonationDate
    ) {
        return new DonorResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getName(),
                profile.getBloodGroup(),
                profile.getCity(),
                profile.getPincode(),
                revealPrivateFields ? profile.getPhone() : maskPhone(profile.getPhone()),
                resolvedAge != null ? resolvedAge : profile.getAge(),
                revealPrivateFields ? profile.getDateOfBirth() : null,
                revealPrivateFields ? profile.getLatitude() : null,
                revealPrivateFields ? profile.getLongitude() : null,
                profile.isAvailable(),
                profile.isMedicalConditionsConfirmed(),
                effectiveLastDonationDate != null ? effectiveLastDonationDate : profile.getDeclaredLastDonationDate(),
                eligible,
                nextEligibleDate,
                eligibilityReason,
                profile.getBadges() != null ? List.copyOf(profile.getBadges()) : List.of(),
                profile.getCreatedAt()
        );
    }
}
