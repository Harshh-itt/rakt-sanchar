package com.example.blood.dto;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.Urgency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BloodRequestCreateRequest(
        @NotBlank(message = "Patient name is required")
        @Size(max = 100, message = "Patient name must not exceed 100 characters")
        String patientName,

        @NotNull(message = "Blood group is required")
        BloodGroup bloodGroup,

        @Min(value = 1, message = "Units needed must be at least 1")
        @Max(value = 20, message = "Units needed must not exceed 20")
        Integer unitsNeeded,

        String hospitalId,

        @Size(max = 150, message = "Hospital name must not exceed 150 characters")
        String hospital,

        @Size(max = 80, message = "City must not exceed 80 characters")
        String city,

        @NotNull(message = "Urgency is required")
        Urgency urgency,

        LocalDate neededBy
) {
}
