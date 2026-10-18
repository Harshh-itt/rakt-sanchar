package com.example.blood.event;

import com.example.blood.model.HospitalStatus;

public record HospitalVerificationEvent(
        String hospitalId,
        String ownerUserId,
        String hospitalName,
        HospitalStatus status,
        String rejectionReason
) {
}
