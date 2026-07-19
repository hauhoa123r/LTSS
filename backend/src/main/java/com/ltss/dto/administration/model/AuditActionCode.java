package com.ltss.dto.administration.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public enum AuditActionCode {
    ACCOUNT_REGISTERED("Đăng ký tài khoản"),
    EMAIL_VERIFIED("Xác minh email"),
    LOGIN_FAILED("Đăng nhập thất bại"),
    LOGIN_SUCCEEDED("Đăng nhập thành công"),
    LOGIN_SUCCESS("Đăng nhập thành công"),
    SESSION_REFRESHED("Làm mới phiên đăng nhập"),
    LOGOUT("Đăng xuất"),
    PASSWORD_RESET("Đặt lại mật khẩu"),
    PASSWORD_CHANGED("Đổi mật khẩu"),
    PROFILE_UPDATED("Cập nhật hồ sơ"),
    PROFILE_UPDATE("Cập nhật hồ sơ"),
    ADMIN_USER_PASSWORD_RESET("Admin đặt lại mật khẩu người dùng"),
    ADMIN_USER_STATUS_CHANGED("Thay đổi trạng thái người dùng"),
    ADMIN_USER_ROLE_ASSIGNED("Gán vai trò người dùng"),
    ADMIN_USER_ROLE_REVOKED("Thu hồi vai trò người dùng"),
    ADMIN_USER_ACCOUNT_UPDATED("Cập nhật thông tin và vai trò người dùng"),
    TOUR_CREATED("Tạo lịch trình"),
    TOUR_UPDATED("Cập nhật lịch trình"),
    TOUR_COPIED("Sao chép lịch trình"),
    TOUR_COPY("Sao chép lịch trình"),
    TOUR_VISIBILITY_CHANGED("Thay đổi hiển thị lịch trình"),
    TOUR_DELETED("Xóa lịch trình"),
    REVIEW_REPLIED("Phản hồi đánh giá"),
    REVIEW_CREATE("Tạo đánh giá"),
    QUIZ_CREATED("Tạo bài quiz"),
    QUIZ_UPDATED("Cập nhật bài quiz"),
    QUIZ_DELETED("Xóa bài quiz"),
    QUIZ_SUBMIT("Gửi bài quiz"),
    QUIZ_ATTEMPT_STARTED("Bắt đầu lượt làm quiz"),
    QUIZ_ATTEMPT_SUBMITTED("Nộp lượt làm quiz"),
    QUIZ_ATTEMPT_AUTO_SUBMITTED("Tự động nộp lượt làm quiz"),
    MODERATION_SUBMITTED("Gửi nội dung kiểm duyệt"),
    MODERATION_CANCELLED("Hủy yêu cầu kiểm duyệt"),
    MODERATION_APPROVED("Phê duyệt nội dung"),
    MODERATION_REJECTED("Từ chối nội dung"),
    MODERATION_DECIDE("Ra quyết định kiểm duyệt"),
    CONTENT_SUBMIT("Gửi nội dung"),
    PLACE_VIEW("Xem địa điểm"),
    DEMO_SEED("Khởi tạo dữ liệu mẫu");

    private final String displayName;

    AuditActionCode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
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
        for (AuditActionCode action : values()) labels.put(action.name(), action.displayName);
        return Collections.unmodifiableMap(labels);
    }

    public static Set<String> hiddenCodes() {
        Set<String> codes = new java.util.LinkedHashSet<>();
        for (AuditActionCode action : hiddenActions()) codes.add(action.name());
        return Collections.unmodifiableSet(codes);
    }

    public static Map<String, String> visibleLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        for (AuditActionCode action : values()) {
            if (!hiddenActions().contains(action)) labels.put(action.name(), action.displayName);
        }
        return Collections.unmodifiableMap(labels);
    }

    private static Set<AuditActionCode> hiddenActions() {
        return EnumSet.of(
                LOGIN_SUCCESS,
                LOGIN_SUCCEEDED,
                LOGIN_FAILED,
                SESSION_REFRESHED,
                LOGOUT,
                PROFILE_UPDATE,
                PROFILE_UPDATED
        );
    }
}
