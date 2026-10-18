package com.example.blood.security;

import com.example.blood.model.Role;

public record CurrentUser(
        String userId,
        Role role,
        String name,
        String preferredLanguage,
        boolean onboarded
) {
    public CurrentUser(String userId, Role role, String name, String preferredLanguage) {
        this(userId, role, name, preferredLanguage, true);
    }
}
