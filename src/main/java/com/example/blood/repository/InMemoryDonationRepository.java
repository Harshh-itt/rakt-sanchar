package com.example.blood.repository;

import com.example.blood.model.Donation;
import com.example.blood.model.DonationStatus;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDonationRepository implements DonationRepository {

    private final Map<String, Donation> store = new ConcurrentHashMap<>();

    @Override
    public synchronized Donation save(Donation donation) {
        if (donation.getId() == null || donation.getId().isBlank()) {
            donation.setId(UUID.randomUUID().toString());
            if (donation.getCreatedAt() == null) {
                donation.setCreatedAt(Instant.now());
            }
        }
        store.put(donation.getId(), donation);
        return donation;
    }

    @Override
    public Optional<Donation> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Donation> findByDonorUserId(String donorUserId) {
        if (donorUserId == null || donorUserId.isBlank()) return List.of();
        return store.values().stream()
                .filter(d -> donorUserId.equals(d.getDonorUserId()))
                .sorted(Comparator.comparing(Donation::getDonatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Donation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Donation::getId))
                .toList();
    }

    @Override
    public List<Donation> findByDonorUserIdAndStatus(String donorUserId, DonationStatus status) {
        return findByDonorUserId(donorUserId).stream()
                .filter(d -> status == null || d.getStatus() == status)
                .toList();
    }

    @Override
    public List<Donation> findByHospitalId(String hospitalId) {
        if (hospitalId == null || hospitalId.isBlank()) return List.of();
        return store.values().stream()
                .filter(d -> hospitalId.equals(d.getHospitalId()))
                .sorted(Comparator.comparing(Donation::getDonatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Donation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Donation::getId))
                .toList();
    }

    @Override
    public List<Donation> findByHospitalIdAndStatus(String hospitalId, DonationStatus status) {
        return findByHospitalId(hospitalId).stream()
                .filter(d -> status == null || d.getStatus() == status)
                .toList();
    }

    @Override
    public List<Donation> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Donation::getDonatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Donation::getId))
                .toList();
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
