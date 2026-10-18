package com.example.blood.dto;

public record PublicStatsResponse(
        long registeredDonors,
        long availableDonors,
        long verifiedHospitals,
        long totalUnitsInStock,
        long openRequests,
        long fulfilledRequests,
        long verifiedDonations
) {
}
