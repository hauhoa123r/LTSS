package com.ltss.dto.content;

import com.ltss.entity.content.PromotionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record BusinessOwnerPromotionResponse(
        Long id,
        String title,
        String description,
        String discountType,
        BigDecimal discountValue,
        String promoCode,
        Instant startAt,
        Instant endAt,
        PromotionStatus status,
        Integer version,
        Instant publishedAt,
        Instant updatedAt
) {}
