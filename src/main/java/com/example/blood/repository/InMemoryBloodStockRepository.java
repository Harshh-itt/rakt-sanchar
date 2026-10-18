package com.example.blood.repository;

import com.example.blood.exception.ApiException;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.BloodStock;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBloodStockRepository implements BloodStockRepository {

    private final Map<String, BloodStock> store = new ConcurrentHashMap<>();

    @Override
    public synchronized BloodStock save(BloodStock stock) {
        if (stock.getHospitalId() == null || stock.getHospitalId().isBlank() || stock.getBloodGroup() == null) {
            throw ApiException.badRequest("hospitalId and bloodGroup are required for BloodStock");
        }
        Optional<BloodStock> existing = findByHospitalIdAndBloodGroup(stock.getHospitalId(), stock.getBloodGroup());
        if (existing.isPresent() && (stock.getId() == null || !existing.get().getId().equals(stock.getId()))) {
            // Enforce unique constraint (hospitalId + bloodGroup)
            throw ApiException.duplicate(
                    "Blood stock entry already exists for hospital " + stock.getHospitalId() + " and group " + stock.getBloodGroup()
            );
        }
        if (stock.getId() == null || stock.getId().isBlank()) {
            stock.setId(UUID.randomUUID().toString());
        }
        if (stock.getUpdatedAt() == null) {
            stock.setUpdatedAt(Instant.now());
        }
        store.put(stock.getId(), stock);
        return stock;
    }

    @Override
    public Optional<BloodStock> findByHospitalIdAndBloodGroup(String hospitalId, BloodGroup bloodGroup) {
        if (hospitalId == null || bloodGroup == null) return Optional.empty();
        return store.values().stream()
                .filter(s -> hospitalId.equals(s.getHospitalId()) && bloodGroup == s.getBloodGroup())
                .findFirst();
    }

    @Override
    public List<BloodStock> findByHospitalId(String hospitalId) {
        if (hospitalId == null) return List.of();
        return store.values().stream()
                .filter(s -> hospitalId.equals(s.getHospitalId()))
                .sorted(Comparator.comparing(s -> s.getBloodGroup().ordinal()))
                .toList();
    }

    @Override
    public List<BloodStock> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(BloodStock::getHospitalId)
                        .thenComparing(s -> s.getBloodGroup().ordinal()))
                .toList();
    }

    @Override
    public synchronized boolean deleteByHospitalIdAndBloodGroup(String hospitalId, BloodGroup bloodGroup) {
        Optional<BloodStock> existing = findByHospitalIdAndBloodGroup(hospitalId, bloodGroup);
        if (existing.isPresent()) {
            store.remove(existing.get().getId());
            return true;
        }
        return false;
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
