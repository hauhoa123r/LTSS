package com.ltss.features.auth.email;

public record AccountEmailEvent(String recipient, AccountEmailType type, String rawToken) {
}
