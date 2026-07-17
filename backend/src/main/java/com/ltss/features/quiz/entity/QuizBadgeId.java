package com.ltss.features.quiz.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class QuizBadgeId implements Serializable {
    private Long quizId;
    private Long badgeId;
}
