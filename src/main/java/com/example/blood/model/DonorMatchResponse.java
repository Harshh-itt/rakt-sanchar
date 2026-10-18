package com.example.blood.model;

import java.time.Instant;

public class DonorMatchResponse {
    private String donorUserId;
    private String donorProfileId;
    private DonorResponseStatus status;
    private Instant respondedAt;

    public DonorMatchResponse() {
    }

    public DonorMatchResponse(String donorUserId, String donorProfileId, DonorResponseStatus status, Instant respondedAt) {
        this.donorUserId = donorUserId;
        this.donorProfileId = donorProfileId;
        this.status = status;
        this.respondedAt = respondedAt;
    }

    public String getDonorUserId() {
        return donorUserId;
    }

    public void setDonorUserId(String donorUserId) {
        this.donorUserId = donorUserId;
    }

    public String getDonorProfileId() {
        return donorProfileId;
    }

    public void setDonorProfileId(String donorProfileId) {
        this.donorProfileId = donorProfileId;
    }

    public DonorResponseStatus getStatus() {
        return status;
    }

    public void setStatus(DonorResponseStatus status) {
        this.status = status;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }
}
