package com.ltss.dto.moderation;

import java.math.BigDecimal;
import java.util.List;

public record ModerationTargetContentResponse(
        String summary,
        String body,
        List<Field> fields,
        List<Question> questions
) {
    public record Field(String label, Object value) {}

    public record Question(
            String content,
            String explanation,
            BigDecimal points,
            List<Answer> answers
    ) {}

    public record Answer(String content, boolean correct) {}
}
