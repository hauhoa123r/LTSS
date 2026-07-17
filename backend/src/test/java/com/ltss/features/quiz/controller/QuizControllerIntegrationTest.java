package com.ltss.features.quiz.controller;

import com.ltss.common.response.PageResponse;
import com.ltss.features.quiz.dto.QuizSummaryResponse;
import com.ltss.features.quiz.service.QuizAttemptService;
import com.ltss.features.quiz.service.QuizService;
import com.ltss.support.MockAuthRepositories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockAuthRepositories
class QuizControllerIntegrationTest {
    @MockitoBean QuizService quizService;
    @MockitoBean QuizAttemptService attemptService;
    @Autowired MockMvc mockMvc;

    @Test
    void publishedQuizCatalogIsPublic() throws Exception {
        when(quizService.published(any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.<QuizSummaryResponse>of(), 0, 12, 0, 0, true, true));
        mockMvc.perform(get("/api/v1/quizzes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void startingAttemptAndManagementRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/quizzes/7/attempts").contentType("application/json")
                        .content("{\"latitude\":21.1,\"longitude\":105.5}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/management/quizzes").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void coordinatesOutsideValidRangeUseValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/quizzes/7/attempts").with(jwt().jwt(token -> token.subject("10")))
                        .contentType("application/json").content("{\"latitude\":91,\"longitude\":105.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
