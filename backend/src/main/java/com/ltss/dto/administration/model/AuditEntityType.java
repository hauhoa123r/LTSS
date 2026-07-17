package com.ltss.dto.administration.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

public enum AuditEntityType {
    USER("Người dùng"),
    PLACE("Địa điểm"),
    BUSINESS("Doanh nghiệp"),
    ARTICLE("Bài viết"),
    EVENT("Sự kiện"),
    BUSINESS_POST("Bài đăng doanh nghiệp"),
    PROMOTION("Khuyến mãi"),
    TOUR("Lịch trình"),
    REVIEW("Đánh giá"),
    QUIZ("Bài quiz"),
    QUIZ_ATTEMPT("Lượt làm quiz"),
    DATABASE("Cơ sở dữ liệu");

    private final String displayName;

    AuditEntityType(String displayName) {
        this.displayName = displayName;
    }

    public static String labelOf(String code) {
        if (code == null || code.isBlank()) return "Không xác định";
        try {
            return valueOf(code).displayName;
        } catch (IllegalArgumentException ignored) {
            return code.replace('_', ' ');
        }
    }

    public static Map<String, String> labels() {
        Map<String, String> labels = new LinkedHashMap<>();
        for (AuditEntityType type : values()) labels.put(type.name(), type.displayName);
        return Collections.unmodifiableMap(labels);
    }

    public static Map<String, String> internalLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        for (AuditEntityType type : EnumSet.of(
                USER, ARTICLE, EVENT, BUSINESS_POST, PROMOTION, REVIEW, QUIZ, QUIZ_ATTEMPT, TOUR, DATABASE)) {
            labels.put(type.name(), type.displayName);
        }
        return Collections.unmodifiableMap(labels);
    }
}
