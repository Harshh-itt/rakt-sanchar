package com.example.blood.config;

import com.example.blood.model.BadgeCode;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.BloodRequest;
import com.example.blood.model.BloodStock;
import com.example.blood.model.Donation;
import com.example.blood.model.DonationStatus;
import com.example.blood.model.DonorProfile;
import com.example.blood.model.EarnedBadge;
import com.example.blood.model.Hospital;
import com.example.blood.model.HospitalStatus;
import com.example.blood.model.Notification;
import com.example.blood.model.NotificationType;
import com.example.blood.model.RequestStatus;
import com.example.blood.model.Role;
import com.example.blood.model.Urgency;
import com.example.blood.model.User;
import com.example.blood.repository.BloodRequestRepository;
import com.example.blood.repository.BloodStockRepository;
import com.example.blood.repository.DonationRepository;
import com.example.blood.repository.DonorProfileRepository;
import com.example.blood.repository.HospitalRepository;
import com.example.blood.repository.NotificationRepository;
import com.example.blood.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Seeds fictional Indian demo users, donor profiles, hospitals, blood stock,
 * blood requests, donations, and in-app notifications under the "demo" profile.
 * All data is held in memory and resets on application restart.
 */
@Component
@Profile("demo")
public class DemoDataSeeder {

    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodStockRepository bloodStockRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonationRepository donationRepository;
    private final NotificationRepository notificationRepository;

    public DemoDataSeeder(
            UserRepository userRepository,
            DonorProfileRepository donorProfileRepository,
            HospitalRepository hospitalRepository,
            BloodStockRepository bloodStockRepository,
            BloodRequestRepository bloodRequestRepository,
            DonationRepository donationRepository,
            NotificationRepository notificationRepository
    ) {
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.hospitalRepository = hospitalRepository;
        this.bloodStockRepository = bloodStockRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.donationRepository = donationRepository;
        this.notificationRepository = notificationRepository;
    }

    @PostConstruct
    public void seedInitialData() {
        seedBaseUsers();
        seedBaseDonorProfiles();
        seedBaseHospitalsAndStock();
        seedDemoRequestsDonationsAndNotifications();
    }

    public void seedBaseUsers() {
        saveUserIfAbsent("admin-1", "Aarav Mehta (System Admin)", Role.ADMIN, true, "en");
        saveUserIfAbsent("donor-1", "Priya Sharma", Role.DONOR, true, "en");
        saveUserIfAbsent("donor-2", "Rohan Verma", Role.DONOR, true, "hi");
        saveUserIfAbsent("donor-3", "Ananya Nair", Role.DONOR, true, "en");
        saveUserIfAbsent("donor-4", "Siddharth Kulkarni", Role.DONOR, true, "en");
        saveUserIfAbsent("donor-5", "Fatima Sheikh", Role.DONOR, true, "hi");
        saveUserIfAbsent("hospital-1", "City Care Super Specialty Hospital", Role.HOSPITAL, true, "en");
        saveUserIfAbsent("hospital-2", "Janata Seva Kendra Hospital", Role.HOSPITAL, true, "hi");
        saveUserIfAbsent("hospital-3", "Kaveri Heart & Trauma Centre", Role.HOSPITAL, true, "en");
    }

    public void seedBaseDonorProfiles() {
        saveDonorIfAbsent("donor-1", "Priya Sharma", BloodGroup.O_POS,
                LocalDate.of(1996, 5, 15), "9876543210", "Mumbai", "400001",
                19.0760, 72.8777, true, LocalDate.now().minusDays(120), List.of());

        saveDonorIfAbsent("donor-2", "Rohan Verma", BloodGroup.A_POS,
                LocalDate.of(1994, 8, 20), "9812345678", "Delhi", "110001",
                28.6139, 77.2090, true, LocalDate.now().minusDays(45), List.of());

        saveDonorIfAbsent("donor-3", "Ananya Nair", BloodGroup.O_NEG,
                LocalDate.of(1997, 2, 11), "9845012345", "Bengaluru", "560001",
                12.9716, 77.5946, true, LocalDate.now().minusDays(140),
                List.of(
                        new EarnedBadge(BadgeCode.FIRST_DROP, Instant.now().minusSeconds(86400L * 140)),
                        new EarnedBadge(BadgeCode.EMERGENCY_RESPONDER, Instant.now().minusSeconds(86400L * 140))
                ));

        saveDonorIfAbsent("donor-4", "Siddharth Kulkarni", BloodGroup.B_POS,
                LocalDate.of(1992, 11, 3), "9820445566", "Hyderabad", "500001",
                17.3850, 78.4867, true, LocalDate.now().minusDays(180), List.of());

        saveDonorIfAbsent("donor-5", "Fatima Sheikh", BloodGroup.AB_POS,
                LocalDate.of(1999, 7, 25), "9890112233", "Pune", "411001",
                18.5204, 73.8567, true, null, List.of());
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
        saveStockIfAbsent(h1.getId(), BloodGroup.B_POS, 11, 5, Instant.now());
        saveStockIfAbsent(h1.getId(), BloodGroup.AB_POS, 6, 3, Instant.now());

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

        Hospital h3 = saveHospitalIfAbsent(
                "hosp-kaveri-blr",
                "hospital-3",
                "Kaveri Heart & Trauma Centre",
                "KA-BLR-2022-045",
                "Dr. Prakash Gowda",
                "100 Feet Road, Indiranagar",
                "Bengaluru",
                "Karnataka",
                "560038",
                "9880112233",
                12.9784,
                77.6408,
                HospitalStatus.VERIFIED
        );
        saveStockIfAbsent(h3.getId(), BloodGroup.A_POS, 12, 4, Instant.now());
        saveStockIfAbsent(h3.getId(), BloodGroup.O_NEG, 2, 4, Instant.now());
        saveStockIfAbsent(h3.getId(), BloodGroup.B_NEG, 4, 3, Instant.now());
    }

