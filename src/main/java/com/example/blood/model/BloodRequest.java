package com.example.blood.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plain Java domain model for a blood request (no persistence-specific annotations).
 */
public class BloodRequest {

    private String id;
    private String requesterUserId;
    private String patientName;
    private BloodGroup bloodGroup;
    private int unitsNeeded = 1;
    private int unitsFulfilled = 0;
    private String hospitalId;
    private String hospital;
    private String city;
    private Double hospitalLatitude;
    private Double hospitalLongitude;
    private Urgency urgency = Urgency.MEDIUM;
    private RequestStatus status = RequestStatus.OPEN;
    private LocalDate neededBy;
    private Map<String, DonorMatchResponse> donorResponses = new ConcurrentHashMap<>();
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public BloodRequest() {
    }

    public BloodRequest(String patientName, String bloodGroup, String hospital,
                        String city, String urgency) {
        this.patientName = patientName;
        if (bloodGroup != null && !bloodGroup.isBlank()) {
            this.bloodGroup = BloodGroup.fromValue(bloodGroup);
        }
        this.hospital = hospital;
        this.city = city;
        if (urgency != null && !urgency.isBlank()) {
            this.urgency = Urgency.fromValue(urgency);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequesterUserId() {
        return requesterUserId;
    }

    public void setRequesterUserId(String requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public BloodGroup getBloodGroupEnum() {
        return bloodGroup;
    }

    public void setBloodGroupEnum(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getBloodGroup() {
        return bloodGroup != null ? bloodGroup.getLabel() : null;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = (bloodGroup != null && !bloodGroup.isBlank()) ? BloodGroup.fromValue(bloodGroup) : null;
    }

    public int getUnitsNeeded() {
        return unitsNeeded;
    }

    public void setUnitsNeeded(int unitsNeeded) {
        this.unitsNeeded = unitsNeeded;
    }

    public int getUnitsFulfilled() {
        return unitsFulfilled;
    }

    public void setUnitsFulfilled(int unitsFulfilled) {
        this.unitsFulfilled = unitsFulfilled;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getHospital() {
        return hospital;
    }

    public void setHospital(String hospital) {
        this.hospital = hospital;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getHospitalLatitude() {
        return hospitalLatitude;
    }

    public void setHospitalLatitude(Double hospitalLatitude) {
        this.hospitalLatitude = hospitalLatitude;
    }

    public Double getHospitalLongitude() {
        return hospitalLongitude;
    }

    public void setHospitalLongitude(Double hospitalLongitude) {
        this.hospitalLongitude = hospitalLongitude;
    }

    public Urgency getUrgencyEnum() {
        return urgency;
    }

    public void setUrgencyEnum(Urgency urgency) {
        this.urgency = urgency;
    }

    public String getUrgency() {
        return urgency != null ? urgency.getLabel() : null;
    }

    public void setUrgency(String urgency) {
        this.urgency = (urgency != null && !urgency.isBlank()) ? Urgency.fromValue(urgency) : null;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDate getNeededBy() {
        return neededBy;
    }

    public void setNeededBy(LocalDate neededBy) {
        this.neededBy = neededBy;
    }

    public Map<String, DonorMatchResponse> getDonorResponses() {
        return donorResponses;
    }

    public void setDonorResponses(Map<String, DonorMatchResponse> donorResponses) {
        this.donorResponses = donorResponses != null ? new ConcurrentHashMap<>(donorResponses) : new ConcurrentHashMap<>();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}