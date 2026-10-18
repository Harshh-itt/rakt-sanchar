package com.example.blood.model;

public enum BadgeCode {
    FIRST_DROP(1, "First Drop", "Completed 1st verified blood donation"),
    REGULAR(3, "Regular Donor", "Completed 3 verified blood donations"),
    HERO(5, "Community Hero", "Completed 5 verified blood donations"),
    LIFESAVER(10, "Lifesaver", "Completed 10 verified blood donations"),
    EMERGENCY_RESPONDER(0, "Emergency Responder", "Completed a verified donation for a CRITICAL or URGENT blood request");

    private final int requiredVerifiedDonations;
    private final String title;
    private final String description;

    BadgeCode(int requiredVerifiedDonations, String title, String description) {
        this.requiredVerifiedDonations = requiredVerifiedDonations;
        this.title = title;
        this.description = description;
    }

    public int getRequiredVerifiedDonations() {
        return requiredVerifiedDonations;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
