package com.example.blood;

import com.example.blood.repository.BloodRequestRepository;
import com.example.blood.repository.DonorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class Phase1ServiceAndControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @BeforeEach
    void setUp() {
        donorProfileRepository.deleteAll();
        bloodRequestRepository.deleteAll();
    }

    @Test
    void donorFiltersWorkIndependentlyAndTogetherWithCaseInsensitivityAndMaskPhone() throws Exception {
        createDonor("Ananya Nair", "A+", "Bengaluru", "9876543210", 25);
        createDonor("Rohan Mehta", "O-", "Bengaluru", "9876543211", 30);
        createDonor("Priya Singh", "A+", "Delhi", "9876543212", 29);

        // Filter by bloodGroup only (A+)
        mockMvc.perform(get("/api/donors").param("bloodGroup", "A+"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));

        // Filter by city only (case-insensitive)
        mockMvc.perform(get("/api/donors").param("city", "bengaluru"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Combined filter (bloodGroup=A+ & city=BENGALURU) + pagination
        mockMvc.perform(get("/api/donors")
                        .param("bloodGroup", "A+")
                        .param("city", "BENGALURU")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Ananya Nair"))
                .andExpect(jsonPath("$.content[0].bloodGroup").value("A+"))
                .andExpect(jsonPath("$.content[0].phone").value("******3210"))
                .andExpect(jsonPath("$.content[0].internalAuditNote").doesNotExist());
    }

    @Test
    void validationErrorsInvalidEnumsMalformedJsonAndPaginationBoundsReturnClear400() throws Exception {
        // Missing required fields & invalid phone
        String invalidDonor = """
                {
                  "name": "",
                  "bloodGroup": "A+",
                  "city": "Mumbai",
                  "phone": "12345",
                  "age": 25
                }
                """;
        mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidDonor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.fields.name").exists())
                .andExpect(jsonPath("$.details.fields.phone").exists());

        // Underage donor (17)
        String underageDonor = """
                {
                  "name": "Young Donor",
                  "bloodGroup": "B+",
                  "city": "Mumbai",
                  "phone": "9876543210",
                  "age": 17
                }
                """;
        mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(underageDonor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // Invalid enum value in JSON
        String invalidEnum = """
                {
                  "name": "Test Donor",
                  "bloodGroup": "Z+",
                  "city": "Mumbai",
                  "phone": "9876543210",
                  "age": 25
                }
                """;
        mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEnum))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ENUM_VALUE"));

        // Invalid enum value in query parameter
        mockMvc.perform(get("/api/donors").param("bloodGroup", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ENUM_VALUE"));

        // Malformed JSON
        mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ malformed json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));

        // Out-of-bounds page size (> 50)
        mockMvc.perform(get("/api/donors").param("page", "0").param("size", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // Not found record
        mockMvc.perform(get("/api/donors/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private void createDonor(String name, String bloodGroup, String city, String phone, int age) throws Exception {
        String payload = """
                {
                  "name": "%s",
                  "bloodGroup": "%s",
                  "city": "%s",
                  "phone": "%s",
                  "age": %d
                }
                """.formatted(name, bloodGroup, city, phone, age);
        mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }
}
