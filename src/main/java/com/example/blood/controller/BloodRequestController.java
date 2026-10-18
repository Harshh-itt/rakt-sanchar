package com.example.blood.controller;

import com.example.blood.dto.BloodRequestCreateRequest;
import com.example.blood.dto.BloodRequestResponse;
import com.example.blood.dto.DonorRespondRequest;
import com.example.blood.dto.MatchedDonorResponse;
import com.example.blood.dto.PageResponse;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.RequestStatus;
import com.example.blood.model.Urgency;
import com.example.blood.security.CurrentUserProvider;
import com.example.blood.service.BloodRequestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class BloodRequestController {

    private final BloodRequestService bloodRequestService;
    private final CurrentUserProvider currentUserProvider;

    public BloodRequestController(
            BloodRequestService bloodRequestService,
            CurrentUserProvider currentUserProvider
    ) {
        this.bloodRequestService = bloodRequestService;
        this.currentUserProvider = currentUserProvider;
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
        return bloodRequestService.searchRequests(
                currentUserProvider.getCurrentUserOptional().orElse(null),
                city,
                bloodGroup,
                urgency,
                status,
                page,
                size
        );
    }

    @GetMapping("/mine")
    public List<BloodRequestResponse> getMyRequests() {
        return bloodRequestService.getMyRequests(currentUserProvider.getCurrentUserOptional().orElse(null));
    }

    @GetMapping("/{id}")
    public BloodRequestResponse getRequestById(@PathVariable("id") String id) {
        return bloodRequestService.getRequestById(currentUserProvider.getCurrentUserOptional().orElse(null), id);
    }

    @GetMapping("/{id}/matches")
    public List<MatchedDonorResponse> getRequestMatches(
            @PathVariable("id") String id,
            @RequestParam(name = "maxDistanceKm", required = false) Double maxDistanceKm
    ) {
        return bloodRequestService.getMatchesForRequest(
                currentUserProvider.getCurrentUserOptional().orElse(null),
                id,
                maxDistanceKm
        );
    }

    @PostMapping
    public BloodRequestResponse addRequest(@Valid @RequestBody BloodRequestCreateRequest request) {
        return bloodRequestService.createRequest(currentUserProvider.getCurrentUserOptional().orElse(null), request);
    }

    @PostMapping("/{id}/respond")
    public BloodRequestResponse respondToRequest(
            @PathVariable("id") String id,
            @Valid @RequestBody DonorRespondRequest request
    ) {
        return bloodRequestService.respondToRequest(
                currentUserProvider.getCurrentUserOptional().orElse(null),
                id,
                request
        );
    }

    @PatchMapping("/{id}/cancel")
    public BloodRequestResponse cancelRequest(@PathVariable("id") String id) {
        return bloodRequestService.cancelRequest(currentUserProvider.getCurrentUserOptional().orElse(null), id);
    }
}