package com.example.blood.repository;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.BloodStock;

import java.util.List;
import java.util.Optional;

public interface BloodStockRepository {
    BloodStock save(BloodStock stock);

    Optional<BloodStock> findByHospitalIdAndBloodGroup(String hospitalId, BloodGroup bloodGroup);

    List<BloodStock> findByHospitalId(String hospitalId);

    List<BloodStock> findAll();

    boolean deleteByHospitalIdAndBloodGroup(String hospitalId, BloodGroup bloodGroup);

    void deleteAll();
}
