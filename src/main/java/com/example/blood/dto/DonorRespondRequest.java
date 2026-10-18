package com.example.blood.dto;

import com.example.blood.model.DonorResponseStatus;
import jakarta.validation.constraints.NotNull;

public record DonorRespondRequest(
        @NotNull(message = "Response status is required")
        DonorResponseStatus status
) {
}
