# Phase 7 — Quiz & Gamification

## Implemented scope

### Quiz authoring and moderation

- Only a user with the official `RELIC_MANAGER` role can create, edit, list, and delete their own quiz drafts.
- A quiz must belong to a `PUBLISHED` place and cannot be moved to another place after creation.
- Time limit is 1–600 seconds and passing score is 0–100 percent.
- The aggregate contains 1–100 questions. Question text is at most 250 characters and points must be positive.
- Every question has 2–4 active answers, answer text is at most 100 characters, and exactly one answer is correct.
- Only `DRAFT` and `REJECTED` quizzes are editable. Published content is immutable in this phase so an active attempt cannot observe changed answer choices.
- Submission uses the existing Phase 5 workflow with target type `QUIZ`. Aggregate validity is checked again both on submission and immediately before Moderator approval.
- Approval maps `PENDING` to `PUBLISHED`; rejection maps it to `REJECTED`; status, moderation record, notification, and audit writes share one transaction.

### Quiz attempts

- Only authenticated users can start an attempt.
- The quiz and place must both be published. The place must have coordinates.
- The backend calculates Haversine distance from the submitted coordinates and rejects a start beyond 200 meters. The verified time and distance are persisted.
- Attempt start creates a server-generated randomization seed, randomizes question and answer presentation, and stores question order plus immutable question/correct-answer/explanation snapshots.
- The server owns `started_at` and `expires_at`; clients cannot pause or extend the timer.
- A manual submission received before expiry is graded once. A submission or detail request at/after expiry follows the same locked grading path as `AUTO_SUBMITTED` and ignores late answers.
- Repeating submission for a terminal attempt returns the existing result without grading, awarding, notifying, or auditing again.
- Submitted answer IDs are checked against the snapshot question before grading, preventing cross-question or foreign-answer injection.

### Badge awards

- Active badge definitions associated through `quiz_badges` are awarded when their configured score threshold is met.
- The attempt row, answer results, score, badge awards, in-app notifications, and audit record are committed in one transaction.
- A pessimistic user lock and the existing unique `(user_id, badge_id)` key prevent duplicate awards across concurrent attempts.
- Badge and quiz-badge catalogs remain approved configuration data. Phase 7 does not create sample badges, loyalty points, or an unapproved global achievement formula.

## API contract

Public reads:

- `GET /api/v1/quizzes?placeId=&page=&size=`
- `GET /api/v1/quizzes/{quizId}`

Authenticated play and history:

- `POST /api/v1/quizzes/{quizId}/attempts`
- `GET /api/v1/quiz-attempts/{attemptId}`
- `POST /api/v1/quiz-attempts/{attemptId}/submit`
- `GET /api/v1/account/quiz-attempts?page=&size=`
- `GET /api/v1/account/badges?page=&size=`

Relic Manager authoring:

- `GET|POST /api/v1/management/quizzes`
- `GET|PUT|DELETE /api/v1/management/quizzes/{quizId}`
- `POST /api/v1/moderation/targets/QUIZ/{quizId}/submit`
- `GET /api/v1/moderation/targets/QUIZ/{quizId}/history`

## Frontend routes

- `/quizzes`: public quiz availability and GPS-backed start.
- `/quiz-attempts/{attemptId}`: protected timer, player, auto-submit, and result page.
- `/quiz-progress`: protected attempt history and earned badges.
- `/manage/quizzes`, `/manage/quizzes/new`, `/manage/quizzes/{id}/edit`: Relic Manager authoring workspace.
- `/moderation`: existing Moderator queue, now filterable by quiz.

## Deferred boundaries

- Editing or version-cloning a published quiz after it has attempt history. Published quiz aggregates remain immutable until that policy is approved.
- Soft deletion of individual questions/answers with historical attempts; authoring replaces the aggregate only while it is draft/rejected.
- Quiz media upload/association and administration of badge definitions or quiz-badge thresholds.
- A background scheduler for attempts whose clients never reconnect. Current timeout is enforced idempotently by the frontend timer and every backend attempt detail/submit request.
- GPS anti-spoofing, device attestation, and an approved accuracy/tolerance policy beyond the required server-side 200 m calculation.
- Global points, leaderboards, streaks, and cross-quiz achievement rules.

## Verification coverage

- Public catalog versus authenticated attempt/management boundaries and coordinate validation envelope.
- Relic Manager authoring gate, exactly-one-correct-answer invariant, and immutable published quiz behavior.
- 200 m geofence rejection.
- Correct grading, timeout rejection of late answers, and terminal submission idempotency.
- Quiz aggregate validation during moderation approval.
- Full backend regression suite and frontend production build.
