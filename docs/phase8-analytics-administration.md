# Phase 8 — Analytics & Administration

## Engagement ingestion

- `POST /api/v1/engagement-events` is public so anonymous and authenticated activity can share one contract.
- The backend owns `occurred_at` and derives `user_id` only from the authenticated principal. Clients cannot submit either field.
- `event_type_code` must reference an active row in `engagement_event_types`. Phase 8 does not seed an unapproved event catalog.
- Exactly one typed target is supplied by the DTO and mapped to one real FK: place, business, event, article, business post, promotion, or tour.
- The target must currently satisfy its public lifecycle rules. Business content also requires an active business linked to a published place; promotions must be active within their period; private tours are rejected.
- Metadata is limited to ten short string values. Keys use a strict format and keys containing password, token, secret, authorization, cookie, OTP, or hash terms are rejected.
- A session can persist at most 120 events per rolling minute.
- If the configured event code is `VIEW`, the same session/target is recorded at most once per 24 hours. A pessimistic lock on the event-type row serializes the dedup check and insert across application instances.
- Events are append-only; no normal update/delete endpoint exists.

The React detail pages use one session-scoped UUID and send `VIEW` without blocking page rendering. Until an approved `VIEW` row is configured, the backend rejects the event and the UI safely ignores the analytics failure.

## Analytics

All reports require inclusive `from` and `to` calendar dates and normalize their boundaries to UTC.

Business Owner analytics:

- `GET /api/v1/analytics/business?from=&to=`
- The service resolves the current user's active owned business. It does not accept a client-supplied business ID.
- Scope includes direct business events, its linked place, and its business posts and promotions.

Administrator analytics:

- `GET /api/v1/analytics/system?from=&to=`
- `GET /api/v1/admin/dashboard?from=&to=`
- Results include total events, distinct sessions, distinct authenticated users, event-type breakdown, and daily series.
- Dashboard adds user-status counts, published-place count, and active-business count.

Anonymous activity contributes to total/session metrics. The authenticated-user metric counts only non-null `user_id`, as required by the report.

## Account and role administration

- Only an effective `ADMINISTRATOR` can access `/api/v1/admin/users`.
- User search supports name/email text, status, and bounded pagination.
- `PUT /api/v1/admin/users/{id}/status` supports the confirmed transitions `ACTIVE → SUSPENDED|DEACTIVATED` and `SUSPENDED|DEACTIVATED → ACTIVE`.
- Pending verification is never bypassed. `DELETED` is not exposed because deletion/retention policy remains open.
- Administrators cannot change their own status or roles through this flow.
- Every status mutation checks the target version, requires a reason, revokes refresh tokens, and writes safe old/new audit values.
- `PUT|DELETE /api/v1/admin/users/{id}/roles/{roleCode}` manages only active roles already present in the official role table.
- Role changes require a reason, revoke refresh tokens, preserve assignment/revocation history, and cannot remove the user's final direct role.
- Atomic permission creation/mapping is not exposed while the permission catalog is intentionally empty.

## Audit viewer

- `GET /api/v1/admin/audit-logs` supports actor, action, entity type/ID, date range, and pagination filters.
- Audit storage remains append-only; no update/delete API is present.
- Old/new details are returned only after filtering sensitive key fragments and restricting values to bounded scalar data.
- Passwords, hashes, access/refresh tokens, OTPs, authorization headers, cookies, and secrets are never intentionally returned.

## Retention boundary

`GET /api/v1/admin/retention-status` reports engagement/audit counts and oldest timestamps. `deletionEnabled` is always false in this phase. No destructive retention command is implemented because the required duration, legal basis, archival, partition, and backup policies are still open.

## Frontend routes

- `/business-analytics`: Business Owner scope.
- `/admin/dashboard`: system metrics and retention status.
- `/admin/users`: user status and direct-role administration.
- `/admin/audit-logs`: sanitized audit search.

Backend authorization remains authoritative; route visibility is only a UX aid.

## Verification coverage

- Public ingestion and authenticated administration boundaries.
- `VIEW` 24-hour dedup, rolling session rate limit, and sensitive metadata rejection.
- System Administrator gate and invalid date range.
- Business report ownership without arbitrary business ID.
- Administrator self-protection, session revocation/audit on status changes, and final-direct-role protection.
- Full backend regression suite and frontend production build.

## Deferred decisions

- Official engagement event type/KPI catalog and whether codes beyond `VIEW` need special dedup semantics.
- Data retention/deletion, archival, monthly partitioning, and pre-aggregated reporting tables.
- Permission catalog and permission-management UI.
- Administrative `DELETED` lifecycle and legal erasure/anonymization workflow.
- Export formats, scheduled reports, alerting, funnels, attribution, and cross-device session identity.
