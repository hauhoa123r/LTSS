package com.ltss.dto.content;

import com.ltss.entity.content.BusinessStatus;

public record BusinessOwnerProfileResponse(
        Long id,
        String registrationNumber,
        String contactEmail,
        String websiteUrl,
        BusinessStatus status,
        LinkedPlaceResponse place,
        String placeStatus,
        Integer version
) {}
