package com.ltss.dto.content;

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
