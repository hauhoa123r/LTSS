# Local Tourism Support System (LTSS)

This repository contains the Phase 0 schema gate, Phase 1 project foundation, the self-service portion of Phase 2 Identity & Access, the public/tourist portion of Phase 3 Discovery & Place, the public read slice of Phase 4 Business & Content, Phase 5 moderation, Phase 6 Community & Tour, Phase 7 Quiz & Gamification, and the policy-safe portion of Phase 8 Analytics & Administration. Production deployment and the still-deferred authoring modules are not included yet.

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 22 and npm
- Docker with Docker Compose, or a separately managed MySQL 8 instance

## Configuration

Copy the root environment template before using Docker Compose:

```bash
cp .env.example .env
```

Replace every `change-me` value in `.env`. The real `.env` file is ignored by Git. In addition to database and CORS settings, Phase 2 requires a random `JWT_SECRET` of at least 32 characters. Set `REFRESH_COOKIE_SECURE=true` behind production HTTPS.

Account emails are disabled by default. Configure the `MAIL_*` variables and set `MAIL_ENABLED=true` to deliver verification links, password-reset links, and password-change OTPs. Security tokens are never returned by an API or written to logs.

## Run with Docker Compose

```bash
docker compose config
docker compose up -d --build
docker compose ps
```

The development services are exposed at:

- Frontend: `http://localhost:5173`
- Backend health: `http://localhost:8080/api/v1/health`
- MySQL: `localhost:3306` by default

Stop the environment with `docker compose down`. Add `-v` only when you intentionally want to remove the MySQL named volume.

## Run locally

Start a MySQL 8 database first, then configure `backend/.env`. The backend
automatically loads this file when launched from either the repository root or
the `backend` directory. Environment variables still take precedence.

The equivalent shell variables are:

```bash
export DB_URL='jdbc:mysql://localhost:3306/ltss?serverTimezone=UTC'
export DB_USERNAME='ltss_app'
export DB_PASSWORD='<your local database password>'
export CORS_ALLOWED_ORIGINS='http://localhost:5173'
export JWT_SECRET='<at least 32 random characters>'
export REFRESH_COOKIE_SECURE='false'
```

PowerShell can also use equivalent `$env:NAME='value'` assignments. Docker
Compose continues to use the root `.env`, while local Spring Boot uses
`backend/.env`.

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

In another terminal:

```bash
cd frontend
npm install
npm run dev
```

The frontend environment template defaults to `http://localhost:8080/api/v1`.

## Phase 2 Identity & Access

New accounts use the schema-compatible `PENDING_VERIFICATION` state and become `ACTIVE` only after email verification. Access tokens are signed JWTs valid for 15 minutes. Opaque refresh tokens are valid for seven days, rotated on refresh, stored only as SHA-256 hashes in MySQL, and transported in an `HttpOnly` cookie. The frontend keeps access tokens in memory instead of local storage.

Public APIs:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/email/verify`
- `POST /api/v1/auth/email/resend`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/password/forgot`
- `POST /api/v1/auth/password/reset`

Authenticated account APIs:

- `GET|PUT /api/v1/account/me`
- `POST /api/v1/account/password/change-otp`
- `PUT /api/v1/account/password`

Phase 8 now provides Administrator-only account-status and official-role assignment flows with optimistic locking, session revocation, reason capture, and audit. Atomic permission administration remains deferred until the official permission catalog is approved. The current JWT resolves direct and inherited roles and their database-backed permissions; no permission codes are invented in application code.

## Phase 3 Discovery & Place

Public APIs provide active place categories, published-place search, place detail with relic/media/hotspot data, and nearby search limited to five kilometers. Authenticated users additionally receive favorite state, keep at most ten normalized recent searches, and can manage their favorite places.

Public endpoints:

- `GET /api/v1/place-categories`
- `GET /api/v1/places?q=&category=&page=&size=`
- `GET /api/v1/places/nearby?latitude=&longitude=&radiusKm=`
- `GET /api/v1/places/{slug}`

Authenticated endpoints:

- `POST|DELETE /api/v1/places/{placeId}/favorite`
- `GET /api/v1/account/favorites`
- `GET|DELETE /api/v1/account/search-history`
- `DELETE /api/v1/account/search-history/{historyId}`

