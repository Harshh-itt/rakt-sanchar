package com.example.blood.repository;

import com.example.blood.model.Donor;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DonorRepository extends MongoRepository<Donor, String> {

    List<Donor> findByBloodGroupIgnoreCaseAndCityIgnoreCase(
            String bloodGroup,
            String city
    );
}