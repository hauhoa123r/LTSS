package com.ltss.features.content.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PromotionResponse(
        Long id,
        String title,
        String description,
        String discountType,
        BigDecimal discountValue,
        String promoCode,
        Instant startAt,
        Instant endAt,
        Long businessId,
        String businessName,
        String coverUrl,
        List<ContentMediaResponse> media,
        Integer version
) {
}
