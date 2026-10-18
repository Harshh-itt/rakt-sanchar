package com.example.blood;

import com.example.blood.config.DemoDataSeeder;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.BloodRequest;
import com.example.blood.model.DonorProfile;
import com.example.blood.model.RequestStatus;
import com.example.blood.model.Urgency;
import com.example.blood.repository.BloodRequestRepository;
import com.example.blood.repository.DonationRepository;
import com.example.blood.repository.DonorProfileRepository;
import com.example.blood.repository.HospitalRepository;
import com.example.blood.service.BloodRequestService;
import com.example.blood.service.CompatibilityService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class Phase5MatchingAndRequestsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompatibilityService compatibilityService;

    @Autowired
    private BloodRequestService bloodRequestService;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DemoDataSeeder demoDataSeeder;

    @BeforeEach
    void setUp() {
        bloodRequestRepository.deleteAll();
        donorProfileRepository.deleteAll();
        hospitalRepository.deleteAll();
        donationRepository.deleteAll();
        demoDataSeeder.seedInitialData();
    }

    @Test
    void fullEightByEightBloodGroupCompatibilityMatrixIsAccurate() {
        // O- is universal donor
        for (BloodGroup recipient : BloodGroup.values()) {
            assertThat(compatibilityService.canDonateTo(BloodGroup.O_NEG, recipient))
                    .as("O- should donate to %s", recipient)
                    .isTrue();
        }

        // AB+ is universal recipient
        for (BloodGroup donor : BloodGroup.values()) {
            assertThat(compatibilityService.canDonateTo(donor, BloodGroup.AB_POS))
                    .as("%s should donate to AB+", donor)
                    .isTrue();
        }

        // Exact matches are always compatible
        for (BloodGroup bg : BloodGroup.values()) {
            assertThat(compatibilityService.canDonateTo(bg, bg)).isTrue();
            assertThat(compatibilityService.isExactMatch(bg, bg)).isTrue();
        }

        // Specific positive/negative and ABO checks
        assertThat(compatibilityService.canDonateTo(BloodGroup.O_POS, BloodGroup.A_POS)).isTrue();
        assertThat(compatibilityService.canDonateTo(BloodGroup.O_POS, BloodGroup.A_NEG)).isFalse();
        assertThat(compatibilityService.canDonateTo(BloodGroup.A_NEG, BloodGroup.A_POS)).isTrue();
        assertThat(compatibilityService.canDonateTo(BloodGroup.A_NEG, BloodGroup.AB_NEG)).isTrue();
        assertThat(compatibilityService.canDonateTo(BloodGroup.A_POS, BloodGroup.B_POS)).isFalse();
        assertThat(compatibilityService.canDonateTo(BloodGroup.B_NEG, BloodGroup.AB_POS)).isTrue();
        assertThat(compatibilityService.canDonateTo(BloodGroup.AB_NEG, BloodGroup.A_NEG)).isFalse();
        assertThat(compatibilityService.canDonateTo(BloodGroup.AB_POS, BloodGroup.AB_NEG)).isFalse();

        // Exact match comes first in compatible donor groups
        List<BloodGroup> aPosDonors = compatibilityService.getCompatibleDonorGroups(BloodGroup.A_POS);
        assertThat(aPosDonors).containsExactlyInAnyOrder(
                BloodGroup.A_POS, BloodGroup.A_NEG, BloodGroup.O_POS, BloodGroup.O_NEG
        );
        assertThat(aPosDonors.get(0)).isEqualTo(BloodGroup.A_POS);
    }

    @Test
    void unverifiedHospitalCannotCreateRequestWhileVerifiedHospitalCan() throws Exception {
        // hospital-2 is PENDING in DemoDataSeeder
        String payload = """
                {
                  "patientName": "Vikram Rao",
                  "bloodGroup": "A+",
                  "unitsNeeded": 2,
                  "urgency": "URGENT",
                  "neededBy": "%s"
                }
                """.formatted(LocalDate.now().plusDays(2));

        mockMvc.perform(post("/api/requests")
                        .header("X-Demo-User-Id", "hospital-2")
                        .header("X-Demo-Role", "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HOSPITAL_NOT_VERIFIED"));

        // hospital-1 is VERIFIED in DemoDataSeeder
        mockMvc.perform(post("/api/requests")
                        .header("X-Demo-User-Id", "hospital-1")
                        .header("X-Demo-Role", "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalId").value("hosp-city-care"))
                .andExpect(jsonPath("$.hospital").value("City Care Super Specialty Hospital"))
                .andExpect(jsonPath("$.city").value("Mumbai"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void matchingRanksExactMatchFirstThenByDistanceAndRespectsPrivacyAndAcceptPhoneReveal() throws Exception {
        // Clear seeded donors and set up controlled Mumbai donors around City Care Hospital (19.0178, 72.8478)
        donorProfileRepository.deleteAll();

        // Donor A: Exact match A+, ~6.5 km away (Bandra: 19.0596, 72.8295)
        saveDonor("donor-a-far", "Kavita Joshi (A+ Far)", BloodGroup.A_POS, "Mumbai",
                "9870001111", 19.0596, 72.8295, true, LocalDate.now().minusDays(120));

        // Donor B: Exact match A+, ~1.5 km away (Prabhadevi: 19.0144, 72.8302)
        saveDonor("donor-a-near", "Arjun Kulkarni (A+ Near)", BloodGroup.A_POS, "Mumbai",
                "9870002222", 19.0144, 72.8302, true, LocalDate.now().minusDays(150));

        // Donor C: Compatible O-, ~0.5 km away (Dadar: 19.0185, 72.8440)
        saveDonor("donor-o-neg", "Meera Nair (O- Compatible)", BloodGroup.O_NEG, "Mumbai",
                "9870003333", 19.0185, 72.8440, true, null);

        // Donor D: Ineligible A+ (donated 20 days ago)
        saveDonor("donor-ineligible", "Recent Donor", BloodGroup.A_POS, "Mumbai",
                "9870004444", 19.0180, 72.8470, true, LocalDate.now().minusDays(20));

        // Donor E: Incompatible B+
        saveDonor("donor-incompatible", "Incompatible B+", BloodGroup.B_POS, "Mumbai",
                "9870005555", 19.0180, 72.8470, true, null);

        // Create request for A+ at hosp-city-care (owned by hospital-1)
        String reqPayload = """
                {
                  "patientName": "Suresh Patil",
                  "bloodGroup": "A+",
                  "unitsNeeded": 2,
                  "hospitalId": "hosp-city-care",
                  "urgency": "CRITICAL",
                  "neededBy": "%s"
                }
                """.formatted(LocalDate.now().plusDays(1));

        MvcResult createRes = mockMvc.perform(post("/api/requests")
                        .header("X-Demo-User-Id", "hospital-1")
                        .header("X-Demo-Role", "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode created = objectMapper.readTree(createRes.getResponse().getContentAsString());
        String requestId = created.get("id").asText();

        // Unrelated donor cannot view matches -> FORBIDDEN
        mockMvc.perform(get("/api/requests/" + requestId + "/matches")
                        .header("X-Demo-User-Id", "donor-a-near")
                        .header("X-Demo-Role", "DONOR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // Hospital owner views matches: should see 3 matches in order:
        // 1. Arjun (A+ exact, ~1.88km)
        // 2. Kavita (A+ exact, ~5.03km)
        // 3. Meera (O- compatible, ~0.41km)
        // Phones must all be masked initially
        mockMvc.perform(get("/api/requests/" + requestId + "/matches")
                        .header("X-Demo-User-Id", "hospital-1")
                        .header("X-Demo-Role", "HOSPITAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].donorUserId").value("donor-a-near"))
                .andExpect(jsonPath("$[0].exactMatch").value(true))
                .andExpect(jsonPath("$[0].phone").value("******2222"))
                .andExpect(jsonPath("$[1].donorUserId").value("donor-a-far"))
                .andExpect(jsonPath("$[1].exactMatch").value(true))
                .andExpect(jsonPath("$[1].phone").value("******1111"))
                .andExpect(jsonPath("$[2].donorUserId").value("donor-o-neg"))
                .andExpect(jsonPath("$[2].exactMatch").value(false))
                .andExpect(jsonPath("$[2].phone").value("******3333"));

        // Incompatible donor tries to respond -> 400 INCOMPATIBLE_BLOOD_GROUP
        mockMvc.perform(post("/api/requests/" + requestId + "/respond")
                        .header("X-Demo-User-Id", "donor-incompatible")
                        .header("X-Demo-Role", "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INCOMPATIBLE_BLOOD_GROUP"));

        // Ineligible donor tries to accept -> 400 INELIGIBLE_DONOR
        mockMvc.perform(post("/api/requests/" + requestId + "/respond")
                        .header("X-Demo-User-Id", "donor-ineligible")
                        .header("X-Demo-Role", "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INELIGIBLE_DONOR"));

        // Eligible donor-a-near accepts the request
        mockMvc.perform(post("/api/requests/" + requestId + "/respond")
                        .header("X-Demo-User-Id", "donor-a-near")
                        .header("X-Demo-Role", "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorResponses", hasSize(1)))
                .andExpect(jsonPath("$.donorResponses[0].status").value("ACCEPTED"));

        // Now hospital owner sees full unmasked phone for donor-a-near in /matches and GET /api/requests/{id}
        mockMvc.perform(get("/api/requests/" + requestId + "/matches")
                        .header("X-Demo-User-Id", "hospital-1")
                        .header("X-Demo-Role", "HOSPITAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].donorUserId").value("donor-a-near"))
                .andExpect(jsonPath("$[0].responseStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$[0].phone").value("9870002222"))
                .andExpect(jsonPath("$[1].phone").value("******1111"));

        // Public / unauthenticated viewer still sees masked phone on GET /api/requests/{id}
        mockMvc.perform(get("/api/requests/" + requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorResponses[0].phone").value("******2222"));
    }

    @Test
    void cancellationEnforcesOwnershipAndStateTransitionAndOverdueExpiryIsIdempotent() throws Exception {
        String reqPayload = """
                {
                  "patientName": "Deepak Mishra",
                  "bloodGroup": "O+",
                  "unitsNeeded": 1,
                  "hospital": "Apollo Clinic",
                  "city": "Mumbai",
                  "urgency": "MEDIUM",
                  "neededBy": "%s"
                }
                """.formatted(LocalDate.now().plusDays(3));

        MvcResult createRes = mockMvc.perform(post("/api/requests")
                        .header("X-Demo-User-Id", "donor-1")
                        .header("X-Demo-Role", "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqPayload))
                .andExpect(status().isOk())
                .andReturn();

        String requestId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        // Another donor cannot cancel donor-1's request
        mockMvc.perform(patch("/api/requests/" + requestId + "/cancel")
                        .header("X-Demo-User-Id", "donor-2")
                        .header("X-Demo-Role", "DONOR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // Requester cancels own request -> CANCELLED
        mockMvc.perform(patch("/api/requests/" + requestId + "/cancel")
                        .header("X-Demo-User-Id", "donor-1")
                        .header("X-Demo-Role", "DONOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Second cancel attempt -> 409 INVALID_STATE_TRANSITION
        mockMvc.perform(patch("/api/requests/" + requestId + "/cancel")
                        .header("X-Demo-User-Id", "donor-1")
                        .header("X-Demo-Role", "DONOR"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

        // Donor trying to respond to a CANCELLED request -> 409 INVALID_STATE_TRANSITION
        mockMvc.perform(post("/api/requests/" + requestId + "/respond")
                        .header("X-Demo-User-Id", "donor-1")
                        .header("X-Demo-Role", "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

        // Overdue request expiry job test
        BloodRequest overdue = new BloodRequest();
        overdue.setPatientName("Overdue Patient");
        overdue.setBloodGroupEnum(BloodGroup.B_POS);
        overdue.setHospital("Metro Hospital");
        overdue.setCity("Delhi");
        overdue.setUrgencyEnum(Urgency.HIGH);
        overdue.setStatus(RequestStatus.OPEN);
        overdue.setNeededBy(LocalDate.now().minusDays(1));
        BloodRequest savedOverdue = bloodRequestRepository.save(overdue);

        int firstRun = bloodRequestService.expireOverdueRequests(LocalDate.now());
        assertThat(firstRun).isEqualTo(1);
        assertThat(bloodRequestRepository.findById(savedOverdue.getId()).orElseThrow().getStatus())
                .isEqualTo(RequestStatus.EXPIRED);

        // Idempotent second run
        int secondRun = bloodRequestService.expireOverdueRequests(LocalDate.now());
        assertThat(secondRun).isZero();
    }

    private void saveDonor(
            String userId,
            String name,
            BloodGroup bg,
            String city,
            String phone,
            Double lat,
            Double lon,
            boolean available,
            LocalDate lastDonation
    ) {
        DonorProfile p = new DonorProfile();
        p.setUserId(userId);
        p.setName(name);
        p.setBloodGroup(bg);
        p.setCity(city);
        p.setPincode("400001");
        p.setPhone(phone);
        p.setDateOfBirth(LocalDate.of(1995, 4, 10));
        p.setAge(30);
        p.setLatitude(lat);
        p.setLongitude(lon);
        p.setAvailable(available);
        p.setMedicalConditionsConfirmed(true);
        p.setDeclaredLastDonationDate(lastDonation);
        donorProfileRepository.save(p);
    }
}
