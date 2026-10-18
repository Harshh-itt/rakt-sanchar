package com.example.blood.service;

import com.example.blood.event.BadgeEarnedEvent;
import com.example.blood.model.BadgeCode;
import com.example.blood.model.BloodRequest;
import com.example.blood.model.DonationStatus;
import com.example.blood.model.DonorProfile;
import com.example.blood.model.EarnedBadge;
import com.example.blood.model.Urgency;
import com.example.blood.repository.DonationRepository;
import com.example.blood.repository.DonorProfileRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class BadgeService {

    private final DonationRepository donationRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BadgeService(
            DonationRepository donationRepository,
            DonorProfileRepository donorProfileRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.donationRepository = donationRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Evaluates and awards milestone and emergency-response badges idempotently.
     */
    public synchronized List<EarnedBadge> evaluateAndAwardBadges(
            DonorProfile donorProfile,
            BloodRequest linkedRequest
    ) {
        if (donorProfile == null || donorProfile.getUserId() == null) {
            return List.of();
        }

        int verifiedCount = donationRepository
                .findByDonorUserIdAndStatus(donorProfile.getUserId(), DonationStatus.VERIFIED)
                .size();

        List<BadgeCode> candidateCodes = new ArrayList<>();
        if (verifiedCount >= BadgeCode.FIRST_DROP.getRequiredVerifiedDonations()) {
            candidateCodes.add(BadgeCode.FIRST_DROP);
        }
        if (verifiedCount >= BadgeCode.REGULAR.getRequiredVerifiedDonations()) {
            candidateCodes.add(BadgeCode.REGULAR);
        }
        if (verifiedCount >= BadgeCode.HERO.getRequiredVerifiedDonations()) {
            candidateCodes.add(BadgeCode.HERO);
        }
        if (verifiedCount >= BadgeCode.LIFESAVER.getRequiredVerifiedDonations()) {
            candidateCodes.add(BadgeCode.LIFESAVER);
        }

        if (linkedRequest != null && (linkedRequest.getUrgencyEnum() == Urgency.CRITICAL
                || linkedRequest.getUrgencyEnum() == Urgency.URGENT)) {
            candidateCodes.add(BadgeCode.EMERGENCY_RESPONDER);
        }

        Instant now = Instant.now();
        List<EarnedBadge> newlyAwarded = new ArrayList<>();
        for (BadgeCode code : candidateCodes) {
            boolean alreadyEarned = donorProfile.getBadges().stream()
                    .anyMatch(b -> b.getCode() == code);
            if (!alreadyEarned) {
                EarnedBadge badge = new EarnedBadge(code, now);
                donorProfile.addBadge(badge);
                newlyAwarded.add(badge);
            }
        }

        if (!newlyAwarded.isEmpty()) {
            donorProfileRepository.save(donorProfile);
            for (EarnedBadge badge : newlyAwarded) {
                eventPublisher.publishEvent(new BadgeEarnedEvent(
                        donorProfile.getUserId(),
                        badge.getCode(),
                        badge.getCode().getTitle(),
                        now
                ));
            }
        }

        return List.copyOf(donorProfile.getBadges());
    }
}
