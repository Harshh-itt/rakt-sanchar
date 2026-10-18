package com.example.blood.repository;

import com.example.blood.exception.ApiException;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.BloodRequest;
import com.example.blood.model.RequestStatus;
import com.example.blood.model.Urgency;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Repository
public class InMemoryBloodRequestRepository implements BloodRequestRepository {

    private final Map<String, BloodRequest> store = new ConcurrentHashMap<>();

    @Override
    public synchronized BloodRequest save(BloodRequest request) {
        if (request.getId() == null || request.getId().isBlank()) {
            request.setId(UUID.randomUUID().toString());
            if (request.getCreatedAt() == null) {
                request.setCreatedAt(Instant.now());
            }
        }
        request.setUpdatedAt(Instant.now());
        store.put(request.getId(), request);
        return request;
    }

    @Override
    public Optional<BloodRequest> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<BloodRequest> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(BloodRequest::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BloodRequest::getId))
                .toList();
    }

    @Override
    public List<BloodRequest> findByRequesterUserId(String requesterUserId) {
        if (requesterUserId == null || requesterUserId.isBlank()) return List.of();
        return store.values().stream()
                .filter(r -> requesterUserId.equals(r.getRequesterUserId()))
                .sorted(Comparator.comparing(BloodRequest::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BloodRequest::getId))
                .toList();
    }

    @Override
    public List<BloodRequest> findByHospitalId(String hospitalId) {
        if (hospitalId == null || hospitalId.isBlank()) return List.of();
        return store.values().stream()
                .filter(r -> hospitalId.equals(r.getHospitalId()))
                .sorted(Comparator.comparing(BloodRequest::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BloodRequest::getId))
                .toList();
    }

    @Override
    public List<BloodRequest> search(String city, BloodGroup bloodGroup, Urgency urgency, RequestStatus status) {
        String normCity = (city != null && !city.isBlank()) ? city.trim() : null;
        return store.values().stream()
                .filter(r -> normCity == null || (r.getCity() != null && r.getCity().trim().equalsIgnoreCase(normCity)))
                .filter(r -> bloodGroup == null || r.getBloodGroupEnum() == bloodGroup)
                .filter(r -> urgency == null || r.getUrgencyEnum() == urgency)
                .filter(r -> status == null || r.getStatus() == status)
                .sorted(Comparator.comparing((BloodRequest r) -> r.getUrgencyEnum() != null ? r.getUrgencyEnum().getPriorityWeight() : 0).reversed()
                        .thenComparing(BloodRequest::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BloodRequest::getId))
                .toList();
    }

    @Override
    public synchronized BloodRequest computeAtomically(String requestId, Function<BloodRequest, BloodRequest> mutator) {
        // TODO(deferred-db): Replace synchronized in-memory mutation with @Transactional / atomic DB update
        BloodRequest existing = store.get(requestId);
        if (existing == null) {
            throw ApiException.notFound("Blood request not found: " + requestId);
        }
        BloodRequest updated = mutator.apply(existing);
        updated.setUpdatedAt(Instant.now());
        store.put(requestId, updated);
        return updated;
    }

    @Override
    public long countByStatus(RequestStatus status) {
        return store.values().stream()
                .filter(r -> r.getStatus() == status)
                .count();
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
