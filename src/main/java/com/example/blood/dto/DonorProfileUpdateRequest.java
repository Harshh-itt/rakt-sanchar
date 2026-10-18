package com.example.blood.dto;

import com.example.blood.model.BloodGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DonorProfileUpdateRequest(
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotNull(message = "Blood group is required")
        BloodGroup bloodGroup,

        @NotNull(message = "Date of birth is required")
        @PastOrPresent(message = "Date of birth cannot be in the future")
        LocalDate dateOfBirth,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^(\\+91[\\-\\s]?)?[0]?(91)?[6789]\\d{9}$", message = "Phone must be a valid 10-digit Indian mobile number")
        String phone,

        @NotBlank(message = "City is required")
        @Size(max = 80, message = "City must not exceed 80 characters")
        String city,

        @Size(max = 12, message = "Pincode must not exceed 12 characters")
        String pincode,

        Double latitude,
        Double longitude,

        @PastOrPresent(message = "Last donation date cannot be in the future")
        LocalDate declaredLastDonationDate,

        Boolean medicalConditionsConfirmed,
        Boolean available
) {
}
