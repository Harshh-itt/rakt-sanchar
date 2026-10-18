package com.example.blood.service;

import com.example.blood.dto.BloodRequestCreateRequest;
import com.example.blood.dto.BloodRequestResponse;
import com.example.blood.dto.PageResponse;
import com.example.blood.exception.ApiException;
import com.example.blood.exception.ErrorCode;
import com.example.blood.mapper.BloodRequestMapper;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.BloodRequest;
import com.example.blood.model.RequestStatus;
import com.example.blood.model.Urgency;
import com.example.blood.repository.BloodRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class BloodRequestService {

    private final BloodRequestRepository bloodRequestRepository;
    private final BloodRequestMapper bloodRequestMapper;

    public BloodRequestService(
            BloodRequestRepository bloodRequestRepository,
            BloodRequestMapper bloodRequestMapper
    ) {
        this.bloodRequestRepository = bloodRequestRepository;
        this.bloodRequestMapper = bloodRequestMapper;
    }

    public BloodRequestResponse createRequest(BloodRequestCreateRequest req) {
        if ((req.hospitalId() == null || req.hospitalId().isBlank())
                && (req.hospital() == null || req.hospital().isBlank())) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Either hospitalId or hospital name is required",
                    Map.of("fields", Map.of("hospital", "Hospital is required"))
            );
        }
        if ((req.city() == null || req.city().isBlank())
                && (req.hospitalId() == null || req.hospitalId().isBlank())) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "City is required when hospitalId is not provided",
                    Map.of("fields", Map.of("city", "City is required"))
            );
        }

        BloodRequest entity = new BloodRequest();
        entity.setPatientName(req.patientName().trim());
        entity.setBloodGroupEnum(req.bloodGroup());
        entity.setUnitsNeeded(req.unitsNeeded() != null ? req.unitsNeeded() : 1);
        entity.setUnitsFulfilled(0);
        entity.setHospitalId(req.hospitalId());
        entity.setHospital(req.hospital() != null ? req.hospital().trim() : null);
        entity.setCity(req.city() != null ? req.city().trim() : null);
        entity.setUrgencyEnum(req.urgency());
        entity.setStatus(RequestStatus.OPEN);
        entity.setNeededBy(req.neededBy() != null ? req.neededBy() : LocalDate.now().plusDays(3));

        BloodRequest saved = bloodRequestRepository.save(entity);
        return bloodRequestMapper.toResponse(saved);
    }

    public PageResponse<BloodRequestResponse> searchRequests(
            String city,
            BloodGroup bloodGroup,
            Urgency urgency,
            RequestStatus status,
            int page,
            int size
    ) {
        List<BloodRequestResponse> items = bloodRequestRepository.search(city, bloodGroup, urgency, status)
                .stream()
                .map(bloodRequestMapper::toResponse)
                .toList();
        return PageResponse.of(items, page, size);
    }

    public BloodRequestResponse getRequestById(String id) {
        BloodRequest request = bloodRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Blood request not found with id: " + id));
        return bloodRequestMapper.toResponse(request);
    }
}
