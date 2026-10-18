package com.example.blood.repository;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.BloodRequest;
import com.example.blood.model.RequestStatus;
import com.example.blood.model.Urgency;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface BloodRequestRepository {
    BloodRequest save(BloodRequest request);

    Optional<BloodRequest> findById(String id);

    List<BloodRequest> findAll();

    List<BloodRequest> findByRequesterUserId(String requesterUserId);

    List<BloodRequest> findByHospitalId(String hospitalId);

    List<BloodRequest> search(String city, BloodGroup bloodGroup, Urgency urgency, RequestStatus status);

    /**
     * Atomically updates a request inside a synchronized lock.
     * In a future persistent implementation, this maps to a database transaction / optimistic lock.
     */
    BloodRequest computeAtomically(String requestId, Function<BloodRequest, BloodRequest> mutator);

    long countByStatus(RequestStatus status);

    void deleteAll();
}