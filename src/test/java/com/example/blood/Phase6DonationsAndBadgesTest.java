package com.example.blood;

import com.example.blood.config.DemoDataSeeder;
import com.example.blood.model.BadgeCode;
import com.example.blood.model.Donation;
import com.example.blood.model.DonationStatus;
import com.example.blood.model.DonorProfile;
import com.example.blood.repository.BloodRequestRepository;
import com.example.blood.repository.DonationRepository;
import com.example.blood.repository.DonorProfileRepository;
import com.example.blood.repository.HospitalRepository;
import com.example.blood.service.BadgeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class Phase6DonationsAndBadgesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private BadgeService badgeService;

    @Autowired
    private DemoDataSeeder demoDataSeeder;

    @BeforeEach
    void setUp() {
        donationRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        donorProfileRepository.deleteAll();
        hospitalRepository.deleteAll();
        demoDataSeeder.seedInitialData();
    }

    @Test
    void pendingClaimDoesNotCountUntilVerifiedByOwningHospitalAndFulfillsRequestAndAwardsBadges() throws Exception {
        // Create a CRITICAL O+ request for 1 unit at hosp-city-care (owned by hospital-1)
        String reqBody = """
                {
                  "patientName": "Critical Patient",
                  "bloodGroup": "O+",
                  "unitsNeeded": 1,
                  "hospitalId": "hosp-city-care",
                  "urgency": "CRITICAL",
                  "neededBy": "%s"
                }
                """.formatted(LocalDate.now().plusDays(1));

        MvcResult reqRes = mockMvc.perform(post("/api/requests")
                        .header("X-Demo-User-Id", "hospital-1")
                        .header("X-Demo-Role", "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isOk())
                .andReturn();
        String requestId = objectMapper.readTree(reqRes.getResponse().getContentAsString()).get("id").asText();

        // Future donation date rejected -> 400 VALIDATION_ERROR
        String futureClaim = """
                {
                  "hospitalId": "hosp-city-care",
                  "requestId": "%s",
                  "units": 1,
                  "donatedAt": "%s"
                }
                """.formatted(requestId, LocalDate.now().plusDays(2));

        mockMvc.perform(post("/api/donors/me/donations")
                        .header("X-Demo-User-Id", "donor-1")
                        .header("X-Demo-Role", "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(futureClaim))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // Donor-1 (O+, last declared donation 120 days ago) submits valid PENDING claim for today
        String validClaim = """
                {
                  "hospitalId": "hosp-city-care",
                  "requestId": "%s",
                  "units": 1,
                  "donatedAt": "%s"
                }
                """.formatted(requestId, LocalDate.now());

        MvcResult claimRes = mockMvc.perform(post("/api/donors/me/donations")
                        .header("X-Demo-User-Id", "donor-1")
                        .header("X-Demo-Role", "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validClaim))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        String donationId = objectMapper.readTree(claimRes.getResponse().getContentAsString()).get("id").asText();

        // While PENDING: summary shows 0 verified donations, 1 pending claim, 0 badges, still eligible
        mockMvc.perform(get("/api/donors/me/summary")
                        .header("X-Demo-User-Id", "donor-1")
                        .header("X-Demo-Role", "DONOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVerifiedDonations").value(0))
                .andExpect(jsonPath("$.pendingClaimsCount").value(1))
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.badges", hasSize(0)));

        // Unverified hospital-2 cannot verify hospital-1's donation
        mockMvc.perform(patch("/api/hospitals/me/donations/" + donationId + "/verify")
                        .header("X-Demo-User-Id", "hospital-2")
                        .header("X-Demo-Role", "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VERIFIED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HOSPITAL_NOT_VERIFIED"));

        // Owning verified hospital-1 verifies the claim
        mockMvc.perform(patch("/api/hospitals/me/donations/" + donationId + "/verify")
                        .header("X-Demo-User-Id", "hospital-1")
                        .header("X-Demo-Role", "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VERIFIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));

        // Linked blood request is now FULFILLED (1/1 unit)
        mockMvc.perform(get("/api/requests/" + requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitsFulfilled").value(1))
                .andExpect(jsonPath("$.status").value("FULFILLED"));

        // Donor-1 summary now has 1 verified donation, eligible=false (90-day cooldown), FIRST_DROP + EMERGENCY_RESPONDER
        mockMvc.perform(get("/api/donors/me/summary")
                        .header("X-Demo-User-Id", "donor-1")
                        .header("X-Demo-Role", "DONOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVerifiedDonations").value(1))
                .andExpect(jsonPath("$.totalVerifiedUnits").value(1))
                .andExpect(jsonPath("$.pendingClaimsCount").value(0))
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.eligibilityReason").value("DONATION_GAP_ACTIVE"))
                .andExpect(jsonPath("$.badges", hasSize(2)))
                .andExpect(jsonPath("$.badges[*].code", containsInAnyOrder("FIRST_DROP", "EMERGENCY_RESPONDER")));

        // Re-verifying already VERIFIED donation fails with 409 INVALID_STATE_TRANSITION
        mockMvc.perform(patch("/api/hospitals/me/donations/" + donationId + "/verify")
                        .header("X-Demo-User-Id", "hospital-1")
                        .header("X-Demo-Role", "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VERIFIED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void hospitalRecordEnforces90DayGapAndMilestoneBadgesAreIdempotent() throws Exception {
        // Clear declared last donation on donor-1 so we can test historical verified donations cleanly
        DonorProfile donor1 = donorProfileRepository.findByUserId("donor-1").orElseThrow();
        donor1.setDeclaredLastDonationDate(null);
        donorProfileRepository.save(donor1);

        // Hospital-1 records a verified donation today for donor-1
        String recordToday = """
                {
                  "donorUserId": "donor-1",
                  "units": 1,
                  "donatedAt": "%s"
                }
                """.formatted(LocalDate.now());

        mockMvc.perform(post("/api/hospitals/me/donations")
                        .header("X-Demo-User-Id", "hospital-1")
                        .header("X-Demo-Role", "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordToday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));

        // Hospital-1 tries to record another donation for donor-1 within 90 days -> 400 INELIGIBLE_DONOR
        String recordWithinGap = """
                {
                  "donorUserId": "donor-1",
                  "units": 1,
                  "donatedAt": "%s"
                }
                """.formatted(LocalDate.now().minusDays(30));

        mockMvc.perform(post("/api/hospitals/me/donations")
                        .header("X-Demo-User-Id", "hospital-1")
                        .header("X-Demo-Role", "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordWithinGap))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INELIGIBLE_DONOR"));

        // Seed 9 additional historical verified donations spaced 100 days apart (total 10 verified donations)
        for (int i = 1; i <= 9; i++) {
            Donation d = new Donation();
            d.setDonorUserId("donor-1");
            d.setHospitalId("hosp-city-care");
            d.setHospitalName("City Care Super Specialty Hospital");
            d.setBloodGroup(donor1.getBloodGroup());
            d.setUnits(1);
            d.setDonatedAt(LocalDate.now().minusDays(100L * i));
            d.setStatus(DonationStatus.VERIFIED);
            donationRepository.save(d);
        }

        // Evaluate badges twice to verify idempotency
        DonorProfile refreshed = donorProfileRepository.findByUserId("donor-1").orElseThrow();
        badgeService.evaluateAndAwardBadges(refreshed, null);
        badgeService.evaluateAndAwardBadges(refreshed, null);

        DonorProfile afterBadges = donorProfileRepository.findByUserId("donor-1").orElseThrow();
        assertThat(afterBadges.getBadges())
                .extracting(b -> b.getCode())
                .containsExactlyInAnyOrder(
                        BadgeCode.FIRST_DROP,
                        BadgeCode.REGULAR,
                        BadgeCode.HERO,
                        BadgeCode.LIFESAVER
                );
    }
}
