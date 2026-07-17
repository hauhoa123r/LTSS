package com.ltss.entity.community;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "prohibited_terms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProhibitedTermEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "normalized_term", nullable = false, length = 255)
    private String normalizedTerm;
    @Column(nullable = false, length = 20)
    private String severity;
    @Column(name = "is_active", nullable = false)
    private boolean active;
}
