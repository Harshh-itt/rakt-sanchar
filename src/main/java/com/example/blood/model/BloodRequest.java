package com.example.blood.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "blood_requests")
public class BloodRequest {

    @Id
    private String id;

    private String patientName;
    private String bloodGroup;
    private String hospital;
    private String city;
    private String urgency;

    public BloodRequest() {
    }

    public BloodRequest(String patientName, String bloodGroup, String hospital,
                        String city, String urgency) {
        this.patientName = patientName;
        this.bloodGroup = bloodGroup;
        this.hospital = hospital;
        this.city = city;
        this.urgency = urgency;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
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

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }
}