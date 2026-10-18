package com.example.blood.dto;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.RequestStatus;
import com.example.blood.model.Urgency;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BloodRequestResponse(
        String id,
        String requesterUserId,
        String patientName,
        BloodGroup bloodGroup,
        int unitsNeeded,
        int unitsFulfilled,
        String hospitalId,
        String hospital,
        String city,
        Double hospitalLatitude,
        Double hospitalLongitude,
        Urgency urgency,
        RequestStatus status,
        LocalDate neededBy,
        List<BloodRequestDonorResponseDto> donorResponses,
        Instant createdAt,
        Instant updatedAt
) {
}
