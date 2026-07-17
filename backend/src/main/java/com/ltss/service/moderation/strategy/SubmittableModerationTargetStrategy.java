package com.ltss.service.moderation.strategy;

import java.time.Instant;

public interface SubmittableModerationTargetStrategy extends ModerationTargetStrategy {
    void requireSubmittable(Object target);

    void submit(Object target, Instant now);
}
