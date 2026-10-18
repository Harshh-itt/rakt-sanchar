package com.example.blood.dto;

import com.example.blood.model.NotificationType;

import java.time.Instant;
import java.util.Map;

public record NotificationResponse(
        String id,
        String userId,
        NotificationType type,
        String messageKey,
        Map<String, String> params,
        String message,
        boolean read,
        Instant createdAt
) {
}
