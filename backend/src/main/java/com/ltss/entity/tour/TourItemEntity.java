package com.ltss.entity.tour;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Entity
@Table(name = "tour_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TourItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tour_id", nullable = false)
    private Long tourId;
    @Column(name = "place_id", nullable = false)
    private Long placeId;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "visit_order", nullable = false)
    private Short visitOrder;
    @Column(name = "planned_start_at")
    private Instant plannedStartAt;
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    @Column(name = "transport_method", length = 50)
    private String transportMethod;
    @Column(length = 1000)
    private String note;

    public TourItemEntity(Long tourId, Long placeId, int visitOrder, Instant plannedStartAt,
                          Integer durationMinutes, String transportMethod, String note) {
        this.tourId = tourId;
        this.placeId = placeId;
        this.visitOrder = (short) visitOrder;
        this.plannedStartAt = plannedStartAt;
        this.durationMinutes = durationMinutes;
        this.transportMethod = transportMethod;
        this.note = note;
    }
}
