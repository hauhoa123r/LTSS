package com.ltss.common.exception;

import com.ltss.support.MockAuthRepositories;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        com.ltss.LtssApplication.class,
        ValidationEnvelopeIntegrationTest.ValidationProbeConfiguration.class
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockAuthRepositories
class ValidationEnvelopeIntegrationTest {

    private final MockMvc mockMvc;

    @Autowired
    ValidationEnvelopeIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @WithMockUser
    void invalidRequestUsesTheValidationErrorEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/test/validation-probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("value"))
                .andExpect(jsonPath("$.error.fieldErrors[0].message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.requestId").exists());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ValidationProbeConfiguration {

        @Bean
        ValidationProbeController validationProbeController() {
            return new ValidationProbeController();
        }
    }

    @RestController
    @RequestMapping("/api/v1/test/validation-probe")
    static class ValidationProbeController {

        @PostMapping
        void validate(@Valid @RequestBody ValidationProbeRequest request) {
        }
    }

    record ValidationProbeRequest(@NotBlank String value) {
    }
}
