package com.example.blood.event;

import java.time.Instant;
import java.time.LocalDate;

public record DonationVerifiedEvent(
        String donationId,
        String donorUserId,
        String hospitalId,
        String hospitalName,
        String requestId,
        int units,
        LocalDate donatedAt,
        Instant occurredAt
) {
}
