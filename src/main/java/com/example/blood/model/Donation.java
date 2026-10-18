package com.example.blood.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Plain Java domain model for a donation record or donor claim.
 */
public class Donation {
    private String id;
    private String donorUserId;
    private String hospitalId;
    private String hospitalName;
    private BloodGroup bloodGroup;
    private int units = 1;
    private LocalDate donatedAt;
    private DonationStatus status = DonationStatus.PENDING;
    private String requestId;
    private String rejectionReason;
    private Instant createdAt = Instant.now();
    private Instant verifiedAt;
    private String verifiedByUserId;

    public Donation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDonorUserId() {
        return donorUserId;
    }

    public void setDonorUserId(String donorUserId) {
        this.donorUserId = donorUserId;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public int getUnits() {
        return units;
    }

    public void setUnits(int units) {
        this.units = units;
    }

    public LocalDate getDonatedAt() {
        return donatedAt;
    }

    public void setDonatedAt(LocalDate donatedAt) {
        this.donatedAt = donatedAt;
    }

    public LocalDate getDonationDate() {
        return donatedAt;
    }

    public void setDonationDate(LocalDate donationDate) {
        this.donatedAt = donationDate;
    }

    public DonationStatus getStatus() {
        return status;
    }

    public void setStatus(DonationStatus status) {
        this.status = status;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerifiedByUserId() {
        return verifiedByUserId;
    }

    public void setVerifiedByUserId(String verifiedByUserId) {
        this.verifiedByUserId = verifiedByUserId;
    }
}
