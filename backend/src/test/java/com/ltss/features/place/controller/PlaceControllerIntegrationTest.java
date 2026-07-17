package com.ltss.features.place.controller;

import com.ltss.common.response.PageResponse;
import com.ltss.features.place.dto.PlaceSummaryResponse;
import com.ltss.features.place.service.DiscoveryService;
import com.ltss.features.place.service.FavoriteService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockAuthRepositories
class PlaceControllerIntegrationTest {
    @MockitoBean
    private DiscoveryService discoveryService;

    @MockitoBean
    private FavoriteService favoriteService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishedPlaceSearchIsPublic() throws Exception {
        when(discoveryService.search(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.<PlaceSummaryResponse>of(), 0, 12, 0, 0, true, true));

        mockMvc.perform(get("/api/v1/places").param("q", "Đền Và"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    void nearbyRejectsRadiusAboveFiveKilometers() throws Exception {
        mockMvc.perform(get("/api/v1/places/nearby")
                        .param("latitude", "21.14")
                        .param("longitude", "105.50")
                        .param("radiusKm", "5.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void favoriteMutationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/places/12/favorite"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }
}