The frontend exposes `/places`, `/places/{slug}`, and the protected `/favorites` route. No sample categories or places are seeded; public results come only from reviewed `PUBLISHED` database records.

## Phase 4 Business & Content

Public APIs expose active businesses linked to published places, published business posts, currently active promotions, active article categories, published articles, and upcoming published events.

Public endpoints:

- `GET /api/v1/businesses?q=&page=&size=` and `GET /api/v1/businesses/{id}`
- `GET /api/v1/business-posts?q=&businessId=&page=&size=` and `GET /api/v1/business-posts/{slug}`
- `GET /api/v1/promotions?businessId=&page=&size=` and `GET /api/v1/promotions/{id}`
- `GET /api/v1/article-categories`
- `GET /api/v1/articles?q=&category=&page=&size=` and `GET /api/v1/articles/{slug}`
- `GET /api/v1/events?q=&page=&size=` and `GET /api/v1/events/{slug}`

The frontend exposes `/businesses`, `/articles`, `/events`, and their detail routes. Phase 4 does not seed sample content. Business/content authoring, ownership mutation, and upload remain deferred.

## Phase 5 Moderation

Authenticated owners can submit an existing draft or rejected article, event, business post, or promotion. Moderators and administrators can inspect the pending queue and make one terminal decision. Submission, target state, notification, and audit writes share one transaction; target rows and cases are locked and target versions are checked to prevent duplicate or stale decisions.

Authenticated endpoints:

- `POST /api/v1/moderation/targets/{targetType}/{targetId}/submit`
- `GET /api/v1/moderation/targets/{targetType}/{targetId}/history`
- `GET /api/v1/moderation/{caseId}`
- `POST /api/v1/moderation/{caseId}/cancel`
- `GET /api/v1/account/notifications`
- `GET /api/v1/account/notifications/unread-count`
- `POST /api/v1/account/notifications/{notificationId}/read`

Moderator/administrator endpoints:

- `GET /api/v1/moderation/queue?targetType=&page=&size=`
- `POST /api/v1/moderation/{caseId}/approve`
- `POST /api/v1/moderation/{caseId}/reject`

Supported target types are `ARTICLE`, `EVENT`, `BUSINESS_POST`, `PROMOTION`, `REVIEW`, and `QUIZ`. Tour moderation remains deferred until its reviewer policy exists. The frontend provides protected `/moderation` and `/notifications` routes.

## Phase 6 Community & Tour

Authenticated users can create one pending review per public place, business, article, or tour. Reviews enforce a 1–5 rating, a trimmed comment of 20–5000 characters, prohibited-term blocking, and at most three existing image assets. Moderators publish or reject reviews through Phase 5. A business owner can add one official reply only to a visible review of their own business.

Tour owners can create and edit private drafts with 2–10 unique published places, reorder stops, list their tours, and soft-delete drafts. Published tours can be listed or opened according to visibility, copied into a new private draft, and switched between `PRIVATE`, `UNLISTED`, and `PUBLIC` by their owner. Tour publication itself is intentionally not implemented because the reviewer and publication policy is still marked TBD in the analysis report.

Public endpoints:

- `GET /api/v1/reviews?targetType=&targetId=&page=&size=`
- `GET /api/v1/tours?q=&page=&size=` and `GET /api/v1/tours/{tourId}`

Authenticated endpoints:

- `POST /api/v1/reviews/{targetType}/{targetId}`
- `GET /api/v1/account/reviews`
- `POST /api/v1/reviews/{reviewId}/reply`
- `GET|POST /api/v1/account/tours`
- `PUT|DELETE /api/v1/account/tours/{tourId}`
- `PUT /api/v1/account/tours/{tourId}/visibility`
- `POST /api/v1/tours/{tourId}/copy`

The frontend provides public `/tours` and `/tours/{tourId}` routes, protected tour-management routes under `/my-tours`, and review panels on supported target detail pages. The exact implementation boundaries and deferred decisions are documented in [`docs/phase6-community-tour.md`](docs/phase6-community-tour.md).

## Phase 7 Quiz & Gamification

Relic Managers can create and edit quiz aggregates for published places. Every quiz contains at least one question, each question has 2–4 active answers with exactly one correct answer, and the time limit is 1–600 seconds. Draft or rejected quizzes are submitted through Phase 5; a Moderator validates the complete aggregate before publishing it.

