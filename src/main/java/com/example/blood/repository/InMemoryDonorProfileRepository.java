package com.example.blood.repository;

import com.example.blood.exception.ApiException;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.DonorProfile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDonorProfileRepository implements DonorProfileRepository {

    private final Map<String, DonorProfile> store = new ConcurrentHashMap<>();

    @Override
    public synchronized DonorProfile save(DonorProfile profile) {
        if (profile.getUserId() != null && !profile.getUserId().isBlank()) {
            Optional<DonorProfile> existingForUser = findByUserId(profile.getUserId());
            if (existingForUser.isPresent() && !existingForUser.get().getId().equals(profile.getId())) {
                throw ApiException.duplicate("Donor profile already exists for userId: " + profile.getUserId());
            }
        }
        if (profile.getId() == null || profile.getId().isBlank()) {
            profile.setId(UUID.randomUUID().toString());
            if (profile.getCreatedAt() == null) {
                profile.setCreatedAt(Instant.now());
            }
        }
        profile.setUpdatedAt(Instant.now());
        store.put(profile.getId(), profile);
        return profile;
    }

    @Override
    public Optional<DonorProfile> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<DonorProfile> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) return Optional.empty();
        return store.values().stream()
                .filter(d -> userId.equals(d.getUserId()))
                .findFirst();
    }

    @Override
    public List<DonorProfile> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(DonorProfile::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DonorProfile::getId))
                .toList();
    }

    @Override
    public List<DonorProfile> search(BloodGroup bloodGroup, String city, Boolean available) {
        String normalizedCity = (city != null && !city.isBlank()) ? city.trim() : null;
        return store.values().stream()
                .filter(d -> bloodGroup == null || bloodGroup == d.getBloodGroup())
                .filter(d -> normalizedCity == null || (d.getCity() != null && d.getCity().trim().equalsIgnoreCase(normalizedCity)))
                .filter(d -> available == null || d.isAvailable() == available)
                .sorted(Comparator.comparing(DonorProfile::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DonorProfile::getId))
                .toList();
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
