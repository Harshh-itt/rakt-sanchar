package com.example.blood.repository;

import com.example.blood.exception.ApiException;
import com.example.blood.model.Hospital;
import com.example.blood.model.HospitalStatus;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryHospitalRepository implements HospitalRepository {

    private final Map<String, Hospital> store = new ConcurrentHashMap<>();

    @Override
    public synchronized Hospital save(Hospital hospital) {
        if (hospital.getOwnerUserId() != null && !hospital.getOwnerUserId().isBlank()) {
            Optional<Hospital> existingForOwner = findByOwnerUserId(hospital.getOwnerUserId());
            if (existingForOwner.isPresent() && !existingForOwner.get().getId().equals(hospital.getId())) {
                throw ApiException.duplicate("Hospital profile already exists for ownerUserId: " + hospital.getOwnerUserId());
            }
        }
        if (hospital.getRegistrationNumber() != null && !hospital.getRegistrationNumber().isBlank()) {
            Optional<Hospital> existingByReg = store.values().stream()
                    .filter(h -> h.getRegistrationNumber() != null
                            && h.getRegistrationNumber().trim().equalsIgnoreCase(hospital.getRegistrationNumber().trim()))
                    .findFirst();
            if (existingByReg.isPresent() && !existingByReg.get().getId().equals(hospital.getId())) {
                throw ApiException.duplicate("Hospital registration number already exists: " + hospital.getRegistrationNumber());
            }
        }
        if (hospital.getId() == null || hospital.getId().isBlank()) {
            hospital.setId(UUID.randomUUID().toString());
            if (hospital.getCreatedAt() == null) {
                hospital.setCreatedAt(Instant.now());
            }
        }
        hospital.setUpdatedAt(Instant.now());
        store.put(hospital.getId(), hospital);
        return hospital;
    }

    @Override
    public Optional<Hospital> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Hospital> findByOwnerUserId(String ownerUserId) {
        if (ownerUserId == null || ownerUserId.isBlank()) return Optional.empty();
        return store.values().stream()
                .filter(h -> ownerUserId.equals(h.getOwnerUserId()))
                .findFirst();
    }

    @Override
    public List<Hospital> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Hospital::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Hospital::getId))
                .toList();
    }

    @Override
    public List<Hospital> findByStatus(HospitalStatus status) {
        return store.values().stream()
                .filter(h -> status == null || h.getStatus() == status)
                .sorted(Comparator.comparing(Hospital::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Hospital::getId))
                .toList();
    }

    @Override
    public List<Hospital> searchVerified(String city, String name) {
        String normCity = (city != null && !city.isBlank()) ? city.trim() : null;
        String normName = (name != null && !name.isBlank()) ? name.trim().toLowerCase(Locale.ROOT) : null;
        return store.values().stream()
                .filter(h -> h.getStatus() == HospitalStatus.VERIFIED)
                .filter(h -> normCity == null || (h.getCity() != null && h.getCity().trim().equalsIgnoreCase(normCity)))
                .filter(h -> normName == null || (h.getName() != null && h.getName().toLowerCase(Locale.ROOT).contains(normName)))
                .sorted(Comparator.comparing(Hospital::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Hospital::getId))
                .toList();
    }

    @Override
    public long countByStatus(HospitalStatus status) {
        return store.values().stream()
                .filter(h -> h.getStatus() == status)
                .count();
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
