package com.ltss.features.content.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "articles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    @Column(name = "place_id")
    private Long placeId;
    @Column(name = "event_id")
    private Long eventId;
    @Column(name = "author_user_id")
    private Long authorUserId;
    @Column(nullable = false, length = 250)
    private String title;
    @Column(nullable = false, length = 280)
    private String slug;
    @Column(length = 700)
    private String summary;
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PublicationStatus status;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Version @Column(nullable = false)
    private Integer version;

    public void submit(Instant now) { status = PublicationStatus.PENDING; submittedAt = now; }
    public void approve(Instant now) { status = PublicationStatus.PUBLISHED; publishedAt = now; }
    public void reject() { status = PublicationStatus.REJECTED; }
    public void cancelSubmission() { status = PublicationStatus.DRAFT; }
}
