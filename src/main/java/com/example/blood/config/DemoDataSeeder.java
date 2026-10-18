package com.example.blood.config;

import com.example.blood.model.Role;
import com.example.blood.model.User;
import com.example.blood.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Seeds fictional demo users (and in Phase 7 full demo hospitals, stock, donors, and requests)
 * into the in-memory repositories when running under the "demo" profile.
 */
@Component
@Profile("demo")
public class DemoDataSeeder {

    private final UserRepository userRepository;

    public DemoDataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void seedInitialData() {
        seedBaseUsers();
    }

    public void seedBaseUsers() {
        saveUserIfAbsent("admin-1", "Aarav Mehta (System Admin)", Role.ADMIN, true, "en");
        saveUserIfAbsent("donor-1", "Priya Sharma", Role.DONOR, true, "en");
        saveUserIfAbsent("donor-2", "Rohan Verma", Role.DONOR, true, "hi");
        saveUserIfAbsent("hospital-1", "City Care Super Specialty Hospital", Role.HOSPITAL, true, "en");
        saveUserIfAbsent("hospital-2", "Janata Seva Kendra Hospital", Role.HOSPITAL, true, "en");
    }

    private void saveUserIfAbsent(String id, String name, Role role, boolean onboarded, String language) {
        if (userRepository.findById(id).isEmpty()) {
            userRepository.save(new User(id, name, role, onboarded, language, Instant.now()));
        }
    }
}
