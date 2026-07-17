package com.ltss.features.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "badges")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BadgeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "badge_code", nullable = false, length = 60)
    private String badgeCode;
    @Column(name = "badge_name", nullable = false, length = 150)
    private String badgeName;
    @Column(length = 700)
    private String description;
    @Column(name = "icon_url", length = 1000)
    private String iconUrl;
    @Column(name = "is_active", nullable = false)
    private boolean active;
}
