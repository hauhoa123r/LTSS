package com.ltss.features.content.controller;

import com.ltss.common.response.PageResponse;
import com.ltss.features.content.dto.ArticleCategoryResponse;
import com.ltss.features.content.dto.ArticleSummaryResponse;
import com.ltss.features.content.dto.BusinessResponse;
import com.ltss.features.content.dto.EventSummaryResponse;
import com.ltss.features.content.service.BusinessPublicService;
import com.ltss.features.content.service.EditorialPublicService;
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
class PublicContentControllerIntegrationTest {
    @MockitoBean
    private BusinessPublicService businessService;

    @MockitoBean
    private EditorialPublicService editorialService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicBusinessListDoesNotRequireAuthentication() throws Exception {
        when(businessService.businesses(any(), anyInt(), anyInt()))
                .thenReturn(emptyPage());

        mockMvc.perform(get("/api/v1/businesses").param("q", "ẩm thực"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void articleCategoriesArePublic() throws Exception {
        when(editorialService.categories()).thenReturn(List.of(
                new ArticleCategoryResponse(1L, "Cẩm nang", "cam-nang", "Kinh nghiệm địa phương")
        ));

        mockMvc.perform(get("/api/v1/article-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("cam-nang"));
    }

    @Test
    void articlesAndEventsArePublic() throws Exception {
        when(editorialService.articles(any(), any(), anyInt(), anyInt())).thenReturn(emptyArticlePage());
        when(editorialService.events(any(), anyInt(), anyInt())).thenReturn(emptyEventPage());

        mockMvc.perform(get("/api/v1/articles").param("category", "cam-nang"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk());
    }

    @Test
    void invalidPageSizeUsesStandardValidationEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/promotions").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void contentMutationStillRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/articles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private PageResponse<BusinessResponse> emptyPage() {
        return new PageResponse<>(List.of(), 0, 12, 0, 0, true, true);
    }

    private PageResponse<ArticleSummaryResponse> emptyArticlePage() {
        return new PageResponse<>(List.of(), 0, 12, 0, 0, true, true);
    }

    private PageResponse<EventSummaryResponse> emptyEventPage() {
        return new PageResponse<>(List.of(), 0, 12, 0, 0, true, true);
    }
}
