package com.ltss.features.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Entity
@Table(name = "review_replies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewReplyEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "review_id", nullable = false)
    private Long reviewId;
    @Column(name = "replied_by_user_id")
    private Long repliedByUserId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Version @Column(nullable = false)
    private Integer version;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ReviewReplyEntity(Long reviewId, Long userId, String content) {
        this.reviewId = reviewId;
        this.repliedByUserId = userId;
        this.content = content;
    }
}
