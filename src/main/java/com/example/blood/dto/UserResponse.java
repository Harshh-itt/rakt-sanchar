package com.example.blood.dto;

import com.example.blood.model.Role;

import java.time.Instant;

public record UserResponse(
        String id,
        String name,
        Role role,
        boolean onboarded,
        String preferredLanguage,
        Instant createdAt
) {
}
