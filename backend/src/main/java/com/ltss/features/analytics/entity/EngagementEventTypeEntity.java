package com.ltss.features.analytics.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "engagement_event_types")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngagementEventTypeEntity {
    @Id @Column(name = "event_type_code", length = 40)
    private String eventTypeCode;
    @Column(name = "event_type_name", nullable = false, length = 120)
    private String eventTypeName;
    @Column(length = 500)
    private String description;
    @Column(name = "is_active", nullable = false)
    private boolean active;
}
