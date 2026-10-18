package com.example.blood.dto;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.RequestStatus;
import com.example.blood.model.Urgency;

import java.time.Instant;
import java.time.LocalDate;

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
        Urgency urgency,
        RequestStatus status,
        LocalDate neededBy,
        Instant createdAt,
        Instant updatedAt
) {
}
