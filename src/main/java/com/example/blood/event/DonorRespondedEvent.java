package com.example.blood.event;

import com.example.blood.model.DonorResponseStatus;

import java.time.Instant;

public record DonorRespondedEvent(
        String requestId,
        String requesterUserId,
        String hospitalId,
        String donorUserId,
        String donorProfileId,
        String donorName,
        DonorResponseStatus status,
        Instant occurredAt
) {
}
