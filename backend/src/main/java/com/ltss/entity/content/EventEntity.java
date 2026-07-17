package com.ltss.entity.content;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "place_id")
    private Long placeId;
    @Column(name = "created_by_user_id")
    private Long createdByUserId;
    @Column(nullable = false, length = 250)
    private String title;
    @Column(nullable = false, length = 280)
    private String slug;
    @Column(columnDefinition = "LONGTEXT")
    private String description;
    @Column(name = "start_at", nullable = false)
    private Instant startAt;
    @Column(name = "end_at", nullable = false)
    private Instant endAt;
    @Column(name = "location_note", length = 500)
    private String locationNote;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private EventStatus status;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Version @Column(nullable = false)
    private Integer version;

    public void submit(Instant now) { status = EventStatus.PENDING; submittedAt = now; }
    public void approve(Instant now) { status = EventStatus.PUBLISHED; publishedAt = now; }
    public void reject() { status = EventStatus.REJECTED; }
    public void cancelSubmission() { status = EventStatus.DRAFT; }
}
