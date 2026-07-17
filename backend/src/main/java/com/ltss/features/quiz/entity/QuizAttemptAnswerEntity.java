package com.ltss.features.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "quiz_attempt_answers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAttemptAnswerEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;
    @Column(name = "question_id")
    private Long questionId;
    @Column(name = "selected_answer_id")
    private Long selectedAnswerId;
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;
    @Column(name = "question_text_snapshot", nullable = false, length = 250)
    private String questionTextSnapshot;
    @Column(name = "selected_answer_text_snapshot", length = 100)
    private String selectedAnswerTextSnapshot;
    @Column(name = "correct_answer_text_snapshot", nullable = false, length = 100)
    private String correctAnswerTextSnapshot;
    @Column(name = "explanation_snapshot", columnDefinition = "TEXT")
    private String explanationSnapshot;
    @Column(name = "is_correct", nullable = false)
    private boolean correct;
    @Column(name = "awarded_points", nullable = false, precision = 6, scale = 2)
    private BigDecimal awardedPoints;
    @Column(name = "answered_at")
    private Instant answeredAt;

    public QuizAttemptAnswerEntity(Long attemptId, Long questionId, Integer questionOrder,
                                   String questionText, String correctAnswerText, String explanation) {
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.questionOrder = questionOrder;
        this.questionTextSnapshot = questionText;
        this.correctAnswerTextSnapshot = correctAnswerText;
        this.explanationSnapshot = explanation;
        this.correct = false;
        this.awardedPoints = BigDecimal.ZERO.setScale(2);
    }

    public void answer(Long answerId, String answerText, boolean correct,
                       BigDecimal awardedPoints, Instant now) {
        this.selectedAnswerId = answerId;
        this.selectedAnswerTextSnapshot = answerText;
        this.correct = correct;
        this.awardedPoints = awardedPoints;
        this.answeredAt = now;
    }
}
