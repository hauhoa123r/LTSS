package com.ltss.features.system;

import com.ltss.common.logging.RequestIdFilter;
import com.ltss.support.MockAuthRepositories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockAuthRepositories
class FoundationWebIntegrationTest {

    private final MockMvc mockMvc;

    @Autowired
    FoundationWebIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void healthIsPublicAndUsesTheSuccessEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestIdFilter.HEADER_NAME))
                .andExpect(header().string(
                        RequestIdFilter.HEADER_NAME,
                        matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                ))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.application").value("ltss-backend"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void validClientRequestIdIsEchoed() throws Exception {
        String requestId = "client-request_123";

        mockMvc.perform(get("/api/v1/health")
                        .header(RequestIdFilter.HEADER_NAME, requestId))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, requestId))
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    @Test
    void unauthenticatedRequestUsesTheSafeErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/foundation/restricted"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(RequestIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void corsPreflightIsPublicForConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/foundation/restricted")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().exists(RequestIdFilter.HEADER_NAME));
    }
}
