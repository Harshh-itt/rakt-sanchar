package com.example.blood.dto;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserOnboardingRequest(
        @NotNull(message = "Role is required")
        Role role,

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Pattern(regexp = "^(en|hi)$", message = "Preferred language must be 'en' or 'hi'")
        String preferredLanguage,

        // Optional donor onboarding fields (populated when onboarding as DONOR)
        BloodGroup bloodGroup,
        LocalDate dateOfBirth,
        String phone,
        String city,
        String pincode,
        Double latitude,
        Double longitude,
        LocalDate lastDonationDate,
        Boolean medicalConditionsConfirmed,
        Boolean available,

        // Optional hospital onboarding fields (populated when onboarding as HOSPITAL)
        String hospitalName,
        String registrationNumber,
        String contactPerson,
        String address
) {
}
