package com.ltss.features.auth.controller;

import com.ltss.features.auth.dto.response.AuthResponse;
import com.ltss.features.auth.dto.response.ProfileResponse;
import com.ltss.features.auth.service.AuthService;
import com.ltss.features.auth.service.AuthenticatedSession;
import com.ltss.support.MockAuthRepositories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockAuthRepositories
class AuthControllerIntegrationTest {
    @MockitoBean
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerIsPublicAndValidatesInput() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"","displayName":"","email":"bad","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors").isArray());
    }

    @Test
    void loginReturnsAccessTokenAndHttpOnlyRefreshCookie() throws Exception {
        ProfileResponse profile = new ProfileResponse(
                7L, "Tourist", "Visitor", "visitor@example.com", null, null, null,
                List.of("TOURIST"), List.of(), 0
        );
        AuthResponse response = new AuthResponse(
                "signed-access-token", "Bearer", Instant.now().plusSeconds(900), profile
        );
        when(authService.login(any(), any()))
                .thenReturn(new AuthenticatedSession(response, "opaque-refresh-token"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"visitor@example.com","password":"River!Stone9"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("signed-access-token"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("TOURIST"))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")));
    }
}
