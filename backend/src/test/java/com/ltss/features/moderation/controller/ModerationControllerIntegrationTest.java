package com.ltss.features.moderation.controller;

import com.ltss.common.response.PageResponse;
import com.ltss.features.moderation.dto.ModerationRecordResponse;
import com.ltss.features.moderation.service.ModerationService;
import com.ltss.support.MockAuthRepositories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockAuthRepositories
class ModerationControllerIntegrationTest {
    @MockitoBean
    private ModerationService moderationService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void queueRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/moderation/queue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void authenticatedQueueUsesStandardPageEnvelope() throws Exception {
        when(moderationService.queue(any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.<ModerationRecordResponse>of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/api/v1/moderation/queue").with(jwt().jwt(token -> token.subject("10"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void submitRequiresVersion() throws Exception {
        mockMvc.perform(post("/api/v1/moderation/targets/ARTICLE/12/submit")
                        .with(jwt().jwt(token -> token.subject("10")))
                        .contentType("application/json")
                        .content("{\"note\":\"ready\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownTargetTypeIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/moderation/targets/TOUR/12/history")
                        .with(jwt().jwt(token -> token.subject("10"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));
    }
}
