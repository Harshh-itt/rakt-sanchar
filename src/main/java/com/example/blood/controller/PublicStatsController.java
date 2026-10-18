package com.example.blood.controller;

import com.example.blood.dto.PublicStatsResponse;
import com.example.blood.model.DonationStatus;
import com.example.blood.model.DonorProfile;
import com.example.blood.model.Hospital;
import com.example.blood.model.HospitalStatus;
import com.example.blood.model.RequestStatus;
import com.example.blood.repository.BloodRequestRepository;
import com.example.blood.repository.BloodStockRepository;
import com.example.blood.repository.DonationRepository;
import com.example.blood.repository.DonorProfileRepository;
import com.example.blood.repository.HospitalRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/stats")
public class PublicStatsController {

    private final DonorProfileRepository donorProfileRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodStockRepository bloodStockRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonationRepository donationRepository;

    public PublicStatsController(
            DonorProfileRepository donorProfileRepository,
            HospitalRepository hospitalRepository,
            BloodStockRepository bloodStockRepository,
            BloodRequestRepository bloodRequestRepository,
            DonationRepository donationRepository
    ) {
        this.donorProfileRepository = donorProfileRepository;
        this.hospitalRepository = hospitalRepository;
        this.bloodStockRepository = bloodStockRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.donationRepository = donationRepository;
    }

    @GetMapping
    public PublicStatsResponse getPublicStats() {
        List<DonorProfile> allDonors = donorProfileRepository.findAll();
        long registeredDonors = allDonors.size();
        long availableDonors = allDonors.stream().filter(DonorProfile::isAvailable).count();

        List<Hospital> verifiedHospitalsList = hospitalRepository.findByStatus(HospitalStatus.VERIFIED);
        Set<String> verifiedHospitalIds = verifiedHospitalsList.stream()
                .map(Hospital::getId)
                .collect(Collectors.toSet());

        long totalUnitsInStock = bloodStockRepository.findAll().stream()
                .filter(s -> verifiedHospitalIds.contains(s.getHospitalId()))
                .mapToLong(s -> Math.max(0, s.getUnits()))
                .sum();

        long openRequests = bloodRequestRepository.countByStatus(RequestStatus.OPEN);
        long fulfilledRequests = bloodRequestRepository.countByStatus(RequestStatus.FULFILLED);
        long verifiedDonations = donationRepository.findAll().stream()
                .filter(d -> d.getStatus() == DonationStatus.VERIFIED)
                .count();

        return new PublicStatsResponse(
                registeredDonors,
                availableDonors,
                verifiedHospitalsList.size(),
                totalUnitsInStock,
                openRequests,
                fulfilledRequests,
                verifiedDonations
        );
    }
}
