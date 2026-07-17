# LTSS schema corrections for Flyway

## 1. Scope and source of truth

`database/ltss_database_mysql8.sql` remains the immutable design baseline. It is not a Flyway migration because it provisions/selects a database and contains several constraints that MySQL 8 cannot create. The deployable baseline is:

- `backend/src/main/resources/db/migration/V1__create_ltss_schema.sql`
- `backend/src/main/resources/db/migration/V2__seed_foundation_data.sql`

V1 keeps all 49 tables, column names, foreign keys, referential actions, unique keys, and indexes from the design unless a correction is explicitly listed below. It intentionally omits `CREATE DATABASE` and `USE`; the datasource-selected schema is provisioned outside Flyway. MySQL 8.0.16 or newer is required because earlier 8.0 releases parse but do not enforce `CHECK` constraints.

The original SQL is not edited so that reviewers can compare the SRS-derived design with the executable baseline and so future corrections remain visible as migration history instead of silent changes.

## 2. MySQL `CHECK` and foreign-key incompatibilities

MySQL prohibits an `AUTO_INCREMENT` column in a `CHECK` expression. It also prohibits foreign-key referential actions on columns used by a `CHECK`. Removing `ON DELETE` clauses would weaken history and ownership protection, so V1 preserves every FK/action and moves the affected cross-column invariants to transactional application validation. No triggers are introduced in Phase 0.

| Table | Original constraint | V1 correction | Required service validation |
|---|---|---|---|
| `role_inheritances` | `chk_role_inheritance_not_self` | Removed; both role FKs and `CASCADE`/`RESTRICT` actions remain. | Reject self-inheritance and any edge that creates a cycle while locking/validating the role graph in one transaction. |
| `tours` | `chk_tours_not_self_source` | Removed; the self-FK and `ON DELETE SET NULL` remain. | A copied tour must reference an existing, visible source other than itself; never trust a client-provided owner/source relationship. |
| `reviews` | `chk_reviews_exactly_one_target` | Removed; four target FKs and four per-user/target unique keys remain. | Require exactly one target, verify target eligibility, then insert the review, media, and moderation submission atomically. |
| `engagement_events` | `chk_engagement_exactly_one_target` | Removed; all seven target FKs remain. | Require exactly one supported target and validate that the event type is allowed for that target before append. |
| `moderation_records` | `chk_moderation_exactly_one_target` | Removed; all nine target FKs remain. | Require exactly one target, lock it, and prevent concurrent duplicate pending cases. |
| `panorama_hotspots` | `chk_panorama_hotspots_target`, `chk_panorama_hotspots_not_self` | Removed; source `CASCADE` and target `RESTRICT` FKs remain. | `TRANSITION` requires a target; `INFO` must not have one; source and target differ, both are active `PANORAMA_360` assets on the same place, and hotspots do not overlap. |

These validations are mandatory even when entities are created by administrative jobs or imports. Repository methods that bypass the owning service must not be exposed as general write APIs.

Reference: [MySQL 8.0 CHECK Constraints](https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html).

## 3. `media_assets.storage_key`

The original `UNIQUE(storage_key)` indexes up to 4,000 bytes for `VARCHAR(1000)` under `utf8mb4`, exceeding InnoDB's 3,072-byte key limit on the normal 16 KiB page size. A unique prefix index was rejected because two distinct object keys can share the indexed prefix.

V1 makes the following change:

```sql
storage_key VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
storage_key_hash BINARY(32)
    GENERATED ALWAYS AS (UNHEX(SHA2(storage_key, 256))) STORED,
CONSTRAINT uq_media_assets_storage_key_hash UNIQUE (storage_key_hash)
```

The full exact-case key is retained. The service must accept the canonical key returned by the storage provider, normalize it to the application's documented Unicode form before persistence, and never independently write the generated hash. Lookups use the hash and compare the returned full `storage_key` as a defensive collision check. SHA-256 provides a fixed 32-byte unique lookup without lossy prefix semantics.

JPA maps `storage_key_hash` as a generated read-only `byte[]` (`insertable = false`, `updatable = false`). The application writes only `storage_key`.

