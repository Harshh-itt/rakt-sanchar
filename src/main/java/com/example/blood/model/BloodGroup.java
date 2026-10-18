package com.example.blood.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum BloodGroup {
    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-");

    private final String label;

    BloodGroup(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static BloodGroup fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Blood group is required");
        }
        String normalized = value.trim().toUpperCase();
        for (BloodGroup bg : values()) {
            if (bg.label.equalsIgnoreCase(normalized) || bg.name().equalsIgnoreCase(normalized)) {
                return bg;
            }
        }
        throw new IllegalArgumentException(
                "Invalid blood group '" + value + "'. Allowed values: " +
                        Arrays.stream(values()).map(BloodGroup::getLabel).toList()
        );
    }

    @Override
    public String toString() {
        return label;
    }
}
