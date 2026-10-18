package com.example.blood;

import com.example.blood.config.DemoDataSeeder;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.DonorProfile;
import com.example.blood.model.NotificationType;
import com.example.blood.repository.BloodRequestRepository;
import com.example.blood.repository.BloodStockRepository;
import com.example.blood.repository.DonationRepository;
import com.example.blood.repository.DonorProfileRepository;
import com.example.blood.repository.HospitalRepository;
import com.example.blood.repository.NotificationRepository;
import com.example.blood.repository.UserRepository;
import com.example.blood.security.DevStubCurrentUserProvider;
import com.example.blood.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class Phase7NotificationsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodStockRepository bloodStockRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DemoDataSeeder demoDataSeeder;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        donationRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        bloodStockRepository.deleteAll();
        hospitalRepository.deleteAll();
        donorProfileRepository.deleteAll();
        userRepository.deleteAll();
        demoDataSeeder.seedInitialData();
    }

    @Test
    void requestCreationAndDonorAcceptanceGenerateNotifications() throws Exception {
        notificationRepository.deleteAll();

        // Hospital-1 creates an O+ request in Mumbai -> donor-1 (O+, Mumbai, eligible) should receive REQUEST_MATCH
        String createPayload = """
                {
                  "patientName": "Vikramaditya Rao",
                  "bloodGroup": "O+",
                  "unitsNeeded": 2,
                  "hospitalId": "hosp-city-care",
                  "city": "Mumbai",
                  "urgency": "CRITICAL",
                  "neededBy": "%s"
                }
                """.formatted(LocalDate.now().plusDays(2));

        String res = mockMvc.perform(post("/api/requests")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "hospital-1")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Check donor-1 unread count and notification list
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "donor-1")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "DONOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        mockMvc.perform(get("/api/notifications")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "donor-1")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "DONOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("REQUEST_MATCH"))
                .andExpect(jsonPath("$.content[0].message", containsString("O+ blood needed (CRITICAL)")));

        String requestId = bloodRequestRepository.findAll().stream()
                .filter(r -> "Vikramaditya Rao".equals(r.getPatientName()))
                .findFirst().orElseThrow().getId();

        // Donor-1 accepts the request -> hospital-1 receives DONOR_ACCEPTED
        mockMvc.perform(post("/api/requests/" + requestId + "/respond")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "donor-1")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACCEPTED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "hospital-1")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "HOSPITAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].type", hasItem("DONOR_ACCEPTED")))
                .andExpect(jsonPath("$.content[0].message", containsString("Priya Sharma")));
    }

    @Test
    void requestMatchFanOutIsBoundedToMax20Donors() throws Exception {
        notificationRepository.deleteAll();

        // Create 25 eligible O- donors in Mumbai
        for (int i = 1; i <= 25; i++) {
            DonorProfile d = new DonorProfile();
            d.setUserId("bulk-donor-" + i);
            d.setName("Bulk Donor " + i);
            d.setBloodGroup(BloodGroup.O_NEG);
            d.setDateOfBirth(LocalDate.of(1995, 1, 1));
            d.setAge(31);
            d.setPhone("98000000" + String.format("%02d", i));
            d.setCity("Mumbai");
            d.setPincode("400001");
            d.setLatitude(19.0178);
            d.setLongitude(72.8478);
            d.setAvailable(true);
            d.setMedicalConditionsConfirmed(true);
            donorProfileRepository.save(d);
        }

        String createPayload = """
                {
                  "patientName": "Fanout Test Patient",
                  "bloodGroup": "O+",
                  "unitsNeeded": 1,
                  "hospitalId": "hosp-city-care",
                  "city": "Mumbai",
                  "urgency": "URGENT",
                  "neededBy": "%s"
                }
                """.formatted(LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/requests")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "hospital-1")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isOk());

        long matchNotifCount = donorProfileRepository.findAll().stream()
                .flatMap(d -> notificationRepository.findByUserId(d.getUserId()).stream())
                .filter(n -> n.getType() == NotificationType.REQUEST_MATCH)
                .count();
        assertThat(matchNotifCount).isEqualTo(NotificationService.MAX_MATCH_NOTIFICATIONS_PER_REQUEST);
    }

    @Test
    void lowStockHospitalVerificationAndDonationBadgeNotificationsWork() throws Exception {
        notificationRepository.deleteAll();

        // 1. Hospital-1 updates stock below threshold -> STOCK_LOW notification
        mockMvc.perform(put("/api/hospitals/me/stock/AB-")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "hospital-1")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "units": 1,
                                  "minThreshold": 5
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "hospital-1")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "HOSPITAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].type", hasItem("STOCK_LOW")));

        // 2. Admin approves pending hospital (hosp-janata-seva owned by hospital-2, preferred language "hi")
        mockMvc.perform(post("/api/admin/hospitals/hosp-janata-seva/approve")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "admin-1")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "ADMIN"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "hospital-2")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "HOSPITAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("HOSPITAL_APPROVED"))
                .andExpect(jsonPath("$.content[0].message", containsString("सत्यापित")));

        // 3. Hospital-1 records a verified donation for donor-5 (first donation -> DONATION_VERIFIED + BADGE_EARNED)
        mockMvc.perform(post("/api/hospitals/me/donations")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "hospital-1")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "donorUserId": "donor-5",
                                  "units": 1,
                                  "donatedAt": "%s"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "donor-5")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "DONOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].type", hasItem("DONATION_VERIFIED")))
                .andExpect(jsonPath("$.content[*].type", hasItem("BADGE_EARNED")));
    }

    @Test
    void markReadOwnershipAndLanguageSwitchWork() throws Exception {
        // Seeded notification notif-demo-1 belongs to donor-1
        // Another user (donor-2) cannot mark donor-1's notification as read
        mockMvc.perform(patch("/api/notifications/notif-demo-1/read")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "donor-2")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "DONOR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // Switch donor-1 preferred language to Hindi ("hi")
        mockMvc.perform(patch("/api/users/me/language")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "donor-1")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredLanguage\": \"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredLanguage").value("hi"));

        // Donor-1 fetches notifications in Hindi
        mockMvc.perform(get("/api/notifications")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "donor-1")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "DONOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].message", containsString("रक्त की")));

        // Mark single notification read
        mockMvc.perform(patch("/api/notifications/notif-demo-1/read")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "donor-1")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "DONOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        // Mark all read
        mockMvc.perform(patch("/api/notifications/read-all")
                        .header(DevStubCurrentUserProvider.HEADER_USER_ID, "donor-1")
                        .header(DevStubCurrentUserProvider.HEADER_ROLE, "DONOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(0));
    }

    @Test
    void publicStatsEndpointReturnsSeededSummary() throws Exception {
        mockMvc.perform(get("/api/public/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredDonors").value(greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.availableDonors").value(greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.verifiedHospitals").value(2))
                .andExpect(jsonPath("$.totalUnitsInStock").value(greaterThanOrEqualTo(50)))
                .andExpect(jsonPath("$.openRequests").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.verifiedDonations").value(greaterThanOrEqualTo(1)));
    }
}
