package com.example.blood.dto;

import com.example.blood.model.BloodGroup;

import java.time.Instant;

public record BloodStockResponse(
        String id,
        String hospitalId,
        String hospitalName,
        String city,
        String phone,
        BloodGroup bloodGroup,
        int units,
        int minThreshold,
        boolean lowStock,
        boolean stale,
        Double distanceKm,
        Instant updatedAt
) {
}
