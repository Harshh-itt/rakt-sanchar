package com.example.blood;

import com.example.blood.exception.ApiException;
import com.example.blood.exception.ErrorCode;
import com.example.blood.model.Role;
import com.example.blood.security.AccessPolicy;
import com.example.blood.security.CurrentUser;
import com.example.blood.security.DisabledCurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class Phase2RoleAndIdentityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessPolicy accessPolicy;

    @Test
    void missingDemoIdentityReturns401IdentityRequired() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("IDENTITY_REQUIRED"));
    }

    @Test
    void donorCallingAdminEndpointReturns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("X-Demo-User-Id", "donor-1")
                        .header("X-Demo-Role", "DONOR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/users")
                        .header("X-Demo-User-Id", "admin-1")
                        .header("X-Demo-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void onboardingAsAdminReturns403WhileDonorAndLanguageUpdateSucceed() throws Exception {
        // Attempting to self-onboard as ADMIN must fail with 403 FORBIDDEN
        mockMvc.perform(post("/api/users/me/onboarding")
                        .header("X-Demo-User-Id", "new-user-1")
                        .header("X-Demo-Role", "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN",
                                  "name": "Malicious Escalation"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // Onboarding as DONOR succeeds and sets onboarded = true
        mockMvc.perform(post("/api/users/me/onboarding")
                        .header("X-Demo-User-Id", "new-user-1")
                        .header("X-Demo-Role", "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "DONOR",
                                  "name": "Kavita Nair",
                                  "preferredLanguage": "en",
                                  "bloodGroup": "A_POS",
                                  "city": "Pune",
                                  "phone": "9876501234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("new-user-1"))
                .andExpect(jsonPath("$.role").value("DONOR"))
                .andExpect(jsonPath("$.onboarded").value(true))
                .andExpect(jsonPath("$.name").value("Kavita Nair"));

        // Updating preferredLanguage to Hindi succeeds
        mockMvc.perform(patch("/api/users/me/language")
                        .header("X-Demo-User-Id", "new-user-1")
                        .header("X-Demo-Role", "DONOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "preferredLanguage": "hi"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredLanguage").value("hi"));
    }

    @Test
    void accessPolicyEvaluatesRoleAndOwnershipRules() {
        CurrentUser donor = new CurrentUser("donor-1", Role.DONOR, "Priya", "en", true);
        CurrentUser hospital = new CurrentUser("hospital-1", Role.HOSPITAL, "City Care", "en", true);
        CurrentUser admin = new CurrentUser("admin-1", Role.ADMIN, "Admin", "en", true);

        assertThat(accessPolicy.requireRole(donor, Role.DONOR)).isEqualTo(donor);
        assertThatThrownBy(() -> accessPolicy.requireRole(donor, Role.ADMIN))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        assertThatThrownBy(() -> accessPolicy.requireAuthenticated(null))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.IDENTITY_REQUIRED);

        accessPolicy.requireOwner(donor, "donor-1");
        assertThatThrownBy(() -> accessPolicy.requireOwner(donor, "donor-2"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        accessPolicy.requireOwnerOrAdmin(admin, "hospital-1");
        assertThatThrownBy(() -> accessPolicy.requireOwnerOrAdmin(hospital, "donor-1"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void stubProviderIsDisabledOutsideDemoProfile() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(
                        com.example.blood.security.DevStubCurrentUserProvider.class,
                        com.example.blood.security.DisabledCurrentUserProvider.class,
                        com.example.blood.repository.InMemoryUserRepository.class
                )
                .withPropertyValues("spring.profiles.active=production");

        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(com.example.blood.security.DevStubCurrentUserProvider.class);
            assertThat(ctx).hasSingleBean(DisabledCurrentUserProvider.class);
            DisabledCurrentUserProvider provider = ctx.getBean(DisabledCurrentUserProvider.class);
            assertThatThrownBy(provider::requireCurrentUser)
                    .isInstanceOf(ApiException.class)
                    .extracting(ex -> ((ApiException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.IDENTITY_REQUIRED);
        });
    }
}
