package com.example.blood.controller;

import com.example.blood.model.Donor;
import com.example.blood.repository.DonorRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donors")
@CrossOrigin("*")
public class DonorController {

    private final DonorRepository donorRepository;

    public DonorController(DonorRepository donorRepository) {
        this.donorRepository = donorRepository;
    }

    @GetMapping
public List<Donor> getDonors(
        @RequestParam(name = "bloodGroup", required = false) String bloodGroup,
        @RequestParam(name = "city", required = false) String city) {

    if (bloodGroup != null && city != null) {
        return donorRepository.findByBloodGroupIgnoreCaseAndCityIgnoreCase(
                bloodGroup, city
        );
    }

    return donorRepository.findAll();
}

    @PostMapping
    public Donor addDonor(@RequestBody Donor donor) {
        return donorRepository.save(donor);
    }
}