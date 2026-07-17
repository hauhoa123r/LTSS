package com.ltss.features.auth.security;

import com.ltss.features.auth.exception.AccountException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {
    public Long optionalUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public Long requireUserId() {
        Long userId = optionalUserId();
        if (userId == null) {
            throw AccountException.forbiddenState();
        }
        return userId;
    }
}
