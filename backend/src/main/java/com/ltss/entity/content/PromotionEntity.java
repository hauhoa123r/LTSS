package com.ltss.entity.content;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "promotions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "business_id", nullable = false)
    private Long businessId;
    @Column(name = "created_by_user_id")
    private Long createdByUserId;
    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;
    @Column(nullable = false, length = 250)
    private String title;
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String description;
    @Column(name = "discount_type", length = 30)
    private String discountType;
    @Column(name = "discount_value", precision = 12, scale = 2)
    private BigDecimal discountValue;
    @Column(name = "promo_code", length = 50)
    private String promoCode;
    @Column(name = "start_at", nullable = false)
    private Instant startAt;
    @Column(name = "end_at", nullable = false)
    private Instant endAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PromotionStatus status;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "deleted_at")
    private Instant deletedAt;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
    @Version @Column(nullable = false)
    private Integer version;

    public void submit(Instant now) { status = PromotionStatus.PENDING; submittedAt = now; }
    public void approve() { status = PromotionStatus.ACTIVE; }
    public void reject() { status = PromotionStatus.REJECTED; }
    public void cancelSubmission() { status = PromotionStatus.DRAFT; }
}
