package com.example.blood;

import com.example.blood.config.DemoDataSeeder;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.BloodStock;
import com.example.blood.repository.BloodStockRepository;
import com.example.blood.repository.HospitalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class Phase4HospitalAndStockTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodStockRepository bloodStockRepository;

    @Autowired
    private DemoDataSeeder demoDataSeeder;

    @BeforeEach
    void setUp() {
        hospitalRepository.deleteAll();
        bloodStockRepository.deleteAll();
        demoDataSeeder.seedInitialData();
    }

    @Test
    void unverifiedHospitalCannotMutateStockUntilApprovedByAdminAndRejectionStoresReason() throws Exception {
        // hospital-2 is PENDING -> attempting to update stock returns 403 HOSPITAL_NOT_VERIFIED
        mockMvc.perform(put("/api/hospitals/me/stock/B_POS")
                        .header("X-Demo-User-Id", "hospital-2")
                        .header("X-Demo-Role", "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "units": 12,
                                  "minThreshold": 4
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HOSPITAL_NOT_VERIFIED"));

        // Admin lists PENDING hospitals -> sees hosp-janata-seva
        mockMvc.perform(get("/api/admin/hospitals")
                        .param("status", "PENDING")
                        .header("X-Demo-User-Id", "admin-1")
                        .header("X-Demo-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("hosp-janata-seva"));

        // Admin rejects hosp-janata-seva with a reason
        mockMvc.perform(post("/api/admin/hospitals/hosp-janata-seva/reject")
                        .header("X-Demo-User-Id", "admin-1")
                        .header("X-Demo-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "License document registration number mismatch"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("License document registration number mismatch"));

        // REJECTED hospital still cannot mutate stock
        mockMvc.perform(put("/api/hospitals/me/stock/B_POS")
                        .header("X-Demo-User-Id", "hospital-2")
                        .header("X-Demo-Role", "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "units": 12,
                                  "minThreshold": 4
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HOSPITAL_NOT_VERIFIED"));

        // Hospital owner edits profile -> status resets from REJECTED to PENDING for re-review
        mockMvc.perform(put("/api/hospitals/me")
                        .header("X-Demo-User-Id", "hospital-2")
                        .header("X-Demo-Role", "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Janata Seva Kendra Hospital",
                                  "registrationNumber": "DL-ND-2023-108-CORRECTED",
                                  "contactPerson": "Dr. Rajesh Khanna",
                                  "address": "Block C, Connaught Place",
                                  "city": "Delhi",
                                  "state": "Delhi",
                                  "pincode": "110001",
                                  "phone": "9811223344",
                                  "latitude": 28.6315,
                                  "longitude": 77.2167
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.rejectionReason").isEmpty());

        // Admin approves hosp-janata-seva
        mockMvc.perform(post("/api/admin/hospitals/hosp-janata-seva/approve")
                        .header("X-Demo-User-Id", "admin-1")
                        .header("X-Demo-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));

        // Now hospital-2 can mutate stock!
        mockMvc.perform(put("/api/hospitals/me/stock/B_POS")
                        .header("X-Demo-User-Id", "hospital-2")
                        .header("X-Demo-Role", "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "units": 12,
                                  "minThreshold": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloodGroup").value("B+"))
                .andExpect(jsonPath("$.units").value(12))
                .andExpect(jsonPath("$.lowStock").value(false));
    }

    @Test
    void publicHospitalAndStockSearchExcludeUnverifiedHospitalsAndUpsertStockCleanly() throws Exception {
        // Public hospital search returns only VERIFIED hospitals (hosp-city-care and hosp-kaveri-blr), excluding PENDING hosp-janata-seva
        mockMvc.perform(get("/api/hospitals").param("city", "Mumbai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value("hosp-city-care"));

        // Add stock to PENDING hospital directly in repository to verify public stock search still excludes it
        BloodStock pendingHospitalStock = new BloodStock();
        pendingHospitalStock.setHospitalId("hosp-janata-seva");
        pendingHospitalStock.setBloodGroup(BloodGroup.O_POS);
        pendingHospitalStock.setUnits(25);
        pendingHospitalStock.setMinThreshold(5);
        pendingHospitalStock.setUpdatedAt(Instant.now());
        bloodStockRepository.save(pendingHospitalStock);

        // Public stock search for O+ only returns VERIFIED hosp-city-care
        mockMvc.perform(get("/api/stock/search")
                        .param("bloodGroup", "O+")
                        .param("lat", "19.0760")
                        .param("lng", "72.8777"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].hospitalId").value("hosp-city-care"))
                .andExpect(jsonPath("$[0].distanceKm").isNumber())
                .andExpect(jsonPath("$[0].stale").value(false));

        // Upserting duplicate hospitalId + bloodGroup updates existing entry instead of creating a duplicate
        mockMvc.perform(put("/api/hospitals/me/stock/O_POS")
                        .header("X-Demo-User-Id", "hospital-1")
                        .header("X-Demo-Role", "HOSPITAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "units": 4,
                                  "minThreshold": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.units").value(4))
                .andExpect(jsonPath("$.lowStock").value(true));

        assertThat(bloodStockRepository.findByHospitalId("hosp-city-care"))
                .filteredOn(s -> s.getBloodGroup() == BloodGroup.O_POS)
                .hasSize(1);

        // Mark O_NEG stock as updated 25 hours ago to verify stale flag
        BloodStock oNeg = bloodStockRepository
                .findByHospitalIdAndBloodGroup("hosp-city-care", BloodGroup.O_NEG)
                .orElseThrow();
        oNeg.setUpdatedAt(Instant.now().minus(Duration.ofHours(25)));
        bloodStockRepository.save(oNeg);

        mockMvc.perform(get("/api/stock/search").param("bloodGroup", "O-").param("city", "Mumbai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stale").value(true));

        // Delete O_NEG stock entry
        mockMvc.perform(delete("/api/hospitals/me/stock/O_NEG")
                        .header("X-Demo-User-Id", "hospital-1")
                        .header("X-Demo-Role", "HOSPITAL"))
                .andExpect(status().isNoContent());

        assertThat(bloodStockRepository.findByHospitalIdAndBloodGroup("hosp-city-care", BloodGroup.O_NEG))
                .isEmpty();
    }
}
