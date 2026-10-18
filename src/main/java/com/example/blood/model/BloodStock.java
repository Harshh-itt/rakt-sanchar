package com.example.blood.model;

import java.time.Instant;

/**
 * Plain Java domain model for hospital blood stock per blood group.
 */
public class BloodStock {
    private String id;
    private String hospitalId;
    private BloodGroup bloodGroup;
    private int units;
    private int lowThreshold;
    private Instant updatedAt = Instant.now();

    public BloodStock() {
    }

    public BloodStock(String id, String hospitalId, BloodGroup bloodGroup, int units, int lowThreshold, Instant updatedAt) {
        this.id = id;
        this.hospitalId = hospitalId;
        this.bloodGroup = bloodGroup;
        this.units = units;
        this.lowThreshold = lowThreshold;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
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

    public int getLowThreshold() {
        return lowThreshold;
    }

    public void setLowThreshold(int lowThreshold) {
        this.lowThreshold = lowThreshold;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
