package com.ltss.features.auth.security;

import com.ltss.common.config.JwtProperties;
import com.ltss.features.auth.entity.UserEntity;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {
    private static final String SECRET = "test-only-jwt-secret-that-is-at-least-thirty-two-bytes";

    @Test
    void issuesHs256AccessTokenWithExpectedIdentityClaims() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
        Instant now = Instant.now();
        JwtService service = new JwtService(
                encoder,
                new JwtProperties("ltss-test", SECRET, Duration.ofMinutes(15)),
                Clock.fixed(now, ZoneOffset.UTC)
        );
        UserEntity user = mock(UserEntity.class);
        when(user.getId()).thenReturn(42L);
        when(user.getEmail()).thenReturn("visitor@example.com");

        IssuedAccessToken issued = service.issue(user, List.of("TOURIST"), List.of("profile:read"));
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        Jwt jwt = decoder.decode(issued.value());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("visitor@example.com");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("TOURIST");
        assertThat(issued.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
    }
}
