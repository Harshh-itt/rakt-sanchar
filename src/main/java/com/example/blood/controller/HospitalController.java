package com.example.blood.controller;

import com.example.blood.dto.BloodStockResponse;
import com.example.blood.dto.BloodStockUpdateRequest;
import com.example.blood.dto.HospitalProfileUpdateRequest;
import com.example.blood.dto.HospitalResponse;
import com.example.blood.dto.PageResponse;
import com.example.blood.model.BloodGroup;
import com.example.blood.security.CurrentUser;
import com.example.blood.security.CurrentUserProvider;
import com.example.blood.service.BloodStockService;
import com.example.blood.service.HospitalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;
    private final BloodStockService bloodStockService;
    private final CurrentUserProvider currentUserProvider;

    public HospitalController(
            HospitalService hospitalService,
            BloodStockService bloodStockService,
            CurrentUserProvider currentUserProvider
    ) {
        this.hospitalService = hospitalService;
        this.bloodStockService = bloodStockService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public PageResponse<HospitalResponse> searchVerifiedHospitals(
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return hospitalService.searchVerifiedHospitals(city, name, page, size);
    }

    @GetMapping("/me")
    public HospitalResponse getMyHospital() {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        return hospitalService.getMyHospital(currentUser);
    }

    @PutMapping("/me")
    public HospitalResponse updateMyHospital(@Valid @RequestBody HospitalProfileUpdateRequest request) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        return hospitalService.upsertMyHospital(currentUser, request);
    }

    @GetMapping("/me/stock")
    public List<BloodStockResponse> getMyHospitalStock() {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        return bloodStockService.getMyHospitalStock(currentUser);
    }

    @PutMapping("/me/stock/{bloodGroup}")
    public BloodStockResponse upsertMyHospitalStock(
            @PathVariable("bloodGroup") BloodGroup bloodGroup,
            @Valid @RequestBody BloodStockUpdateRequest request
    ) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        return bloodStockService.upsertMyHospitalStock(currentUser, bloodGroup, request);
    }

    @DeleteMapping("/me/stock/{bloodGroup}")
    public ResponseEntity<Void> deleteMyHospitalStock(@PathVariable("bloodGroup") BloodGroup bloodGroup) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        bloodStockService.deleteMyHospitalStock(currentUser, bloodGroup);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public HospitalResponse getVerifiedHospitalById(@PathVariable("id") String id) {
        return hospitalService.getVerifiedHospitalById(id);
    }
}
