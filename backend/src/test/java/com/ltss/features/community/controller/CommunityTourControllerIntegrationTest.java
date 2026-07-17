package com.ltss.features.community.controller;

import com.ltss.common.response.PageResponse;
import com.ltss.features.community.dto.ReviewResponse;
import com.ltss.features.community.service.ReviewService;
import com.ltss.features.tour.dto.TourSummaryResponse;
import com.ltss.features.tour.service.TourService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockAuthRepositories
class CommunityTourControllerIntegrationTest {
    @MockitoBean private ReviewService reviewService;
    @MockitoBean private TourService tourService;
    @Autowired private MockMvc mockMvc;

    @Test
    void publishedToursAndVisibleReviewsArePublic() throws Exception {
        when(tourService.publicTours(any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.<TourSummaryResponse>of(), 0, 12, 0, 0, true, true));
        when(reviewService.visible(any(), anyLong(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.<ReviewResponse>of(), 0, 10, 0, 0, true, true));

        mockMvc.perform(get("/api/v1/tours")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
        mockMvc.perform(get("/api/v1/reviews").param("targetType", "PLACE").param("targetId", "4"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void creatingReviewAndTourRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/reviews/PLACE/4").contentType("application/json")
                        .content("{\"rating\":5,\"comment\":\"Một trải nghiệm thực sự rất đáng nhớ\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/account/tours").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reviewRejectsRatingOutsideRange() throws Exception {
        mockMvc.perform(post("/api/v1/reviews/PLACE/4")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(token -> token.subject("10")))
                        .contentType("application/json")
                        .content("{\"rating\":6,\"comment\":\"Một trải nghiệm thực sự rất đáng nhớ\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
