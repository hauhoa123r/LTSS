package com.ltss.service.moderation.strategy;

import java.time.Instant;

public abstract class AbstractSubmittableModerationTargetStrategy<T>
        extends AbstractModerationTargetStrategy<T>
        implements SubmittableModerationTargetStrategy {

    protected AbstractSubmittableModerationTargetStrategy(Class<T> targetClass) {
        super(targetClass);
    }

    @Override
    public final void requireSubmittable(Object target) {
        requireSubmittableTarget(cast(target));
    }

    @Override
    public final void submit(Object target, Instant now) {
        submitTarget(cast(target), now);
    }

    protected abstract void requireSubmittableTarget(T target);

    protected abstract void submitTarget(T target, Instant now);
}
