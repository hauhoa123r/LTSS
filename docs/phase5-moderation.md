# Phase 5 — Moderation

## Implemented scope

- Submission for owned `ARTICLE`, `EVENT`, `BUSINESS_POST`, `PROMOTION`, and `QUIZ` targets, plus atomic registration of newly created `REVIEW` targets.
- Submission only from `DRAFT` or `REJECTED`; the target moves to `PENDING` and a new moderation record is appended.
- One pending case per target, guarded by a pessimistic target lock and application check.
- Pending queue filtered by target type with a page-size limit of 50.
- Moderator/Administrator approval and rejection. Rejection requires a non-blank reason.
- Approval maps articles/posts to `PUBLISHED`, events to `PUBLISHED`, and promotions to `ACTIVE`.
- Review approval maps `PENDING` to `VISIBLE`; rejection maps it to `REJECTED`.
- Quiz approval revalidates the complete question/answer aggregate and maps `PENDING` to `PUBLISHED`.
- Submitter cancellation maps the pending target back to `DRAFT` and makes the case terminal `CANCELLED`.
- Target history and case detail with submitter/moderator access boundaries.
- Notification creation for moderators on submission and submitters on decision.
- Current-user notification list, unread count, and ownership-scoped mark-read operation.
- React moderation queue and notification pages.

## Transaction and concurrency boundaries

Submission and decision methods are transactional. Target state, moderation record, notification, and audit are written inside the same transaction. Any downstream persistence exception propagates so Spring rolls the entire operation back.

Target rows and moderation cases use pessimistic write locks during commands. Every command also requires the current target `version`; a stale request returns `409 CONFLICT`. Terminal moderation records are immutable and cannot be resolved or cancelled twice.

The corrected Flyway baseline already enforces coherent `PENDING`, `RESOLVED`, and `CANCELLED` fields. Reject reason is validated in both the service and corrected database constraint.

## Authorization and ownership

The official roles seeded by the architecture are used because the atomic permission catalog remains intentionally empty:

- Article submission: `author_user_id` must equal the current user.
- Event submission: `created_by_user_id` must equal the current user.
- Business post/promotion submission: the current user must own the linked business.
- Review cases: the review author is the submitter; creation and pending-case registration share one transaction.
- Quiz submission: `created_by_user_id` must equal the current user; only the official `RELIC_MANAGER` role can author quizzes.
- Queue and decisions: effective `MODERATOR` role; `ADMINISTRATOR` inherits `MODERATOR`.
- Cancellation: only the original submitter.
- History/detail: submitter/target owner or moderator.

Backend enforcement remains authoritative; hiding frontend navigation is only a UX aid.

## Deferred extensions

- Tour, business-profile, and place moderation until their ownership/authoring aggregates and previously documented scope decisions are approved.
- Review edit, re-review, hide, and remove commands until their actor/state policy is approved.
- Content authoring forms, media upload, diff/snapshot presentation, and revision comparison.
- Atomic permission codes replacing the temporary role-level moderation gate after the official permission catalog is approved.
- Notification delivery channels beyond the in-application inbox.
