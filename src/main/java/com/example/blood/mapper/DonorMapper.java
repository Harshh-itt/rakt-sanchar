package com.example.blood.mapper;

import com.example.blood.dto.DonorResponse;
import com.example.blood.model.DonorProfile;
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
            boolean revealPhone,
            boolean eligible,
            LocalDate nextEligibleDate,
            String eligibilityReason
    ) {
        return new DonorResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getName(),
                profile.getBloodGroup(),
                profile.getCity(),
                revealPhone ? profile.getPhone() : maskPhone(profile.getPhone()),
                profile.getAge(),
                revealPhone ? profile.getDateOfBirth() : null,
                profile.getLatitude(),
                profile.getLongitude(),
                profile.isAvailable(),
                profile.getDeclaredLastDonationDate(),
                eligible,
                nextEligibleDate,
                eligibilityReason,
                profile.getBadges() != null ? List.copyOf(profile.getBadges()) : List.of(),
                profile.getCreatedAt()
        );
    }
}
