package com.ltss.entity.quiz;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Entity
@Table(name = "answers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "question_id", nullable = false)
    private Long questionId;
    @Column(nullable = false, length = 100)
    private String content;
    @Column(name = "is_correct", nullable = false)
    private boolean correct;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    @Column(name = "is_active", nullable = false)
    private boolean active;
    @Column(name = "deleted_at")
    private Instant deletedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AnswerEntity(Long questionId, String content, boolean correct, Integer displayOrder) {
        this.questionId = questionId;
        this.content = content;
        this.correct = correct;
        this.displayOrder = displayOrder;
        this.active = true;
    }
}
