package com.example.blood.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain Java domain model for a donor profile (no persistence annotations).
 */
public class DonorProfile {
    private String id;
    private String userId;
    private String name;
    private BloodGroup bloodGroup;
    private LocalDate dateOfBirth;
    private Integer age;
    private String phone;
    private String city;
    private Double latitude;
    private Double longitude;
    private boolean available = true;
    private LocalDate declaredLastDonationDate;
    private List<EarnedBadge> badges = new ArrayList<>();
    private String internalAuditNote;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public DonorProfile() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public LocalDate getDeclaredLastDonationDate() {
        return declaredLastDonationDate;
    }

    public void setDeclaredLastDonationDate(LocalDate declaredLastDonationDate) {
        this.declaredLastDonationDate = declaredLastDonationDate;
    }

    public List<EarnedBadge> getBadges() {
        return badges;
    }

    public void setBadges(List<EarnedBadge> badges) {
        this.badges = badges != null ? new ArrayList<>(badges) : new ArrayList<>();
    }

    public String getInternalAuditNote() {
        return internalAuditNote;
    }

    public void setInternalAuditNote(String internalAuditNote) {
        this.internalAuditNote = internalAuditNote;
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
