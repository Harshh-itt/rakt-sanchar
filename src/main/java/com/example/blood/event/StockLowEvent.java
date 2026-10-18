package com.example.blood.event;

import com.example.blood.model.BloodGroup;

public record StockLowEvent(
        String hospitalId,
        String hospitalName,
        String city,
        Double latitude,
        Double longitude,
        BloodGroup bloodGroup,
        int units,
        int minThreshold
) {
}
