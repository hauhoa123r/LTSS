package com.ltss.features.content.dto;

public record BusinessResponse(
        Long id,
        String registrationNumber,
        String contactEmail,
        String websiteUrl,
        LinkedPlaceResponse place,
        String coverUrl,
        Integer version
) {
}
