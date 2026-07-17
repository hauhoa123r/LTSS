package com.ltss.features.auth.dto.response;

import com.ltss.features.auth.entity.UserEntity;

import java.util.List;

public record ProfileResponse(
        Long id,
        String fullName,
        String displayName,
        String email,
        String phone,
        String avatarUrl,
        String address,
        List<String> roles,
        List<String> permissions,
        Integer version
) {
    public static ProfileResponse from(UserEntity user, List<String> roles, List<String> permissions) {
        return new ProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getAddress(),
                List.copyOf(roles),
                List.copyOf(permissions),
                user.getVersion()
        );
    }
}
