package com.ltss.service.moderation.strategy;

import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.exception.ConflictException;
import com.ltss.entity.moderation.ModerationTargetType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModerationTargetStrategyRegistryTest {
    @Test
    void resolvesStrategyByTargetType() {
        ModerationTargetStrategy article = mock(ModerationTargetStrategy.class);
        when(article.type()).thenReturn(ModerationTargetType.ARTICLE);
        ModerationTargetStrategyRegistry registry = new ModerationTargetStrategyRegistry(List.of(article));

        assertEquals(article, registry.require(ModerationTargetType.ARTICLE));
        assertThrows(ResourceNotFoundException.class, () -> registry.require(ModerationTargetType.QUIZ));
    }

    @Test
    void nonSubmittableTargetDoesNotViolateBaseStrategyContract() {
        ModerationTargetStrategy review = mock(ModerationTargetStrategy.class);
        when(review.type()).thenReturn(ModerationTargetType.REVIEW);
        ModerationTargetStrategyRegistry registry = new ModerationTargetStrategyRegistry(List.of(review));

        assertEquals(review, registry.require(ModerationTargetType.REVIEW));
        assertThrows(ConflictException.class,
                () -> registry.requireSubmittable(ModerationTargetType.REVIEW));
    }

    @Test
    void rejectsDuplicateStrategyRegistration() {
        ModerationTargetStrategy first = mock(ModerationTargetStrategy.class);
        ModerationTargetStrategy second = mock(ModerationTargetStrategy.class);
        when(first.type()).thenReturn(ModerationTargetType.REVIEW);
        when(second.type()).thenReturn(ModerationTargetType.REVIEW);

        assertThrows(IllegalStateException.class,
                () -> new ModerationTargetStrategyRegistry(List.of(first, second)));
    }
}