    public void seedDemoRequestsDonationsAndNotifications() {
        if (bloodRequestRepository.findById("req-demo-mum-1").isEmpty()) {
            BloodRequest r1 = new BloodRequest();
            r1.setId("req-demo-mum-1");
            r1.setRequesterUserId("hospital-1");
            r1.setPatientName("Meenakshi Joshi");
            r1.setBloodGroupEnum(BloodGroup.O_POS);
            r1.setUnitsNeeded(2);
            r1.setUnitsFulfilled(0);
            r1.setHospitalId("hosp-city-care");
            r1.setHospital("City Care Super Specialty Hospital");
            r1.setCity("Mumbai");
            r1.setHospitalLatitude(19.0178);
            r1.setHospitalLongitude(72.8478);
            r1.setUrgencyEnum(Urgency.CRITICAL);
            r1.setStatus(RequestStatus.OPEN);
            r1.setNeededBy(LocalDate.now().plusDays(1));
            bloodRequestRepository.save(r1);
        }

        if (bloodRequestRepository.findById("req-demo-blr-1").isEmpty()) {
            BloodRequest r2 = new BloodRequest();
            r2.setId("req-demo-blr-1");
            r2.setRequesterUserId("hospital-3");
            r2.setPatientName("Karthik Hegde");
            r2.setBloodGroupEnum(BloodGroup.O_NEG);
            r2.setUnitsNeeded(1);
            r2.setUnitsFulfilled(0);
            r2.setHospitalId("hosp-kaveri-blr");
            r2.setHospital("Kaveri Heart & Trauma Centre");
            r2.setCity("Bengaluru");
            r2.setHospitalLatitude(12.9784);
            r2.setHospitalLongitude(77.6408);
            r2.setUrgencyEnum(Urgency.URGENT);
            r2.setStatus(RequestStatus.OPEN);
            r2.setNeededBy(LocalDate.now().plusDays(2));
            bloodRequestRepository.save(r2);
        }

        if (donationRepository.findById("don-demo-blr-1").isEmpty()) {
            Donation d1 = new Donation();
            d1.setId("don-demo-blr-1");
            d1.setDonorUserId("donor-3");
            d1.setHospitalId("hosp-kaveri-blr");
            d1.setHospitalName("Kaveri Heart & Trauma Centre");
            d1.setBloodGroup(BloodGroup.O_NEG);
            d1.setUnits(1);
            d1.setDonatedAt(LocalDate.now().minusDays(140));
            d1.setStatus(DonationStatus.VERIFIED);
            d1.setVerifiedAt(Instant.now().minusSeconds(86400L * 140));
            d1.setVerifiedByUserId("hospital-3");
            donationRepository.save(d1);
        }

        if (notificationRepository.findById("notif-demo-1").isEmpty()) {
            Notification n1 = new Notification();
            n1.setId("notif-demo-1");
            n1.setUserId("donor-1");
            n1.setType(NotificationType.REQUEST_MATCH);
            n1.setMessageKey("notification.request_match");
            n1.setParams(Map.of(
                    "requestId", "req-demo-mum-1",
                    "bloodGroup", "O+",
                    "urgency", "CRITICAL",
                    "hospital", "City Care Super Specialty Hospital",
                    "city", "Mumbai"
            ));
            n1.setDedupKey("REQUEST_MATCH:req-demo-mum-1:donor-1");
            notificationRepository.save(n1);
        }
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
            LocalDate declaredLastDonation,
            List<EarnedBadge> badges
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
            profile.setBadges(badges);
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
