package com.example.blood;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class Phase0SmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoadsAndExistingEndpointsRespondInDemoProfile() throws Exception {
        String donorPayload = """
                {
                  "name": "Aarav Sharma",
                  "bloodGroup": "A+",
                  "city": "Bengaluru",
                  "phone": "9876543210",
                  "age": 28
                }
                """;

        mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donorPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aarav Sharma"))
                .andExpect(jsonPath("$.bloodGroup").value("A+"));

        mockMvc.perform(get("/api/donors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        String requestPayload = """
                {
                  "patientName": "Riya Verma",
                  "bloodGroup": "A+",
                  "hospital": "City Care Hospital",
                  "city": "Bengaluru",
                  "urgency": "High"
                }
                """;

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("Riya Verma"));

        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
