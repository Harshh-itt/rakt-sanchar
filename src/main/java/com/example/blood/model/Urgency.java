package com.example.blood.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Urgency {
    LOW("LOW", 1),
    MEDIUM("MEDIUM", 2),
    HIGH("HIGH", 3),
    URGENT("URGENT", 4),
    CRITICAL("CRITICAL", 5);

    private final String label;
    private final int priorityWeight;

    Urgency(String label, int priorityWeight) {
        this.label = label;
        this.priorityWeight = priorityWeight;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    public int getPriorityWeight() {
        return priorityWeight;
    }

    public boolean isEmergency() {
        return this == CRITICAL || this == URGENT || this == HIGH;
    }

    @JsonCreator
    public static Urgency fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Urgency is required");
        }
        String normalized = value.trim().toUpperCase();
        if ("NORMAL".equals(normalized) || "STANDARD".equals(normalized)) {
            return MEDIUM;
        }
        for (Urgency urgency : values()) {
            if (urgency.name().equalsIgnoreCase(normalized) || urgency.label.equalsIgnoreCase(normalized)) {
                return urgency;
            }
        }
        throw new IllegalArgumentException(
                "Invalid urgency '" + value + "'. Allowed values: " +
                        Arrays.stream(values()).map(Urgency::getLabel).toList()
        );
    }
}