Authenticated users can start a published quiz only after backend Haversine verification places them within 200 meters of its destination. Each attempt stores a randomization seed, randomized question order, immutable question/correct-answer snapshots, verification distance, and expiry. Manual and timeout submission use one locked, idempotent grading path. Score, snapshots, badge awards, notifications, and audit writes share the transaction.

Public endpoints:

- `GET /api/v1/quizzes?placeId=&page=&size=`
- `GET /api/v1/quizzes/{quizId}`

Authenticated play and achievement endpoints:

- `POST /api/v1/quizzes/{quizId}/attempts`
- `GET /api/v1/quiz-attempts/{attemptId}`
- `POST /api/v1/quiz-attempts/{attemptId}/submit`
- `GET /api/v1/account/quiz-attempts`
- `GET /api/v1/account/badges`

Relic Manager endpoints:

- `GET|POST /api/v1/management/quizzes`
- `GET|PUT|DELETE /api/v1/management/quizzes/{quizId}`
- Submit and moderation history use the existing `/api/v1/moderation/targets/QUIZ/{quizId}/...` contract.

The frontend provides `/quizzes`, protected attempt/result and achievement pages, plus authoring routes under `/manage/quizzes`. Badge definitions and quiz-badge mappings remain configuration data: the application does not invent or seed a catalog. See [`docs/phase7-quiz-gamification.md`](docs/phase7-quiz-gamification.md).

## Phase 8 Analytics & Administration

The public engagement endpoint accepts only active event-type codes already present in `engagement_event_types`, validates one supported public target, limits metadata to a small safe map, and owns the event timestamp and authenticated user identity. The configured `VIEW` code receives 24-hour session/target deduplication. A database lock on the event-type row closes the concurrent check/insert race, and each session is limited to 120 persisted events per minute.

Business Owners receive analytics only for their active owned business, associated place, posts, and promotions; no arbitrary business ID is accepted. Administrators receive system engagement reports, content/account dashboard counts, sanitized append-only audit search, retention status, and user/status/role management. Status and role changes require reasons, revoke refresh tokens, prohibit self-administration, and write safe old/new audit values.

Public endpoint:

- `POST /api/v1/engagement-events`

Business Owner endpoint:

- `GET /api/v1/analytics/business?from=&to=`

Administrator endpoints:

- `GET /api/v1/analytics/system?from=&to=`
- `GET /api/v1/admin/dashboard?from=&to=`
- `GET /api/v1/admin/retention-status`
- `GET /api/v1/admin/audit-logs?from=&to=&actorId=&action=&entityType=&entityId=`
- `GET /api/v1/admin/users` and `GET /api/v1/admin/users/{userId}`
- `PUT /api/v1/admin/users/{userId}/status`
- `PUT|DELETE /api/v1/admin/users/{userId}/roles/{roleCode}`

The frontend tracks supported public detail views and exposes business analytics plus Administrator dashboard, users, and audit routes. Event types are deliberately not seeded because the official event/KPI catalog remains open. Retention deletion is disabled and exposed read-only until a duration and legal policy are approved. See [`docs/phase8-analytics-administration.md`](docs/phase8-analytics-administration.md).

## Verification

```bash
cd backend
mvn clean test
```

The MySQL/Testcontainers test runs when Docker is available and is reported as skipped when Docker cannot be reached. No H2 substitute is used.

```bash
cd frontend
npm install
npm run build
```

Test the public endpoint with:

```bash
curl http://localhost:8080/api/v1/health
```

## Schema ownership and design notes

- The preserved source design is [`database/ltss_database_mysql8.sql`](database/ltss_database_mysql8.sql); Flyway does not modify that file.
- Corrected executable migrations live in [`backend/src/main/resources/db/migration`](backend/src/main/resources/db/migration).
- Schema changes and application-enforced invariants are documented in [`docs/schema-corrections.md`](docs/schema-corrections.md).
- Future entity mappings must follow [`docs/jpa-mapping-guide.md`](docs/jpa-mapping-guide.md).

Flyway owns schema creation and Hibernate is configured with `ddl-auto=validate`. Credentials, tokens, and other secrets must stay outside source control.
