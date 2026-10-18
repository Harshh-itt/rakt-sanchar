package com.example.blood.event;

import com.example.blood.model.BadgeCode;

import java.time.Instant;

public record BadgeEarnedEvent(
        String donorUserId,
        BadgeCode badgeCode,
        String badgeTitle,
        Instant occurredAt
) {
}
