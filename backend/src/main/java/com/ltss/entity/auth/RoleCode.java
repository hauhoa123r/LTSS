package com.ltss.entity.auth;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum RoleCode {
    TOURIST("Khách du lịch"),
    BUSINESS_OWNER("Chủ doanh nghiệp"),
    RELIC_MANAGER("Quản lý di tích"),
    MODERATOR("Kiểm duyệt viên"),
    ADMINISTRATOR("Quản trị viên");

    private final String displayName;

    RoleCode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Map<String, String> labels() {
        Map<String, String> labels = new LinkedHashMap<>();
        for (RoleCode role : values()) {
            labels.put(role.name(), role.displayName);
        }
        return Collections.unmodifiableMap(labels);
    }
}
