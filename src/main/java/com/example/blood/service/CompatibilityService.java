package com.example.blood.service;

import com.example.blood.model.BloodGroup;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ABO and Rh red-cell compatibility rules for donor-to-recipient matching.
 */
@Service
public class CompatibilityService {

    private static final Map<BloodGroup, Set<BloodGroup>> DONOR_TO_RECIPIENTS = new EnumMap<>(BloodGroup.class);

    static {
        DONOR_TO_RECIPIENTS.put(BloodGroup.O_NEG, EnumSet.allOf(BloodGroup.class));
        DONOR_TO_RECIPIENTS.put(BloodGroup.O_POS, EnumSet.of(
                BloodGroup.O_POS, BloodGroup.A_POS, BloodGroup.B_POS, BloodGroup.AB_POS
        ));
        DONOR_TO_RECIPIENTS.put(BloodGroup.A_NEG, EnumSet.of(
                BloodGroup.A_NEG, BloodGroup.A_POS, BloodGroup.AB_NEG, BloodGroup.AB_POS
        ));
        DONOR_TO_RECIPIENTS.put(BloodGroup.A_POS, EnumSet.of(
                BloodGroup.A_POS, BloodGroup.AB_POS
        ));
        DONOR_TO_RECIPIENTS.put(BloodGroup.B_NEG, EnumSet.of(
                BloodGroup.B_NEG, BloodGroup.B_POS, BloodGroup.AB_NEG, BloodGroup.AB_POS
        ));
        DONOR_TO_RECIPIENTS.put(BloodGroup.B_POS, EnumSet.of(
                BloodGroup.B_POS, BloodGroup.AB_POS
        ));
        DONOR_TO_RECIPIENTS.put(BloodGroup.AB_NEG, EnumSet.of(
                BloodGroup.AB_NEG, BloodGroup.AB_POS
        ));
        DONOR_TO_RECIPIENTS.put(BloodGroup.AB_POS, EnumSet.of(
                BloodGroup.AB_POS
        ));
    }

    public boolean canDonateTo(BloodGroup donorGroup, BloodGroup recipientGroup) {
        if (donorGroup == null || recipientGroup == null) {
            return false;
        }
        Set<BloodGroup> allowed = DONOR_TO_RECIPIENTS.get(donorGroup);
        return allowed != null && allowed.contains(recipientGroup);
    }

    public boolean isExactMatch(BloodGroup donorGroup, BloodGroup recipientGroup) {
        return donorGroup != null && donorGroup == recipientGroup;
    }

    /**
     * Returns all donor blood groups compatible with the given recipient blood group,
     * with the exact blood group match placed first.
     */
    public List<BloodGroup> getCompatibleDonorGroups(BloodGroup recipientGroup) {
        if (recipientGroup == null) {
            return List.of();
        }
        List<BloodGroup> result = new ArrayList<>();
        result.add(recipientGroup);
        for (BloodGroup candidate : BloodGroup.values()) {
            if (candidate != recipientGroup && canDonateTo(candidate, recipientGroup)) {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Returns all recipient blood groups that a donor of the given blood group can donate to.
     */
    public List<BloodGroup> getCompatibleRecipientGroups(BloodGroup donorGroup) {
        if (donorGroup == null) {
            return List.of();
        }
        Set<BloodGroup> recipients = DONOR_TO_RECIPIENTS.getOrDefault(donorGroup, Set.of());
        List<BloodGroup> result = new ArrayList<>();
        if (recipients.contains(donorGroup)) {
            result.add(donorGroup);
        }
        for (BloodGroup recipient : BloodGroup.values()) {
            if (recipient != donorGroup && recipients.contains(recipient)) {
                result.add(recipient);
            }
        }
        return List.copyOf(result);
    }
}
