package com.ltss.service.moderation.strategy;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.entity.moderation.ModerationTargetType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ModerationTargetStrategyRegistry {
    private final Map<ModerationTargetType, ModerationTargetStrategy> strategies;

    public ModerationTargetStrategyRegistry(List<ModerationTargetStrategy> strategies) {
        Map<ModerationTargetType, ModerationTargetStrategy> indexed = new EnumMap<>(ModerationTargetType.class);
        for (ModerationTargetStrategy strategy : strategies) {
            if (indexed.put(strategy.type(), strategy) != null) {
                throw new IllegalStateException("Duplicate moderation strategy for " + strategy.type());
            }
        }
        this.strategies = Map.copyOf(indexed);
    }

    public ModerationTargetStrategy require(ModerationTargetType type) {
        ModerationTargetStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new ResourceNotFoundException("Moderation target type is not supported");
        }
        return strategy;
    }

    public SubmittableModerationTargetStrategy requireSubmittable(ModerationTargetType type) {
        ModerationTargetStrategy strategy = require(type);
        if (strategy instanceof SubmittableModerationTargetStrategy submittable) {
            return submittable;
        }
        throw new ConflictException("This moderation target cannot be submitted manually");
    }
}
