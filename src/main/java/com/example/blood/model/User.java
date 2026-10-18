package com.example.blood.model;

import java.time.Instant;

/**
 * Lightweight in-memory user domain model.
 * Note: Password hashes, tokens, and login metadata are intentionally excluded (deferred to owner).
 */
public class User {

    private String id;
    private String name;
    private Role role;
    private boolean onboarded;
    private String preferredLanguage = "en";
    private Instant createdAt;

    public User() {
    }

    public User(String id, String name, Role role, boolean onboarded, String preferredLanguage, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.onboarded = onboarded;
        this.preferredLanguage = preferredLanguage != null ? preferredLanguage : "en";
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isOnboarded() {
        return onboarded;
    }

    public void setOnboarded(boolean onboarded) {
        this.onboarded = onboarded;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
