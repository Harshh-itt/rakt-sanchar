package com.example.blood.repository;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.DonorProfile;

import java.util.List;
import java.util.Optional;

public interface DonorProfileRepository {
    DonorProfile save(DonorProfile profile);

    Optional<DonorProfile> findById(String id);

    Optional<DonorProfile> findByUserId(String userId);

    List<DonorProfile> findAll();

    List<DonorProfile> search(BloodGroup bloodGroup, String city, Boolean available);

    long count();

    void deleteAll();
}
