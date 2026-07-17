package com.ltss.features.content.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "businesses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "place_id", nullable = false)
    private Long placeId;
    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;
    @Column(name = "registration_number", length = 100)
    private String registrationNumber;
    @Column(name = "contact_email", length = 255)
    private String contactEmail;
    @Column(name = "website_url", length = 1000)
    private String websiteUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private BusinessStatus status;
    @Version @Column(nullable = false)
    private Integer version;
}
