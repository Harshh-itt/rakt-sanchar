package com.example.blood.service;

import com.example.blood.model.Donation;
import com.example.blood.model.DonationStatus;
import com.example.blood.model.DonorProfile;
import com.example.blood.repository.DonationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.Optional;

@Service
public class EligibilityService {

    public record EligibilityStatus(
            boolean eligible,
            LocalDate nextEligibleDate,
            String reason,
            Integer ageYears,
            LocalDate effectiveLastDonationDate
    ) {
    }

    private final DonationRepository donationRepository;
    private final int minAge;
    private final int maxAge;
    private final int minGapDays;

    public EligibilityService(
            DonationRepository donationRepository,
            @Value("${app.eligibility.min-age:18}") int minAge,
            @Value("${app.eligibility.max-age:65}") int maxAge,
            @Value("${app.eligibility.min-gap-days:90}") int minGapDays
    ) {
        this.donationRepository = donationRepository;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.minGapDays = minGapDays;
    }

    public EligibilityStatus evaluate(DonorProfile profile) {
        return evaluate(profile, LocalDate.now());
    }

    public EligibilityStatus evaluate(DonorProfile profile, LocalDate referenceDate) {
        if (profile == null) {
            return new EligibilityStatus(false, null, "PROFILE_MISSING", null, null);
        }
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();

        Integer ageYears = resolveAgeYears(profile, ref);
        LocalDate effectiveLastDonation = resolveEffectiveLastDonationDate(profile);

        if (ageYears == null) {
            return new EligibilityStatus(false, null, "AGE_REQUIRED", null, effectiveLastDonation);
        }
        if (ageYears < minAge) {
            LocalDate eighteenthBirthday = profile.getDateOfBirth() != null
                    ? profile.getDateOfBirth().plusYears(minAge)
                    : null;
            return new EligibilityStatus(false, eighteenthBirthday, "UNDER_MINIMUM_AGE", ageYears, effectiveLastDonation);
        }
        if (ageYears > maxAge) {
            return new EligibilityStatus(false, null, "OVER_MAXIMUM_AGE", ageYears, effectiveLastDonation);
        }

        if (effectiveLastDonation != null) {
            LocalDate earliestNextDate = effectiveLastDonation.plusDays(minGapDays);
            if (ref.isBefore(earliestNextDate)) {
                return new EligibilityStatus(false, earliestNextDate, "DONATION_GAP_ACTIVE", ageYears, effectiveLastDonation);
            }
        }

        return new EligibilityStatus(true, ref, "ELIGIBLE", ageYears, effectiveLastDonation);
    }

    public LocalDate resolveEffectiveLastDonationDate(DonorProfile profile) {
        LocalDate declared = profile.getDeclaredLastDonationDate();
        LocalDate latestVerified = null;
        if (profile.getUserId() != null && !profile.getUserId().isBlank()) {
            Optional<LocalDate> verifiedOpt = donationRepository.findByDonorUserId(profile.getUserId())
                    .stream()
                    .filter(d -> d.getStatus() == DonationStatus.VERIFIED && d.getDonationDate() != null)
                    .map(Donation::getDonationDate)
                    .max(Comparator.naturalOrder());
            if (verifiedOpt.isPresent()) {
                latestVerified = verifiedOpt.get();
            }
        }
        if (latestVerified != null && declared != null) {
            return latestVerified.isAfter(declared) ? latestVerified : declared;
        }
        return latestVerified != null ? latestVerified : declared;
    }

    private Integer resolveAgeYears(DonorProfile profile, LocalDate ref) {
        if (profile.getDateOfBirth() != null) {
            return Period.between(profile.getDateOfBirth(), ref).getYears();
        }
        return profile.getAge();
    }

    public int getMinAge() {
        return minAge;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public int getMinGapDays() {
        return minGapDays;
    }
}
