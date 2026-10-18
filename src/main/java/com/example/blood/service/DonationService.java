package com.example.blood.service;

import com.example.blood.dto.DonationResponse;
import com.example.blood.dto.DonationVerificationRequest;
import com.example.blood.dto.DonorDonationClaimRequest;
import com.example.blood.dto.DonorSummaryResponse;
import com.example.blood.dto.HospitalDonationRecordRequest;
import com.example.blood.event.DonationVerifiedEvent;
import com.example.blood.exception.ApiException;
import com.example.blood.exception.ErrorCode;
import com.example.blood.model.BloodRequest;
import com.example.blood.model.Donation;
import com.example.blood.model.DonationStatus;
import com.example.blood.model.DonorProfile;
import com.example.blood.model.Hospital;
import com.example.blood.model.HospitalStatus;
import com.example.blood.model.RequestStatus;
import com.example.blood.model.Role;
import com.example.blood.repository.BloodRequestRepository;
import com.example.blood.repository.DonationRepository;
import com.example.blood.repository.DonorProfileRepository;
import com.example.blood.repository.HospitalRepository;
import com.example.blood.security.AccessPolicy;
import com.example.blood.security.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class DonationService {

    private final DonationRepository donationRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final EligibilityService eligibilityService;
    private final CompatibilityService compatibilityService;
    private final BadgeService badgeService;
    private final AccessPolicy accessPolicy;
    private final ApplicationEventPublisher eventPublisher;

    public DonationService(
            DonationRepository donationRepository,
            DonorProfileRepository donorProfileRepository,
            HospitalRepository hospitalRepository,
            BloodRequestRepository bloodRequestRepository,
            EligibilityService eligibilityService,
            CompatibilityService compatibilityService,
            BadgeService badgeService,
            AccessPolicy accessPolicy,
            ApplicationEventPublisher eventPublisher
    ) {
        this.donationRepository = donationRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.hospitalRepository = hospitalRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.eligibilityService = eligibilityService;
        this.compatibilityService = compatibilityService;
        this.badgeService = badgeService;
        this.accessPolicy = accessPolicy;
        this.eventPublisher = eventPublisher;
    }

    public synchronized DonationResponse recordHospitalDonation(
            CurrentUser currentUser,
            HospitalDonationRecordRequest req
    ) {
        accessPolicy.requireRole(currentUser, Role.HOSPITAL);
        Hospital hospital = requireVerifiedHospitalForOwner(currentUser.userId());

        DonorProfile donor = donorProfileRepository.findByUserId(req.donorUserId().trim())
                .orElseThrow(() -> ApiException.notFound("Donor profile not found for userId: " + req.donorUserId()));

        LocalDate donatedAt = req.donatedAt() != null ? req.donatedAt() : LocalDate.now();
        validateNotFutureDate(donatedAt);
        validate90DayGap(donor, donatedAt, null);

        BloodRequest linkedRequest = null;
        if (req.requestId() != null && !req.requestId().isBlank()) {
            linkedRequest = bloodRequestRepository.findById(req.requestId().trim())
                    .orElseThrow(() -> ApiException.notFound("Blood request not found with id: " + req.requestId()));
            if (linkedRequest.getHospitalId() != null
                    && !linkedRequest.getHospitalId().equals(hospital.getId())) {
                throw new ApiException(
                        ErrorCode.FORBIDDEN,
                        HttpStatus.FORBIDDEN,
                        "Hospital cannot record a donation against another hospital's blood request"
                );
            }
            if (!compatibilityService.canDonateTo(donor.getBloodGroup(), linkedRequest.getBloodGroupEnum())) {
                throw new ApiException(
                        ErrorCode.INCOMPATIBLE_BLOOD_GROUP,
                        HttpStatus.BAD_REQUEST,
                        "Donor blood group " + donor.getBloodGroup().getLabel()
                                + " is not compatible with request blood group "
                                + linkedRequest.getBloodGroupEnum().getLabel()
                );
            }
        }

        int units = req.units() != null ? req.units() : 1;
        Instant now = Instant.now();

        Donation donation = new Donation();
        donation.setDonorUserId(donor.getUserId());
        donation.setHospitalId(hospital.getId());
        donation.setHospitalName(hospital.getName());
        donation.setBloodGroup(donor.getBloodGroup());
        donation.setUnits(units);
        donation.setDonatedAt(donatedAt);
        donation.setStatus(DonationStatus.VERIFIED);
        donation.setRequestId(linkedRequest != null ? linkedRequest.getId() : null);
        donation.setCreatedAt(now);
        donation.setVerifiedAt(now);
        donation.setVerifiedByUserId(currentUser.userId());

        Donation saved = donationRepository.save(donation);
        BloodRequest updatedRequest = applyFulfillmentIfLinked(saved.getRequestId(), saved.getUnits());
        badgeService.evaluateAndAwardBadges(donor, updatedRequest);

        eventPublisher.publishEvent(new DonationVerifiedEvent(
                saved.getId(),
                saved.getDonorUserId(),
                saved.getHospitalId(),
                saved.getHospitalName(),
                saved.getRequestId(),
                saved.getUnits(),
                saved.getDonatedAt(),
                now
        ));

        return toResponse(saved);
    }

    public synchronized DonationResponse submitDonorClaim(
            CurrentUser currentUser,
            DonorDonationClaimRequest req
    ) {
        accessPolicy.requireRole(currentUser, Role.DONOR);
        DonorProfile donor = donorProfileRepository.findByUserId(currentUser.userId())
                .orElseThrow(() -> ApiException.notFound("Donor profile not found for user: " + currentUser.userId()));

        validateNotFutureDate(req.donatedAt());
        validate90DayGap(donor, req.donatedAt(), null);

        BloodRequest linkedRequest = null;
        if (req.requestId() != null && !req.requestId().isBlank()) {
            linkedRequest = bloodRequestRepository.findById(req.requestId().trim())
                    .orElseThrow(() -> ApiException.notFound("Blood request not found with id: " + req.requestId()));
            if (!compatibilityService.canDonateTo(donor.getBloodGroup(), linkedRequest.getBloodGroupEnum())) {
                throw new ApiException(
                        ErrorCode.INCOMPATIBLE_BLOOD_GROUP,
                        HttpStatus.BAD_REQUEST,
                        "Donor blood group " + donor.getBloodGroup().getLabel()
                                + " is not compatible with request blood group "
                                + linkedRequest.getBloodGroupEnum().getLabel()
                );
            }
        }

        Hospital resolvedHospital = null;
        String hospitalId = (req.hospitalId() != null && !req.hospitalId().isBlank())
                ? req.hospitalId().trim()
                : (linkedRequest != null ? linkedRequest.getHospitalId() : null);

        if (hospitalId != null && !hospitalId.isBlank()) {
            resolvedHospital = hospitalRepository.findById(hospitalId)
                    .orElseThrow(() -> ApiException.notFound("Hospital not found with id: " + hospitalId));
        }

        String hospitalName = (req.hospitalName() != null && !req.hospitalName().isBlank())
                ? req.hospitalName().trim()
                : (resolvedHospital != null
                        ? resolvedHospital.getName()
                        : (linkedRequest != null ? linkedRequest.getHospital() : null));

        if (hospitalName == null || hospitalName.isBlank()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Either hospitalId or hospitalName is required",
                    Map.of("fields", Map.of("hospitalName", "Hospital is required"))
            );
        }

        Donation claim = new Donation();
        claim.setDonorUserId(donor.getUserId());
        claim.setHospitalId(resolvedHospital != null ? resolvedHospital.getId() : null);
        claim.setHospitalName(hospitalName);
        claim.setBloodGroup(donor.getBloodGroup());
        claim.setUnits(req.units() != null ? req.units() : 1);
        claim.setDonatedAt(req.donatedAt());
        claim.setStatus(DonationStatus.PENDING);
        claim.setRequestId(linkedRequest != null ? linkedRequest.getId() : null);
        claim.setCreatedAt(Instant.now());

        Donation saved = donationRepository.save(claim);
        return toResponse(saved);
    }

    public synchronized DonationResponse verifyOrRejectDonation(
            CurrentUser currentUser,
            String donationId,
            DonationVerificationRequest req
    ) {
        accessPolicy.requireRole(currentUser, Role.HOSPITAL);
        Hospital hospital = requireVerifiedHospitalForOwner(currentUser.userId());

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> ApiException.notFound("Donation not found with id: " + donationId));

        if (donation.getHospitalId() == null || !donation.getHospitalId().equals(hospital.getId())) {
            throw new ApiException(
                    ErrorCode.FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "You can only verify or reject donations for your own hospital"
            );
        }

        if (donation.getStatus() != DonationStatus.PENDING) {
            throw new ApiException(
                    ErrorCode.INVALID_STATE_TRANSITION,
                    HttpStatus.CONFLICT,
                    "Only PENDING donation claims can be verified or rejected; current status is " + donation.getStatus()
            );
        }

        if (req.status() != DonationStatus.VERIFIED && req.status() != DonationStatus.REJECTED) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Verification status must be VERIFIED or REJECTED",
                    Map.of("fields", Map.of("status", "Must be VERIFIED or REJECTED"))
            );
        }

        Instant now = Instant.now();
        if (req.status() == DonationStatus.REJECTED) {
            if (req.rejectionReason() == null || req.rejectionReason().isBlank()) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR,
                        HttpStatus.BAD_REQUEST,
                        "Rejection reason is required when rejecting a donation claim",
                        Map.of("fields", Map.of("rejectionReason", "Rejection reason is required"))
                );
            }
            donation.setStatus(DonationStatus.REJECTED);
            donation.setRejectionReason(req.rejectionReason().trim());
            donation.setVerifiedAt(now);
            donation.setVerifiedByUserId(currentUser.userId());
            return toResponse(donationRepository.save(donation));
        }

        DonorProfile donor = donorProfileRepository.findByUserId(donation.getDonorUserId())
                .orElseThrow(() -> ApiException.notFound("Donor profile not found for user: " + donation.getDonorUserId()));

        validate90DayGap(donor, donation.getDonatedAt(), donation.getId());

        donation.setStatus(DonationStatus.VERIFIED);
        donation.setRejectionReason(null);
        donation.setVerifiedAt(now);
        donation.setVerifiedByUserId(currentUser.userId());
        Donation saved = donationRepository.save(donation);

        BloodRequest updatedRequest = applyFulfillmentIfLinked(saved.getRequestId(), saved.getUnits());
        badgeService.evaluateAndAwardBadges(donor, updatedRequest);

        eventPublisher.publishEvent(new DonationVerifiedEvent(
                saved.getId(),
                saved.getDonorUserId(),
                saved.getHospitalId(),
                saved.getHospitalName(),
                saved.getRequestId(),
                saved.getUnits(),
                saved.getDonatedAt(),
                now
        ));

        return toResponse(saved);
    }

    public List<DonationResponse> getMyDonorDonations(CurrentUser currentUser) {
        accessPolicy.requireRole(currentUser, Role.DONOR);
        return donationRepository.findByDonorUserId(currentUser.userId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public DonorSummaryResponse getMyDonorSummary(CurrentUser currentUser) {
        accessPolicy.requireRole(currentUser, Role.DONOR);
        DonorProfile donor = donorProfileRepository.findByUserId(currentUser.userId())
                .orElseThrow(() -> ApiException.notFound("Donor profile not found for user: " + currentUser.userId()));

        List<Donation> allMyDonations = donationRepository.findByDonorUserId(currentUser.userId());
        List<Donation> verifiedDonations = allMyDonations.stream()
                .filter(d -> d.getStatus() == DonationStatus.VERIFIED)
                .toList();

        int totalVerifiedDonations = verifiedDonations.size();
        int totalVerifiedUnits = verifiedDonations.stream().mapToInt(Donation::getUnits).sum();
        int pendingClaimsCount = (int) allMyDonations.stream()
                .filter(d -> d.getStatus() == DonationStatus.PENDING)
                .count();

        EligibilityService.EligibilityStatus eligibility = eligibilityService.evaluate(donor);

        return new DonorSummaryResponse(
                donor.getUserId(),
                donor.getName(),
                donor.getBloodGroup(),
                totalVerifiedDonations,
                totalVerifiedUnits,
                pendingClaimsCount,
                eligibility.effectiveLastDonationDate(),
                eligibility.eligible(),
                eligibility.nextEligibleDate(),
                eligibility.reason(),
                donor.getBadges() != null ? List.copyOf(donor.getBadges()) : List.of()
        );
    }

    public List<DonationResponse> getMyHospitalDonations(CurrentUser currentUser, DonationStatus status) {
        accessPolicy.requireRole(currentUser, Role.HOSPITAL);
        Hospital hospital = hospitalRepository.findByOwnerUserId(currentUser.userId())
                .orElseThrow(() -> ApiException.notFound("Hospital profile not found for user: " + currentUser.userId()));

        List<Donation> list = status != null
                ? donationRepository.findByHospitalIdAndStatus(hospital.getId(), status)
                : donationRepository.findByHospitalId(hospital.getId());

        return list.stream()
                .sorted(Comparator.comparing(Donation::getDonatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Donation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    private Hospital requireVerifiedHospitalForOwner(String ownerUserId) {
        Hospital hospital = hospitalRepository.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.HOSPITAL_NOT_VERIFIED,
                        HttpStatus.FORBIDDEN,
                        "Hospital profile not found or not verified"
                ));
        if (hospital.getStatus() != HospitalStatus.VERIFIED) {
            throw new ApiException(
                    ErrorCode.HOSPITAL_NOT_VERIFIED,
                    HttpStatus.FORBIDDEN,
                    "Only VERIFIED hospitals can record or verify blood donations; current status: " + hospital.getStatus()
            );
        }
        return hospital;
    }

    private void validateNotFutureDate(LocalDate donatedAt) {
        if (donatedAt == null) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Donation date is required",
                    Map.of("fields", Map.of("donatedAt", "Donation date is required"))
            );
        }
        if (donatedAt.isAfter(LocalDate.now())) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Donation date cannot be in the future",
                    Map.of("fields", Map.of("donatedAt", "Donation date cannot be in the future"))
            );
        }
    }

    private void validate90DayGap(DonorProfile donor, LocalDate candidateDate, String excludeDonationId) {
        int minGapDays = eligibilityService.getMinGapDays();

        for (Donation existing : donationRepository.findByDonorUserIdAndStatus(donor.getUserId(), DonationStatus.VERIFIED)) {
            if (excludeDonationId != null && excludeDonationId.equals(existing.getId())) {
                continue;
            }
            if (existing.getDonatedAt() != null) {
                long daysBetween = Math.abs(ChronoUnit.DAYS.between(existing.getDonatedAt(), candidateDate));
                if (daysBetween < minGapDays) {
                    throw new ApiException(
                            ErrorCode.INELIGIBLE_DONOR,
                            HttpStatus.BAD_REQUEST,
                            "Donation on " + candidateDate + " is within " + minGapDays
                                    + " days of verified donation on " + existing.getDonatedAt()
                    );
                }
            }
        }

        LocalDate declared = donor.getDeclaredLastDonationDate();
        if (declared != null && !candidateDate.isBefore(declared)) {
            long daysSinceDeclared = ChronoUnit.DAYS.between(declared, candidateDate);
            if (daysSinceDeclared < minGapDays) {
                throw new ApiException(
                        ErrorCode.INELIGIBLE_DONOR,
                        HttpStatus.BAD_REQUEST,
                        "Donation on " + candidateDate + " is within " + minGapDays
                                + " days of declared last donation on " + declared
                );
            }
        }
    }

    private BloodRequest applyFulfillmentIfLinked(String requestId, int unitsToAdd) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        if (bloodRequestRepository.findById(requestId).isEmpty()) {
            return null;
        }
        // TODO(deferred-db): Wrap donation save + request fulfillment increment in a single DB transaction
        return bloodRequestRepository.computeAtomically(requestId, req -> {
            int newFulfilled = req.getUnitsFulfilled() + Math.max(1, unitsToAdd);
            req.setUnitsFulfilled(newFulfilled);
            if (req.getStatus() == RequestStatus.OPEN && newFulfilled >= req.getUnitsNeeded()) {
                req.setStatus(RequestStatus.FULFILLED);
            }
            return req;
        });
    }

    private DonationResponse toResponse(Donation d) {
        String donorName = donorProfileRepository.findByUserId(d.getDonorUserId())
                .map(DonorProfile::getName)
                .orElse(d.getDonorUserId());

        return new DonationResponse(
                d.getId(),
                d.getDonorUserId(),
                donorName,
                d.getHospitalId(),
                d.getHospitalName(),
                d.getBloodGroup(),
                d.getUnits(),
                d.getDonatedAt(),
                d.getStatus(),
                d.getRequestId(),
                d.getRejectionReason(),
                d.getCreatedAt(),
                d.getVerifiedAt(),
                d.getVerifiedByUserId()
        );
    }
}
