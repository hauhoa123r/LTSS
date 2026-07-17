package com.ltss.service.moderation.strategy;

import java.time.Instant;

public abstract class AbstractModerationTargetStrategy<T> implements ModerationTargetStrategy {
    private final Class<T> targetClass;

    protected AbstractModerationTargetStrategy(Class<T> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public final boolean isOwner(Object target, Long actorId) {
        return isOwnerTarget(cast(target), actorId);
    }

    @Override
    public final boolean isPending(Object target) {
        return isPendingTarget(cast(target));
    }

    @Override
    public final Integer version(Object target) {
        return versionTarget(cast(target));
    }

    @Override
    public final String title(Object target) {
        return titleTarget(cast(target));
    }

    @Override
    public final void approve(Object target, Instant now) {
        approveTarget(cast(target), now);
    }

    @Override
    public final void reject(Object target) {
        rejectTarget(cast(target));
    }

    @Override
    public final void cancel(Object target) {
        cancelTarget(cast(target));
    }

    protected final T cast(Object target) {
        return targetClass.cast(target);
    }

    protected abstract boolean isOwnerTarget(T target, Long actorId);

    protected abstract boolean isPendingTarget(T target);

    protected abstract Integer versionTarget(T target);

    protected abstract String titleTarget(T target);

    protected abstract void approveTarget(T target, Instant now);

    protected abstract void rejectTarget(T target);

    protected abstract void cancelTarget(T target);
}
