package com.example.blood.service;

import com.example.blood.dto.HospitalProfileUpdateRequest;
import com.example.blood.dto.HospitalRejectRequest;
import com.example.blood.dto.HospitalResponse;
import com.example.blood.dto.PageResponse;
import com.example.blood.event.HospitalVerificationEvent;
import com.example.blood.exception.ApiException;
import com.example.blood.model.Hospital;
import com.example.blood.model.HospitalStatus;
import com.example.blood.model.Role;
import com.example.blood.repository.HospitalRepository;
import com.example.blood.security.AccessPolicy;
import com.example.blood.security.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final AccessPolicy accessPolicy;
    private final ApplicationEventPublisher eventPublisher;

    public HospitalService(
            HospitalRepository hospitalRepository,
            AccessPolicy accessPolicy,
            ApplicationEventPublisher eventPublisher
    ) {
        this.hospitalRepository = hospitalRepository;
        this.accessPolicy = accessPolicy;
        this.eventPublisher = eventPublisher;
    }

    public HospitalResponse getMyHospital(CurrentUser currentUser) {
        accessPolicy.requireRole(currentUser, Role.HOSPITAL);
        Hospital hospital = hospitalRepository.findByOwnerUserId(currentUser.userId())
                .orElseThrow(() -> ApiException.notFound("Hospital profile not found for current user"));
        accessPolicy.requireOwner(currentUser, hospital.getOwnerUserId());
        return toResponse(hospital);
    }

    public HospitalResponse upsertMyHospital(CurrentUser currentUser, HospitalProfileUpdateRequest request) {
        accessPolicy.requireRole(currentUser, Role.HOSPITAL);
        Hospital hospital = hospitalRepository.findByOwnerUserId(currentUser.userId())
                .orElseGet(() -> {
                    Hospital h = new Hospital();
                    h.setOwnerUserId(currentUser.userId());
                    h.setStatus(HospitalStatus.PENDING);
                    return h;
                });
        accessPolicy.requireOwner(currentUser, hospital.getOwnerUserId());

        hospital.setName(request.name().trim());
        hospital.setRegistrationNumber(request.registrationNumber().trim());
        hospital.setContactPerson(request.contactPerson() != null ? request.contactPerson().trim() : null);
        hospital.setAddress(request.address().trim());
        hospital.setCity(request.city().trim());
        hospital.setState(request.state() != null ? request.state().trim() : null);
        hospital.setPincode(request.pincode() != null ? request.pincode().trim() : null);
        hospital.setPhone(request.phone().trim());
        hospital.setLatitude(request.latitude());
        hospital.setLongitude(request.longitude());

        if (hospital.getStatus() == null || hospital.getStatus() == HospitalStatus.REJECTED) {
            hospital.setStatus(HospitalStatus.PENDING);
            hospital.setRejectionReason(null);
        }

        Hospital saved = hospitalRepository.save(hospital);
        return toResponse(saved);
    }

    public PageResponse<HospitalResponse> searchVerifiedHospitals(String city, String name, int page, int size) {
        List<HospitalResponse> verified = hospitalRepository.searchVerified(city, name)
                .stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(verified, page, size);
    }

    public HospitalResponse getVerifiedHospitalById(String id) {
        Hospital hospital = hospitalRepository.findById(id)
                .filter(h -> h.getStatus() == HospitalStatus.VERIFIED)
                .orElseThrow(() -> ApiException.notFound("Verified hospital not found with id: " + id));
        return toResponse(hospital);
    }

    public List<HospitalResponse> listHospitalsForAdmin(CurrentUser currentUser, HospitalStatus status) {
        accessPolicy.requireRole(currentUser, Role.ADMIN);
        List<Hospital> hospitals = status != null
                ? hospitalRepository.findByStatus(status)
                : hospitalRepository.findAll();
        return hospitals.stream()
                .map(this::toResponse)
                .toList();
    }

    public HospitalResponse approveHospital(CurrentUser currentUser, String hospitalId) {
        accessPolicy.requireRole(currentUser, Role.ADMIN);
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> ApiException.notFound("Hospital not found with id: " + hospitalId));

        hospital.setStatus(HospitalStatus.VERIFIED);
        hospital.setRejectionReason(null);
        Hospital saved = hospitalRepository.save(hospital);

        eventPublisher.publishEvent(new HospitalVerificationEvent(
                saved.getId(),
                saved.getOwnerUserId(),
                saved.getName(),
                HospitalStatus.VERIFIED,
                null
        ));
        return toResponse(saved);
    }

    public HospitalResponse rejectHospital(CurrentUser currentUser, String hospitalId, HospitalRejectRequest request) {
        accessPolicy.requireRole(currentUser, Role.ADMIN);
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> ApiException.notFound("Hospital not found with id: " + hospitalId));

        hospital.setStatus(HospitalStatus.REJECTED);
        hospital.setRejectionReason(request.reason().trim());
        Hospital saved = hospitalRepository.save(hospital);

        eventPublisher.publishEvent(new HospitalVerificationEvent(
                saved.getId(),
                saved.getOwnerUserId(),
                saved.getName(),
                HospitalStatus.REJECTED,
                saved.getRejectionReason()
        ));
        return toResponse(saved);
    }

    public HospitalResponse toResponse(Hospital h) {
        return new HospitalResponse(
                h.getId(),
                h.getOwnerUserId(),
                h.getName(),
                h.getRegistrationNumber(),
                h.getContactPerson(),
                h.getAddress(),
                h.getCity(),
                h.getState(),
                h.getPincode(),
                h.getPhone(),
                h.getLatitude(),
                h.getLongitude(),
                h.getStatus(),
                h.getRejectionReason(),
                h.getCreatedAt(),
                h.getUpdatedAt()
        );
    }
}
