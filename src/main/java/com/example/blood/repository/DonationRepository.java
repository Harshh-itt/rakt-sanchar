package com.example.blood.repository;

import com.example.blood.model.Donation;
import com.example.blood.model.DonationStatus;

import java.util.List;
import java.util.Optional;

public interface DonationRepository {
    Donation save(Donation donation);

    Optional<Donation> findById(String id);

    List<Donation> findByDonorUserId(String donorUserId);

    List<Donation> findByDonorUserIdAndStatus(String donorUserId, DonationStatus status);

    List<Donation> findByHospitalId(String hospitalId);

    List<Donation> findByHospitalIdAndStatus(String hospitalId, DonationStatus status);

    List<Donation> findAll();

    void deleteAll();
}
