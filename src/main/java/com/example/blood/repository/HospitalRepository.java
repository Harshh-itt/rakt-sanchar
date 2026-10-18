package com.example.blood.repository;

import com.example.blood.model.Hospital;
import com.example.blood.model.HospitalStatus;

import java.util.List;
import java.util.Optional;

public interface HospitalRepository {
    Hospital save(Hospital hospital);

    Optional<Hospital> findById(String id);

    Optional<Hospital> findByOwnerUserId(String ownerUserId);

    List<Hospital> findAll();

    List<Hospital> findByStatus(HospitalStatus status);

    List<Hospital> searchVerified(String city, String name);

    long countByStatus(HospitalStatus status);

    void deleteAll();
}
