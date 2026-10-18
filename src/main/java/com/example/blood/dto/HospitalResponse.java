package com.example.blood.dto;

import com.example.blood.model.HospitalStatus;

import java.time.Instant;

public record HospitalResponse(
        String id,
        String ownerUserId,
        String name,
        String registrationNumber,
        String contactPerson,
        String address,
        String city,
        String state,
        String pincode,
        String phone,
        Double latitude,
        Double longitude,
        HospitalStatus status,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
}
