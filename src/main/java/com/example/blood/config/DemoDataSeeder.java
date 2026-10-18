package com.example.blood.config;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.BloodStock;
import com.example.blood.model.DonorProfile;
import com.example.blood.model.Hospital;
import com.example.blood.model.HospitalStatus;
import com.example.blood.model.Role;
import com.example.blood.model.User;
import com.example.blood.repository.BloodStockRepository;
import com.example.blood.repository.DonorProfileRepository;
import com.example.blood.repository.HospitalRepository;
import com.example.blood.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Seeds fictional demo users, donor profiles, hospitals, and blood stock
 * (expanded in Phase 7 with full demo dataset) under the "demo" profile.
 */
@Component
@Profile("demo")
public class DemoDataSeeder {

    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodStockRepository bloodStockRepository;

    public DemoDataSeeder(
            UserRepository userRepository,
            DonorProfileRepository donorProfileRepository,
            HospitalRepository hospitalRepository,
            BloodStockRepository bloodStockRepository
    ) {
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.hospitalRepository = hospitalRepository;
        this.bloodStockRepository = bloodStockRepository;
    }

    @PostConstruct
    public void seedInitialData() {
        seedBaseUsers();
        seedBaseDonorProfiles();
        seedBaseHospitalsAndStock();
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

    public void seedBaseHospitalsAndStock() {
        Hospital h1 = saveHospitalIfAbsent(
                "hosp-city-care",
                "hospital-1",
                "City Care Super Specialty Hospital",
                "MH-MUM-2021-001",
                "Dr. Sunita Deshmukh",
                "Plot 14, Veer Savarkar Marg, Dadar West",
                "Mumbai",
                "Maharashtra",
                "400028",
                "9820112233",
                19.0178,
                72.8478,
                HospitalStatus.VERIFIED
        );
        saveStockIfAbsent(h1.getId(), BloodGroup.O_POS, 14, 5, Instant.now());
        saveStockIfAbsent(h1.getId(), BloodGroup.A_POS, 9, 4, Instant.now());
        saveStockIfAbsent(h1.getId(), BloodGroup.O_NEG, 3, 4, Instant.now());

        saveHospitalIfAbsent(
                "hosp-janata-seva",
                "hospital-2",
                "Janata Seva Kendra Hospital",
                "DL-ND-2023-108",
                "Dr. Rajesh Khanna",
                "Block C, Connaught Place",
                "Delhi",
                "Delhi",
                "110001",
                "9811223344",
                28.6315,
                77.2167,
                HospitalStatus.PENDING
        );
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

    private Hospital saveHospitalIfAbsent(
            String id,
            String ownerUserId,
            String name,
            String regNo,
            String contactPerson,
            String address,
            String city,
            String state,
            String pincode,
            String phone,
            Double lat,
            Double lng,
            HospitalStatus status
    ) {
        return hospitalRepository.findByOwnerUserId(ownerUserId).orElseGet(() -> {
            Hospital h = new Hospital();
            h.setId(id);
            h.setOwnerUserId(ownerUserId);
            h.setName(name);
            h.setRegistrationNumber(regNo);
            h.setContactPerson(contactPerson);
            h.setAddress(address);
            h.setCity(city);
            h.setState(state);
            h.setPincode(pincode);
            h.setPhone(phone);
            h.setLatitude(lat);
            h.setLongitude(lng);
            h.setStatus(status);
            return hospitalRepository.save(h);
        });
    }

    private void saveStockIfAbsent(String hospitalId, BloodGroup bloodGroup, int units, int minThreshold, Instant updatedAt) {
        if (bloodStockRepository.findByHospitalIdAndBloodGroup(hospitalId, bloodGroup).isEmpty()) {
            BloodStock stock = new BloodStock();
            stock.setHospitalId(hospitalId);
            stock.setBloodGroup(bloodGroup);
            stock.setUnits(units);
            stock.setMinThreshold(minThreshold);
            stock.setUpdatedAt(updatedAt);
            bloodStockRepository.save(stock);
        }
    }
}
