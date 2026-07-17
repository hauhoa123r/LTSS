package com.ltss.dto.administration.model;

public enum AuditFieldCode {
    status("Trạng thái"),
    role("Vai trò"),
    reason("Lý do"),
    visibility("Chế độ hiển thị"),
    migration("Bản cập nhật dữ liệu");

    private final String displayName;

    AuditFieldCode(String displayName) {
        this.displayName = displayName;
    }

    public static String labelOf(String code) {
        if (code == null || code.isBlank()) return "Thông tin";
        try {
            return valueOf(code).displayName;
        } catch (IllegalArgumentException ignored) {
            return code.replace('_', ' ');
        }
    }
}
