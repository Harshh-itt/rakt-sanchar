package com.example.blood.config;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.DonorProfile;
import com.example.blood.model.Role;
import com.example.blood.model.User;
import com.example.blood.repository.DonorProfileRepository;
import com.example.blood.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Seeds fictional demo users and initial donor profiles (expanded in Phase 7 with full
 * demo hospitals, stock, donors, requests, and notifications) under the "demo" profile.
 */
@Component
@Profile("demo")
public class DemoDataSeeder {

    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;

    public DemoDataSeeder(
            UserRepository userRepository,
            DonorProfileRepository donorProfileRepository
    ) {
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
    }

    @PostConstruct
    public void seedInitialData() {
        seedBaseUsers();
        seedBaseDonorProfiles();
    }

    public void seedBaseUsers() {
        saveUserIfAbsent("admin-1", "Aarav Mehta (System Admin)", Role.ADMIN, true, "en");
        saveUserIfAbsent("donor-1", "Priya Sharma", Role.DONOR, true, "en");
        saveUserIfAbsent("donor-2", "Rohan Verma", Role.DONOR, true, "hi");
        saveUserIfAbsent("hospital-1", "City Care Super Specialty Hospital", Role.HOSPITAL, true, "en");
        saveUserIfAbsent("hospital-2", "Janata Seva Kendra Hospital", Role.HOSPITAL, true, "en");
    }

    public void seedBaseDonorProfiles() {
        saveDonorIfAbsent("donor-1", "Priya Sharma", BloodGroup.O_POS,
                LocalDate.of(1996, 5, 15), "9876543210", "Mumbai", "400001",
                19.0760, 72.8777, true, LocalDate.now().minusDays(120));
        saveDonorIfAbsent("donor-2", "Rohan Verma", BloodGroup.A_POS,
                LocalDate.of(1994, 8, 20), "9812345678", "Delhi", "110001",
                28.6139, 77.2090, true, LocalDate.now().minusDays(45));
    }

    private void saveUserIfAbsent(String id, String name, Role role, boolean onboarded, String language) {
        if (userRepository.findById(id).isEmpty()) {
            userRepository.save(new User(id, name, role, onboarded, language, Instant.now()));
        }
    }

    private void saveDonorIfAbsent(
            String userId,
            String name,
            BloodGroup bloodGroup,
            LocalDate dob,
            String phone,
            String city,
            String pincode,
            Double lat,
            Double lng,
            boolean available,
            LocalDate declaredLastDonation
    ) {
        if (donorProfileRepository.findByUserId(userId).isEmpty()) {
            DonorProfile profile = new DonorProfile();
            profile.setUserId(userId);
            profile.setName(name);
            profile.setBloodGroup(bloodGroup);
            profile.setDateOfBirth(dob);
            profile.setAge(java.time.Period.between(dob, LocalDate.now()).getYears());
            profile.setPhone(phone);
            profile.setCity(city);
            profile.setPincode(pincode);
            profile.setLatitude(lat);
            profile.setLongitude(lng);
            profile.setAvailable(available);
            profile.setMedicalConditionsConfirmed(true);
            profile.setDeclaredLastDonationDate(declaredLastDonation);
            donorProfileRepository.save(profile);
        }
    }
}
