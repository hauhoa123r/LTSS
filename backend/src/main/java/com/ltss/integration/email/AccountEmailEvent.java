package com.ltss.integration.email;

public record AccountEmailEvent(String recipient, AccountEmailType type, String rawToken) {
}
