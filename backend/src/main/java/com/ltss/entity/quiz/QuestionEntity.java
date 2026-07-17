package com.ltss.entity.quiz;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "quiz_id", nullable = false)
    private Long quizId;
    @Column(nullable = false, length = 250)
    private String content;
    @Column(columnDefinition = "TEXT")
    private String explanation;
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal points;
    @Column(name = "is_active", nullable = false)
    private boolean active;
    @Column(name = "deleted_at")
    private Instant deletedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public QuestionEntity(Long quizId, String content, String explanation,
                          Integer displayOrder, BigDecimal points) {
        this.quizId = quizId;
        this.content = content;
        this.explanation = explanation;
        this.displayOrder = displayOrder;
        this.points = points;
        this.active = true;
    }
}
