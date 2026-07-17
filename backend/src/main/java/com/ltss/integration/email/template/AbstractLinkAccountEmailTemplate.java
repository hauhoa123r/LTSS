package com.ltss.integration.email.template;

import com.ltss.config.auth.AccountProperties;
import org.springframework.web.util.UriComponentsBuilder;

public abstract class AbstractLinkAccountEmailTemplate implements AccountEmailTemplate {
    private final AccountProperties properties;

    protected AbstractLinkAccountEmailTemplate(AccountProperties properties) {
        this.properties = properties;
    }

    protected final String link(String path, String token) {
        return UriComponentsBuilder.fromUriString(properties.email().frontendBaseUrl())
                .path(path)
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
    }
}
