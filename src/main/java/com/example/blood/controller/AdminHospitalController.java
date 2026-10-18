package com.example.blood.controller;

import com.example.blood.dto.HospitalRejectRequest;
import com.example.blood.dto.HospitalResponse;
import com.example.blood.model.HospitalStatus;
import com.example.blood.security.CurrentUser;
import com.example.blood.security.CurrentUserProvider;
import com.example.blood.service.HospitalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/hospitals")
public class AdminHospitalController {

    private final HospitalService hospitalService;
    private final CurrentUserProvider currentUserProvider;

    public AdminHospitalController(HospitalService hospitalService, CurrentUserProvider currentUserProvider) {
        this.hospitalService = hospitalService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<HospitalResponse> listHospitals(
            @RequestParam(name = "status", required = false) HospitalStatus status
    ) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        return hospitalService.listHospitalsForAdmin(currentUser, status);
    }

    @PostMapping("/{id}/approve")
    public HospitalResponse approveHospital(@PathVariable("id") String id) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        return hospitalService.approveHospital(currentUser, id);
    }

    @PostMapping("/{id}/reject")
    public HospitalResponse rejectHospital(
            @PathVariable("id") String id,
            @Valid @RequestBody HospitalRejectRequest request
    ) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        return hospitalService.rejectHospital(currentUser, id, request);
    }
}
