package com.example.blood.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HospitalProfileUpdateRequest(
        @NotBlank(message = "Hospital name is required")
        @Size(max = 150, message = "Hospital name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Registration number is required")
        @Size(max = 80, message = "Registration number must not exceed 80 characters")
        String registrationNumber,

        @Size(max = 100, message = "Contact person name must not exceed 100 characters")
        String contactPerson,

        @NotBlank(message = "Address is required")
        @Size(max = 250, message = "Address must not exceed 250 characters")
        String address,

        @NotBlank(message = "City is required")
        @Size(max = 80, message = "City must not exceed 80 characters")
        String city,

        @Size(max = 80, message = "State must not exceed 80 characters")
        String state,

        @Size(max = 12, message = "Pincode must not exceed 12 characters")
        String pincode,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^(\\+91[\\-\\s]?)?[0]?(91)?[6789]\\d{9}$", message = "Phone must be a valid 10-digit Indian mobile number")
        String phone,

        Double latitude,
        Double longitude
) {
}
