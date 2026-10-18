package com.example.blood.controller;

import com.example.blood.dto.BloodRequestCreateRequest;
import com.example.blood.dto.BloodRequestResponse;
import com.example.blood.dto.PageResponse;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.RequestStatus;
import com.example.blood.model.Urgency;
import com.example.blood.service.BloodRequestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
public class BloodRequestController {

    private final BloodRequestService bloodRequestService;

    public BloodRequestController(BloodRequestService bloodRequestService) {
        this.bloodRequestService = bloodRequestService;
    }

    @GetMapping
    public PageResponse<BloodRequestResponse> getRequests(
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "bloodGroup", required = false) BloodGroup bloodGroup,
            @RequestParam(name = "urgency", required = false) Urgency urgency,
            @RequestParam(name = "status", required = false) RequestStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return bloodRequestService.searchRequests(city, bloodGroup, urgency, status, page, size);
    }

    @GetMapping("/{id}")
    public BloodRequestResponse getRequestById(@PathVariable("id") String id) {
        return bloodRequestService.getRequestById(id);
    }

    @PostMapping
    public BloodRequestResponse addRequest(@Valid @RequestBody BloodRequestCreateRequest request) {
        return bloodRequestService.createRequest(request);
    }
}