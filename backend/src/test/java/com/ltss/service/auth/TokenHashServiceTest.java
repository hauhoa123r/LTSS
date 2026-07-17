package com.ltss.service.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHashServiceTest {
    private final TokenHashService tokenHashService = new TokenHashService();

    @Test
    void opaqueTokensAreRandomAndStoredAsStableSha256() {
        String first = tokenHashService.generateOpaqueToken();
        String second = tokenHashService.generateOpaqueToken();

        assertThat(first).isNotEqualTo(second);
        assertThat(tokenHashService.hash(first))
                .hasSize(64)
                .isEqualTo(tokenHashService.hash(first))
                .isNotEqualTo(first);
    }

    @Test
    void otpAlwaysContainsSixDigits() {
        assertThat(tokenHashService.generateOtp()).matches("[0-9]{6}");
    }
}
