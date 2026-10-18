package com.example.blood.controller;

import com.example.blood.dto.DonorCreateRequest;
import com.example.blood.dto.DonorResponse;
import com.example.blood.dto.PageResponse;
import com.example.blood.model.BloodGroup;
import com.example.blood.service.DonorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donors")
public class DonorController {

    private final DonorService donorService;

    public DonorController(DonorService donorService) {
        this.donorService = donorService;
    }

    @GetMapping
    public PageResponse<DonorResponse> getDonors(
            @RequestParam(name = "bloodGroup", required = false) BloodGroup bloodGroup,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "available", required = false) Boolean available,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return donorService.searchDonors(bloodGroup, city, available, page, size);
    }

    @GetMapping("/{id}")
    public DonorResponse getDonorById(@PathVariable("id") String id) {
        return donorService.getDonorById(id);
    }

    @PostMapping
    public DonorResponse addDonor(@Valid @RequestBody DonorCreateRequest donor) {
        return donorService.createDonor(donor);
    }
}