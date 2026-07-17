# Phase 2 — Identity & Access

## Confirmed implementation choices

- Registration creates a `PENDING_VERIFICATION` user and assigns the seeded `TOURIST` role in one transaction.
- Verification changes the account to `ACTIVE`. The 24-hour verification-link lifetime is a technical assumption because the source rules specify 10 minutes only for OTP/reset tokens.
- Access JWT lifetime is 15 minutes; refresh lifetime is seven days.
- Refresh tokens are random opaque values. Only their lowercase SHA-256 hash is persisted. Refresh rotates the token and logout revokes it.
- Refresh transport is an `HttpOnly`, `SameSite=Lax` cookie scoped to `/api/v1/auth`. Production must use HTTPS and `Secure=true`.
- Access tokens remain in frontend memory. A page reload bootstraps a new access token through the refresh cookie.
- Password reset and change revoke all active refresh tokens. Password change requires the current password and a six-digit email OTP.
- Login errors are generic. The sixth consecutive bad password temporarily locks the account for 15 minutes.
- Effective roles and permissions are resolved recursively from the RBAC tables. JWT validation also checks current database account status and temporary lock state on every authenticated request.
- Account security changes write append-only `audit_logs` rows. Tokens, OTPs, and passwords are never included in logs or API responses.

## Email delivery

Account email is published only after the database transaction commits. SMTP failure is logged without recipient or token data, and users can request a replacement message after the 60-second cooldown. This is not a durable outbox; a persisted encrypted outbox and retry worker remain an operational hardening item once key management is selected.

## Deferred scope

Administrator APIs for role and account-status changes are deferred. The architecture requires permission-based enforcement and auditing, but the official permission catalog has not been approved. Adding hard-coded permission names or authorizing solely by a role string would contradict the analysis report.

## Frontend routes

- `/register`, `/verify-email`
- `/login`, `/forgot-password`, `/reset-password`
- `/profile` (protected; profile and password security forms)

All forms use the shared API error envelope, include loading/success/error states, and retain the existing request-ID flow.
