package com.ltss.features.analytics.controller;

import com.ltss.features.administration.service.*;
import com.ltss.features.analytics.dto.EngagementAcceptedResponse;
import com.ltss.features.analytics.service.*;
import com.ltss.support.MockAuthRepositories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockAuthRepositories
class Phase8ControllerIntegrationTest {
    @MockitoBean EngagementService engagementService;
    @MockitoBean AnalyticsService analyticsService;
    @MockitoBean AdministrationService administrationService;
    @MockitoBean AuditQueryService auditQueryService;
    @Autowired MockMvc mockMvc;

    @Test
    void engagementIngestionIsPublicAndReturnsAccepted() throws Exception {
        when(engagementService.record(any())).thenReturn(new EngagementAcceptedResponse(true));
        mockMvc.perform(post("/api/v1/engagement-events").contentType("application/json")
                        .content("{\"eventTypeCode\":\"VIEW\",\"sessionKey\":\"session-1\",\"targetType\":\"PLACE\",\"targetId\":7}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.recorded").value(true));
    }

    @Test
    void administrationEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/analytics/system").param("from", "2026-07-01").param("to", "2026-07-16"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedEngagementTypeUsesValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/engagement-events").contentType("application/json")
                        .content("{\"eventTypeCode\":\"view\",\"sessionKey\":\"session-1\",\"targetType\":\"PLACE\",\"targetId\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
