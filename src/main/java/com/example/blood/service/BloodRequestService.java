package com.example.blood.service;

import com.example.blood.dto.BloodRequestCreateRequest;
import com.example.blood.dto.BloodRequestResponse;
import com.example.blood.dto.DonorRespondRequest;
import com.example.blood.dto.MatchedDonorResponse;
import com.example.blood.dto.PageResponse;
import com.example.blood.event.DonorRespondedEvent;
import com.example.blood.event.RequestCreatedEvent;
import com.example.blood.exception.ApiException;
import com.example.blood.exception.ErrorCode;
import com.example.blood.mapper.BloodRequestMapper;
import com.example.blood.mapper.DonorMapper;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.BloodRequest;
import com.example.blood.model.DonorMatchResponse;
import com.example.blood.model.DonorProfile;
import com.example.blood.model.DonorResponseStatus;
import com.example.blood.model.Hospital;
import com.example.blood.model.HospitalStatus;
import com.example.blood.model.RequestStatus;
import com.example.blood.model.Role;
import com.example.blood.model.Urgency;
import com.example.blood.repository.BloodRequestRepository;
import com.example.blood.repository.DonorProfileRepository;
import com.example.blood.repository.HospitalRepository;
import com.example.blood.security.AccessPolicy;
import com.example.blood.security.CurrentUser;
import com.example.blood.util.GeoUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class BloodRequestService {

    private final BloodRequestRepository bloodRequestRepository;
    private final HospitalRepository hospitalRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final CompatibilityService compatibilityService;
    private final EligibilityService eligibilityService;
    private final AccessPolicy accessPolicy;
    private final BloodRequestMapper bloodRequestMapper;
    private final ApplicationEventPublisher eventPublisher;

    public BloodRequestService(
            BloodRequestRepository bloodRequestRepository,
            HospitalRepository hospitalRepository,
            DonorProfileRepository donorProfileRepository,
            CompatibilityService compatibilityService,
            EligibilityService eligibilityService,
            AccessPolicy accessPolicy,
            BloodRequestMapper bloodRequestMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.bloodRequestRepository = bloodRequestRepository;
        this.hospitalRepository = hospitalRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.compatibilityService = compatibilityService;
        this.eligibilityService = eligibilityService;
        this.accessPolicy = accessPolicy;
        this.bloodRequestMapper = bloodRequestMapper;
        this.eventPublisher = eventPublisher;
    }

    public BloodRequestResponse createRequest(BloodRequestCreateRequest req) {
        return createRequest(null, req);
    }

    public BloodRequestResponse createRequest(CurrentUser currentUser, BloodRequestCreateRequest req) {
        Hospital resolvedHospital = null;

        if (req.hospitalId() != null && !req.hospitalId().isBlank()) {
            resolvedHospital = hospitalRepository.findById(req.hospitalId().trim())
                    .orElseThrow(() -> ApiException.notFound("Hospital not found with id: " + req.hospitalId()));
        } else if (currentUser != null && currentUser.role() == Role.HOSPITAL
                && currentUser.userId() != null && !currentUser.userId().isBlank()) {
            resolvedHospital = hospitalRepository.findByOwnerUserId(currentUser.userId()).orElse(null);
        }

        if (resolvedHospital != null) {
            if (currentUser != null && currentUser.role() == Role.HOSPITAL
                    && resolvedHospital.getOwnerUserId() != null
                    && !resolvedHospital.getOwnerUserId().equals(currentUser.userId())) {
                throw new ApiException(
                        ErrorCode.FORBIDDEN,
                        HttpStatus.FORBIDDEN,
                        "Hospital users can only create blood requests for their own hospital"
                );
            }
            if (resolvedHospital.getStatus() != HospitalStatus.VERIFIED) {
                throw new ApiException(
                        ErrorCode.HOSPITAL_NOT_VERIFIED,
                        HttpStatus.FORBIDDEN,
                        "Only VERIFIED hospitals can create or be linked to blood requests; current status: " + resolvedHospital.getStatus()
                );
            }
        } else if (currentUser != null && currentUser.role() == Role.HOSPITAL) {
            throw new ApiException(
                    ErrorCode.HOSPITAL_NOT_VERIFIED,
                    HttpStatus.FORBIDDEN,
                    "Hospital user must have a VERIFIED hospital profile before creating blood requests"
            );
        }

        String finalHospitalName = (req.hospital() != null && !req.hospital().isBlank())
                ? req.hospital().trim()
                : (resolvedHospital != null ? resolvedHospital.getName() : null);

        String finalCity = (req.city() != null && !req.city().isBlank())
                ? req.city().trim()
                : (resolvedHospital != null ? resolvedHospital.getCity() : null);

        if (finalHospitalName == null || finalHospitalName.isBlank()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Either hospitalId or hospital name is required",
                    Map.of("fields", Map.of("hospital", "Hospital is required"))
            );
        }
        if (finalCity == null || finalCity.isBlank()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "City is required when hospitalId is not provided",
                    Map.of("fields", Map.of("city", "City is required"))
            );
        }

        LocalDate neededBy = req.neededBy() != null ? req.neededBy() : LocalDate.now().plusDays(3);
        if (neededBy.isBefore(LocalDate.now())) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "neededBy date cannot be in the past",
                    Map.of("fields", Map.of("neededBy", "neededBy cannot be in the past"))
            );
        }

        BloodRequest entity = new BloodRequest();
        if (currentUser != null && currentUser.userId() != null && !currentUser.userId().isBlank()) {
            entity.setRequesterUserId(currentUser.userId());
        }
        entity.setPatientName(req.patientName().trim());
        entity.setBloodGroupEnum(req.bloodGroup());
        entity.setUnitsNeeded(req.unitsNeeded() != null ? req.unitsNeeded() : 1);
        entity.setUnitsFulfilled(0);
        entity.setHospitalId(resolvedHospital != null ? resolvedHospital.getId() : null);
        entity.setHospital(finalHospitalName);
        entity.setCity(finalCity);
        if (resolvedHospital != null) {
            entity.setHospitalLatitude(resolvedHospital.getLatitude());
            entity.setHospitalLongitude(resolvedHospital.getLongitude());
        }
        entity.setUrgencyEnum(req.urgency());
        entity.setStatus(RequestStatus.OPEN);
        entity.setNeededBy(neededBy);

        BloodRequest saved = bloodRequestRepository.save(entity);

        eventPublisher.publishEvent(new RequestCreatedEvent(
                saved.getId(),
                saved.getRequesterUserId(),
                saved.getHospitalId(),
                saved.getHospital(),
                saved.getPatientName(),
                saved.getBloodGroupEnum(),
                saved.getUrgencyEnum(),
                saved.getCity(),
                Instant.now()
        ));

        return bloodRequestMapper.toResponse(saved, canViewAcceptedPhone(currentUser, saved));
    }

    public PageResponse<BloodRequestResponse> searchRequests(
            String city,
            BloodGroup bloodGroup,
            Urgency urgency,
            RequestStatus status,
            int page,
            int size
    ) {
        return searchRequests(null, city, bloodGroup, urgency, status, page, size);
    }

    public PageResponse<BloodRequestResponse> searchRequests(
            CurrentUser currentUser,
            String city,
            BloodGroup bloodGroup,
            Urgency urgency,
            RequestStatus status,
            int page,
            int size
    ) {
        List<BloodRequestResponse> items = bloodRequestRepository.search(city, bloodGroup, urgency, status)
                .stream()
                .map(req -> bloodRequestMapper.toResponse(req, canViewAcceptedPhone(currentUser, req)))
                .toList();
        return PageResponse.of(items, page, size);
    }

    public BloodRequestResponse getRequestById(String id) {
        return getRequestById(null, id);
    }

    public BloodRequestResponse getRequestById(CurrentUser currentUser, String id) {
        BloodRequest request = bloodRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Blood request not found with id: " + id));
        return bloodRequestMapper.toResponse(request, canViewAcceptedPhone(currentUser, request));
    }

    public List<BloodRequestResponse> getMyRequests(CurrentUser currentUser) {
        accessPolicy.requireAuthenticated(currentUser);
        Map<String, BloodRequest> merged = new LinkedHashMap<>();

        for (BloodRequest r : bloodRequestRepository.findByRequesterUserId(currentUser.userId())) {
            merged.put(r.getId(), r);
        }

        if (currentUser.role() == Role.HOSPITAL) {
            hospitalRepository.findByOwnerUserId(currentUser.userId())
                    .ifPresent(hospital -> {
                        for (BloodRequest r : bloodRequestRepository.findByHospitalId(hospital.getId())) {
                            merged.put(r.getId(), r);
                        }
                    });
        }

        return merged.values().stream()
                .sorted(Comparator.comparing(BloodRequest::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BloodRequest::getId))
                .map(r -> bloodRequestMapper.toResponse(r, true))
                .toList();
    }

    public List<MatchedDonorResponse> getMatchesForRequest(
            CurrentUser currentUser,
            String requestId,
            Double maxDistanceKm
    ) {
        accessPolicy.requireAuthenticated(currentUser);
        BloodRequest request = bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Blood request not found with id: " + requestId));

        if (!canManageOrViewMatches(currentUser, request)) {
            throw new ApiException(
                    ErrorCode.FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "Only the requester, owning hospital, or admin can view matched donors"
            );
        }

        Double reqLat = request.getHospitalLatitude();
        Double reqLon = request.getHospitalLongitude();
        if (!GeoUtils.hasCoordinates(reqLat, reqLon) && request.getHospitalId() != null) {
            Optional<Hospital> hospOpt = hospitalRepository.findById(request.getHospitalId());
            if (hospOpt.isPresent()) {
                reqLat = hospOpt.get().getLatitude();
                reqLon = hospOpt.get().getLongitude();
            }
        }
        boolean requestHasCoords = GeoUtils.hasCoordinates(reqLat, reqLon);

        List<MatchedDonorResponse> matches = new ArrayList<>();
        for (DonorProfile donor : donorProfileRepository.findAll()) {
            if (!donor.isAvailable()) {
                continue;
            }
            if (!compatibilityService.canDonateTo(donor.getBloodGroup(), request.getBloodGroupEnum())) {
                continue;
            }
            EligibilityService.EligibilityStatus eligibility = eligibilityService.evaluate(donor);
            if (!eligibility.eligible()) {
                continue;
            }

            Double distanceKm = null;
            boolean donorHasCoords = GeoUtils.hasCoordinates(donor.getLatitude(), donor.getLongitude());
            if (requestHasCoords && donorHasCoords) {
                distanceKm = GeoUtils.haversineKm(reqLat, reqLon, donor.getLatitude(), donor.getLongitude());
                if (maxDistanceKm != null && maxDistanceKm > 0 && distanceKm > maxDistanceKm) {
                    continue;
                }
            } else {
                // City fallback when coordinates are absent on either side
                if (request.getCity() == null || donor.getCity() == null
                        || !request.getCity().trim().equalsIgnoreCase(donor.getCity().trim())) {
                    continue;
                }
            }

            boolean exactMatch = compatibilityService.isExactMatch(donor.getBloodGroup(), request.getBloodGroupEnum());
            DonorMatchResponse existingResp = resolveDonorResponse(request, donor);
            DonorResponseStatus respStatus = existingResp != null ? existingResp.getStatus() : null;
            Instant respondedAt = existingResp != null ? existingResp.getRespondedAt() : null;

            String phone = (respStatus == DonorResponseStatus.ACCEPTED)
                    ? donor.getPhone()
                    : DonorMapper.maskPhone(donor.getPhone());

            matches.add(new MatchedDonorResponse(
                    donor.getId(),
                    donor.getUserId(),
                    donor.getName(),
                    donor.getBloodGroup(),
                    donor.getCity(),
                    phone,
                    exactMatch,
                    distanceKm,
                    donor.isAvailable(),
                    true,
                    respStatus,
                    respondedAt
            ));
        }

        matches.sort(
                Comparator.comparing(MatchedDonorResponse::exactMatch).reversed()
                        .thenComparing(MatchedDonorResponse::distanceKm, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(m -> responsePriority(m.responseStatus()))
                        .thenComparing(MatchedDonorResponse::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        );

        return matches;
    }

    public BloodRequestResponse respondToRequest(
            CurrentUser currentUser,
            String requestId,
            DonorRespondRequest respondRequest
    ) {
        accessPolicy.requireRole(currentUser, Role.DONOR);
        if (respondRequest.status() != DonorResponseStatus.ACCEPTED
                && respondRequest.status() != DonorResponseStatus.DECLINED) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Response status must be ACCEPTED or DECLINED",
                    Map.of("fields", Map.of("status", "Must be ACCEPTED or DECLINED"))
            );
        }

        DonorProfile donor = donorProfileRepository.findByUserId(currentUser.userId())
                .orElseThrow(() -> ApiException.notFound("Donor profile not found for user: " + currentUser.userId()));

        BloodRequest existingRequest = bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Blood request not found with id: " + requestId));

        if (!compatibilityService.canDonateTo(donor.getBloodGroup(), existingRequest.getBloodGroupEnum())) {
            throw new ApiException(
                    ErrorCode.INCOMPATIBLE_BLOOD_GROUP,
                    HttpStatus.BAD_REQUEST,
                    "Donor blood group " + donor.getBloodGroup().getLabel()
                            + " is not compatible with requested blood group "
                            + existingRequest.getBloodGroupEnum().getLabel()
            );
        }

        if (respondRequest.status() == DonorResponseStatus.ACCEPTED) {
            if (!donor.isAvailable()) {
                throw new ApiException(
                        ErrorCode.INELIGIBLE_DONOR,
                        HttpStatus.BAD_REQUEST,
                        "Donor must be marked available before accepting a request"
                );
            }
            EligibilityService.EligibilityStatus eligibility = eligibilityService.evaluate(donor);
            if (!eligibility.eligible()) {
                throw new ApiException(
                        ErrorCode.INELIGIBLE_DONOR,
                        HttpStatus.BAD_REQUEST,
                        "Donor is not eligible to donate (" + eligibility.reason() + ")"
                );
            }
        }

        Instant now = Instant.now();
        BloodRequest updated = bloodRequestRepository.computeAtomically(requestId, req -> {
            if (req.getStatus() != RequestStatus.OPEN) {
                throw new ApiException(
                        ErrorCode.INVALID_STATE_TRANSITION,
                        HttpStatus.CONFLICT,
                        "Cannot respond to a blood request with status " + req.getStatus()
                );
            }
            req.getDonorResponses().put(
                    currentUser.userId(),
                    new DonorMatchResponse(currentUser.userId(), donor.getId(), respondRequest.status(), now)
            );
            return req;
        });

        eventPublisher.publishEvent(new DonorRespondedEvent(
                updated.getId(),
                updated.getRequesterUserId(),
                updated.getHospitalId(),
                currentUser.userId(),
                donor.getId(),
                donor.getName(),
                respondRequest.status(),
                now
        ));

        return bloodRequestMapper.toResponse(updated, canViewAcceptedPhone(currentUser, updated));
    }

    public BloodRequestResponse cancelRequest(CurrentUser currentUser, String requestId) {
        accessPolicy.requireAuthenticated(currentUser);
        BloodRequest existing = bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Blood request not found with id: " + requestId));

        if (!canManageOrViewMatches(currentUser, existing)) {
            throw new ApiException(
                    ErrorCode.FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "Only the requester, owning hospital, or admin can cancel this blood request"
            );
        }

        BloodRequest updated = bloodRequestRepository.computeAtomically(requestId, req -> {
            if (req.getStatus() != RequestStatus.OPEN) {
                throw new ApiException(
                        ErrorCode.INVALID_STATE_TRANSITION,
                        HttpStatus.CONFLICT,
                        "Only OPEN requests can be cancelled; current status is " + req.getStatus()
                );
            }
            req.setStatus(RequestStatus.CANCELLED);
            return req;
        });

        return bloodRequestMapper.toResponse(updated, true);
    }

    @Scheduled(fixedDelayString = "${app.requests.expiry-check-ms:3600000}")
    public void scheduledExpireOverdueRequests() {
        expireOverdueRequests(LocalDate.now());
    }

    /**
     * Marks OPEN blood requests whose neededBy date is strictly before referenceDate as EXPIRED.
     * Safe and idempotent when called repeatedly.
     */
    public int expireOverdueRequests(LocalDate referenceDate) {
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        int expiredCount = 0;

        for (BloodRequest candidate : bloodRequestRepository.search(null, null, null, RequestStatus.OPEN)) {
            if (candidate.getNeededBy() != null && candidate.getNeededBy().isBefore(ref)) {
                AtomicBoolean transitioned = new AtomicBoolean(false);
                bloodRequestRepository.computeAtomically(candidate.getId(), req -> {
                    if (req.getStatus() == RequestStatus.OPEN
                            && req.getNeededBy() != null
                            && req.getNeededBy().isBefore(ref)) {
                        req.setStatus(RequestStatus.EXPIRED);
                        transitioned.set(true);
                    }
                    return req;
                });
                if (transitioned.get()) {
                    expiredCount++;
                }
            }
        }
        return expiredCount;
    }

    public boolean canManageOrViewMatches(CurrentUser currentUser, BloodRequest request) {
        if (currentUser == null || currentUser.userId() == null || currentUser.userId().isBlank()) {
            return false;
        }
        if (currentUser.role() == Role.ADMIN) {
            return true;
        }
        if (currentUser.userId().equals(request.getRequesterUserId())) {
            return true;
        }
        if (request.getHospitalId() != null && !request.getHospitalId().isBlank()) {
            return hospitalRepository.findById(request.getHospitalId())
                    .map(h -> currentUser.userId().equals(h.getOwnerUserId()))
                    .orElse(false);
        }
        return false;
    }

    private boolean canViewAcceptedPhone(CurrentUser currentUser, BloodRequest request) {
        return canManageOrViewMatches(currentUser, request);
    }

    private DonorMatchResponse resolveDonorResponse(BloodRequest request, DonorProfile donor) {
        if (request.getDonorResponses() == null) {
            return null;
        }
        if (donor.getUserId() != null) {
            DonorMatchResponse byUser = request.getDonorResponses().get(donor.getUserId());
            if (byUser != null) {
                return byUser;
            }
        }
        if (donor.getId() != null) {
            return request.getDonorResponses().get(donor.getId());
        }
        return null;
    }

    private int responsePriority(DonorResponseStatus status) {
        if (status == DonorResponseStatus.ACCEPTED) {
            return 0;
        }
        if (status == null) {
            return 1;
        }
        return 2;
    }
}
