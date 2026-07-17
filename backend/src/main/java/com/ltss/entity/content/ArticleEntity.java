package com.ltss.entity.content;

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
    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;
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
    @Column(name = "deleted_at")
    private Instant deletedAt;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
    @Version @Column(nullable = false)
    private Integer version;

    public ArticleEntity(
            Long categoryId, Long placeId, Long authorUserId, String title,
            String slug, String summary, String content
    ) {
        this.categoryId = categoryId;
        this.placeId = placeId;
        this.authorUserId = authorUserId;
        this.updatedByUserId = authorUserId;
        this.title = title;
        this.slug = slug;
        this.summary = summary;
        this.content = content;
        this.status = PublicationStatus.DRAFT;
    }

    public void update(
            Long actorId, Long categoryId, Long placeId, String title, String summary, String content
    ) {
        this.updatedByUserId = actorId;
        this.categoryId = categoryId;
        this.placeId = placeId;
        this.title = title;
        this.summary = summary;
        this.content = content;
    }

    public void submit(Instant now) { status = PublicationStatus.PENDING; submittedAt = now; }
    public void approve(Instant now) { status = PublicationStatus.PUBLISHED; publishedAt = now; }
    public void reject() { status = PublicationStatus.REJECTED; }
    public void cancelSubmission() { status = PublicationStatus.DRAFT; }
    public void delete(Instant now) { status = PublicationStatus.DELETED; deletedAt = now; }
}
