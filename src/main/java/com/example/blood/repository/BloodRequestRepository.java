package com.example.blood.repository;

import com.example.blood.model.BloodRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BloodRequestRepository extends MongoRepository<BloodRequest, String> {
}