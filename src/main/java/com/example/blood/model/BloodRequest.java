package com.example.blood.model;

public class BloodRequest {
    private Long id;
    private String patientName;
    private String bloodGroup;
    private String hospital;
    private String city;
    private String urgency;

    public BloodRequest() {}

    public BloodRequest(Long id, String patientName, String bloodGroup, String hospital, String city, String urgency) {
        this.id = id;
        this.patientName = patientName;
        this.bloodGroup = bloodGroup;
        this.hospital = hospital;
        this.city = city;
        this.urgency = urgency;
    }

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getHospital() { return hospital; }
    public void setHospital(String hospital) { this.hospital = hospital; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
}
