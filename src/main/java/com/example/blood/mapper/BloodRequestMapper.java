package com.example.blood.mapper;

import com.example.blood.dto.BloodRequestResponse;
import com.example.blood.model.BloodRequest;
import org.springframework.stereotype.Component;

@Component
public class BloodRequestMapper {

    public BloodRequestResponse toResponse(BloodRequest request) {
        return new BloodRequestResponse(
                request.getId(),
                request.getRequesterUserId(),
                request.getPatientName(),
                request.getBloodGroupEnum(),
                request.getUnitsNeeded(),
                request.getUnitsFulfilled(),
                request.getHospitalId(),
                request.getHospital(),
                request.getCity(),
                request.getUrgencyEnum(),
                request.getStatus(),
                request.getNeededBy(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