Reference: [InnoDB limits](https://dev.mysql.com/doc/refman/8.0/en/innodb-limits.html) and [generated columns](https://dev.mysql.com/doc/refman/8.0/en/create-table-generated-columns.html).

## 4. NULL and timestamp corrections

MySQL accepts a `CHECK` expression that evaluates to `UNKNOWN`, so nullable operands must be handled explicitly. V1 changes these constraints:

| Table/constraint | Correction |
|---|---|
| `notifications.chk_notifications_read_state` | Unread requires `read_at IS NULL`; read requires `read_at IS NOT NULL`. |
| `user_roles.chk_user_roles_revocation` | Active requires no revocation timestamp; inactive requires a revocation timestamp. The actor remains nullable because its user FK uses `SET NULL`. |
| `promotions.chk_promotions_percentage` | `PERCENTAGE` now requires non-null `discount_value` in the inclusive range 0–100. Nullable type still represents a non-quantified offer as documented. |
| `moderation_records.chk_moderation_rejection_reason` | `REJECTED` explicitly requires a non-null, nonblank reason. |
| `moderation_records.chk_moderation_resolution` | `PENDING` has no decision/time; `CANCELLED` has no decision but must have `resolved_at`; `RESOLVED` requires both decision and `resolved_at`. Moderator identity remains service-enforced because it is an FK-action column. |
| `quiz_attempts.chk_quiz_attempts_submission_timestamp` | `IN_PROGRESS` has no submission timestamp; `SUBMITTED` and `AUTO_SUBMITTED` require one. `ABANDONED` is left to the service because the schema has no dedicated abandonment timestamp. |

V1 adds conservative one-way lifecycle checks where the timestamp meaning is unambiguous:

- `PUBLISHED` requires `published_at` for places, events, articles, business posts, tours, and quizzes.
- `ACTIVE` promotion requires `published_at`.
- `VISIBLE` review requires `published_at`.
- `COMPLETED` tour requires `completed_at`.
- `DELETED` requires `deleted_at` for places, events, articles, business posts, promotions, tours, and quizzes.

The checks deliberately do not erase historical timestamps when an object later becomes archived, rejected, hidden, or otherwise non-public.

## 5. Rules that remain application-owned

The following rules cannot be safely represented by the current relational shape or depend on business decisions that are still open:

- Role inheritance cycle detection and effective permission expansion.
- Exactly one polymorphic target for review, engagement, and moderation rows.
- One pending moderation case per target and immutable terminal moderation decisions.
- Two to ten unique tour stops as an aggregate, one correct answer among two to four active answers, at most ten search terms, and one view per session/target/24 hours.
- Panorama asset type, same-place transition, overlap detection, and one primary media per target/usage.
- Ownership/authorization, moderation scope, review eligibility, geofence rules, and all valid state transitions.
- Timestamp coherence for unresolved states such as user activation/deactivation, business approval, event cancellation, promotion expiration, tour cancellation/archive, review hide/remove, and abandoned quiz attempts.

State-changing services must lock the aggregate or use optimistic concurrency and write target state, history, notification, and audit data in one transaction. Database constraints are a final integrity layer, not a substitute for authorization or aggregate validation.

## 6. Foundation seed policy

V2 seeds only the five persisted actor roles confirmed by the architecture:

- `TOURIST`
- `BUSINESS_OWNER`
- `RELIC_MANAGER`
- `MODERATOR`
- `ADMINISTRATOR`

It also seeds the two confirmed inheritance edges: `BUSINESS_OWNER -> TOURIST` and `ADMINISTRATOR -> MODERATOR`. Guest is not a row. No account, fixed password, permission, badge, prohibited term, content, or sample business data is created.

The permission catalog and exact engagement event-type catalog are still explicit open questions in the architecture report. V2 therefore leaves both tables empty rather than inventing codes. They must be introduced by a later versioned migration after approval.

## 7. JPA impact

- Entities with `version INT UNSIGNED` map that field with `@Version` and `Integer`.
- The six moved invariant groups require feature validators/services; entity annotations alone are insufficient.
- Polymorphic target tables should use explicit nullable associations and a single target-discriminator helper in the domain layer, not Hibernate `@Any` and not raw entity serialization.
- The generated `storage_key_hash` is read-only in JPA.
- Fourteen association tables use composite identifiers; see `docs/jpa-mapping-guide.md`.
- JPA cascade/orphan removal must follow the DDL's `ON DELETE` semantics. `RESTRICT` and history tables must never receive blanket `CascadeType.ALL` or `REMOVE`.
- Hibernate remains `ddl-auto=validate`; it must never create/update this schema.

## 8. Verification gate

The acceptance test for Phase 0 is a clean MySQL 8 instance running both migrations through Flyway, followed by Hibernate schema validation and checks that:

1. 49 domain tables plus Flyway history exist.
2. Both migrations are successful.
3. All FK names/actions from the original design remain.
4. The seven incompatible original `CHECK` constraints are absent and their replacement comments/docs exist.
5. A duplicate canonical `storage_key` is rejected through `uq_media_assets_storage_key_hash`.
6. Invalid read-state, percentage discount, rejection reason, resolution timestamp, and safe lifecycle timestamp rows are rejected.

