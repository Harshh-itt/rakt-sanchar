package com.example.blood.event;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.Urgency;

import java.time.Instant;

public record RequestCreatedEvent(
        String requestId,
        String requesterUserId,
        String hospitalId,
        String hospitalName,
        String patientName,
        BloodGroup bloodGroup,
        Urgency urgency,
        String city,
        Instant occurredAt
) {
}
