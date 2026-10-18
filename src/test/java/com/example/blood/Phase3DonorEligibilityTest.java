package com.example.blood;

import com.example.blood.config.DemoDataSeeder;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.Donation;
import com.example.blood.model.DonationStatus;
import com.example.blood.model.DonorProfile;
import com.example.blood.repository.DonationRepository;
import com.example.blood.repository.InMemoryDonationRepository;
import com.example.blood.service.EligibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class Phase3DonorEligibilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoDataSeeder demoDataSeeder;

    @BeforeEach
    void setUp() {
        demoDataSeeder.seedInitialData();
    }

    @Test
    void eligibilityServiceEvaluatesExactAgeBoundaries17To66() {
        DonationRepository donationRepository = new InMemoryDonationRepository();
        EligibilityService service = new EligibilityService(donationRepository, 18, 65, 90);
        LocalDate ref = LocalDate.of(2026, 10, 18);

        // Age 17 (1 day before 18th birthday) -> Ineligible
        DonorProfile age17 = new DonorProfile();
        age17.setDateOfBirth(ref.minusYears(18).plusDays(1));
        EligibilityService.EligibilityStatus status17 = service.evaluate(age17, ref);
        assertThat(status17.eligible()).isFalse();
        assertThat(status17.ageYears()).isEqualTo(17);
        assertThat(status17.reason()).isEqualTo("UNDER_MINIMUM_AGE");
        assertThat(status17.nextEligibleDate()).isEqualTo(ref.plusDays(1));

        // Age 18 (exact 18th birthday) -> Eligible
        DonorProfile age18 = new DonorProfile();
        age18.setDateOfBirth(ref.minusYears(18));
        EligibilityService.EligibilityStatus status18 = service.evaluate(age18, ref);
        assertThat(status18.eligible()).isTrue();
        assertThat(status18.ageYears()).isEqualTo(18);
        assertThat(status18.reason()).isEqualTo("ELIGIBLE");

        // Age 65 -> Eligible
        DonorProfile age65 = new DonorProfile();
        age65.setDateOfBirth(ref.minusYears(65));
        EligibilityService.EligibilityStatus status65 = service.evaluate(age65, ref);
        assertThat(status65.eligible()).isTrue();
        assertThat(status65.ageYears()).isEqualTo(65);
        assertThat(status65.reason()).isEqualTo("ELIGIBLE");

        // Age 66 -> Ineligible
        DonorProfile age66 = new DonorProfile();
        age66.setDateOfBirth(ref.minusYears(66));
        EligibilityService.EligibilityStatus status66 = service.evaluate(age66, ref);
        assertThat(status66.eligible()).isFalse();
        assertThat(status66.ageYears()).isEqualTo(66);
        assertThat(status66.reason()).isEqualTo("OVER_MAXIMUM_AGE");
        assertThat(status66.nextEligibleDate()).isNull();
    }

    @Test
    void eligibilityServiceEvaluatesDonationGapBoundariesMinus1ZeroPlus1Days() {
        DonationRepository donationRepository = new InMemoryDonationRepository();
        EligibilityService service = new EligibilityService(donationRepository, 18, 65, 90);
        LocalDate ref = LocalDate.of(2026, 10, 18);

        DonorProfile donor = new DonorProfile();
        donor.setUserId("donor-gap-test");
        donor.setDateOfBirth(LocalDate.of(1995, 1, 1));

        // 89 days ago (-1 day from 90-day requirement) -> Ineligible, eligible tomorrow
        donor.setDeclaredLastDonationDate(ref.minusDays(89));
        EligibilityService.EligibilityStatus gap89 = service.evaluate(donor, ref);
        assertThat(gap89.eligible()).isFalse();
        assertThat(gap89.reason()).isEqualTo("DONATION_GAP_ACTIVE");
        assertThat(gap89.nextEligibleDate()).isEqualTo(ref.plusDays(1));

        // 90 days ago (0 day boundary - exact 90th day) -> Eligible today
        donor.setDeclaredLastDonationDate(ref.minusDays(90));
        EligibilityService.EligibilityStatus gap90 = service.evaluate(donor, ref);
        assertThat(gap90.eligible()).isTrue();
        assertThat(gap90.reason()).isEqualTo("ELIGIBLE");
        assertThat(gap90.nextEligibleDate()).isEqualTo(ref);

        // 91 days ago (+1 day past requirement) -> Eligible today
        donor.setDeclaredLastDonationDate(ref.minusDays(91));
        EligibilityService.EligibilityStatus gap91 = service.evaluate(donor, ref);
        assertThat(gap91.eligible()).isTrue();
        assertThat(gap91.reason()).isEqualTo("ELIGIBLE");

        // Verified donation in repository (30 days ago) overrides older declared date (91 days ago)
        Donation verified = new Donation();
        verified.setDonorUserId("donor-gap-test");
        verified.setBloodGroup(BloodGroup.O_POS);
        verified.setUnits(1);
        verified.setDonationDate(ref.minusDays(30));
        verified.setStatus(DonationStatus.VERIFIED);
        donationRepository.save(verified);

        EligibilityService.EligibilityStatus withVerified = service.evaluate(donor, ref);
        assertThat(withVerified.eligible()).isFalse();
        assertThat(withVerified.effectiveLastDonationDate()).isEqualTo(ref.minusDays(30));
        assertThat(withVerified.nextEligibleDate()).isEqualTo(ref.plusDays(60));
    }

    @Test
    void publicSearchMasksPhoneAndCoordinatesWhileDonorsMeReturnsFullProfile() throws Exception {
        // Owner GET /api/donors/me sees full unmasked phone and coordinates
        mockMvc.perform(get("/api/donors/me")
                        .header("X-Demo-User-Id", "donor-1")
                        .header("X-Demo-Role", "DONOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("donor-1"))
                .andExpect(jsonPath("$.phone").value("9876543210"))
                .andExpect(jsonPath("$.latitude").value(19.0760))
                .andExpect(jsonPath("$.eligible").value(true));

        // Public GET /api/donors masks phone and omits exact coordinates
        mockMvc.perform(get("/api/donors").param("city", "Mumbai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].phone").value("******3210"))
                .andExpect(jsonPath("$.content[0].latitude").isEmpty())
                .andExpect(jsonPath("$.content[0].longitude").isEmpty());
    }

    @Test
    void donorUpdatesOwnProfileAndAvailabilityAndCannotEditAnotherDonorsProfile() throws Exception {
        // Update donor-2's own profile via PUT /api/donors/me
        mockMvc.perform(put("/api/donors/me")
                        .header("X-Demo-User-Id", "donor-2")
                        .header("X-Demo-Role", "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rohan Verma",
                                  "bloodGroup": "A_POS",
                                  "dateOfBirth": "1994-08-20",
                                  "phone": "9812349999",
                                  "city": "New Delhi",
                                  "pincode": "110002",
                                  "available": true,
                                  "medicalConditionsConfirmed": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("donor-2"))
                .andExpect(jsonPath("$.phone").value("9812349999"))
                .andExpect(jsonPath("$.city").value("New Delhi"));

        // Verify donor-1's profile was untouched by donor-2's PUT /api/donors/me
        mockMvc.perform(get("/api/donors/me")
                        .header("X-Demo-User-Id", "donor-1")
                        .header("X-Demo-Role", "DONOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("donor-1"))
                .andExpect(jsonPath("$.phone").value("9876543210"))
                .andExpect(jsonPath("$.city").value("Mumbai"));

        // Toggle donor-1 availability via PATCH /api/donors/me/availability
        mockMvc.perform(patch("/api/donors/me/availability")
                        .header("X-Demo-User-Id", "donor-1")
                        .header("X-Demo-Role", "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "available": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));

        // Restore donor-1 availability to true
        mockMvc.perform(patch("/api/donors/me/availability")
                        .header("X-Demo-User-Id", "donor-1")
                        .header("X-Demo-Role", "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "available": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        // Hospital role cannot access /api/donors/me
        mockMvc.perform(get("/api/donors/me")
                        .header("X-Demo-User-Id", "hospital-1")
                        .header("X-Demo-Role", "HOSPITAL"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
