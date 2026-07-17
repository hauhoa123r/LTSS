package com.ltss.dto.administration.model;

public enum AuditValueCode {
    PENDING_VERIFICATION("Chờ xác minh"),
    ACTIVE("Đang hoạt động"),
    DEACTIVATED("Đã vô hiệu hóa"),
    SUSPENDED("Tạm ngưng"),
    DELETED("Đã xóa"),
    DRAFT("Bản nháp"),
    PENDING("Đang chờ"),
    PUBLISHED("Đã xuất bản"),
    REJECTED("Đã từ chối"),
    ARCHIVED("Đã lưu trữ"),
    RESOLVED("Đã xử lý"),
    CANCELLED("Đã hủy"),
    APPROVED("Đã phê duyệt"),
    PUBLIC("Công khai"),
    PRIVATE("Riêng tư"),
    UNLISTED("Không công khai"),
    TOURIST("Khách du lịch"),
    BUSINESS_OWNER("Chủ doanh nghiệp"),
    RELIC_MANAGER("Quản lý di tích"),
    MODERATOR("Kiểm duyệt viên"),
    ADMINISTRATOR("Quản trị viên");

    private final String displayName;

    AuditValueCode(String displayName) {
        this.displayName = displayName;
    }

    public static Object labelOf(Object value) {
        if (!(value instanceof String text)) return value;
        try {
            return valueOf(text).displayName;
        } catch (IllegalArgumentException ignored) {
            return text;
        }
    }
}
