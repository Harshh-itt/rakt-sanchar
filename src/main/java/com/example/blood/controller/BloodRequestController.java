package com.example.blood.controller;

import com.example.blood.model.BloodRequest;
import com.example.blood.repository.BloodRequestRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin("*")
public class BloodRequestController {

    private final BloodRequestRepository bloodRequestRepository;

    public BloodRequestController(BloodRequestRepository bloodRequestRepository) {
        this.bloodRequestRepository = bloodRequestRepository;
    }

    @GetMapping
    public List<BloodRequest> getRequests() {
        return bloodRequestRepository.findAll();
    }

    @PostMapping
    public BloodRequest addRequest(@RequestBody BloodRequest request) {
        return bloodRequestRepository.save(request);
    }
}