# Phase 4 — Business & Content

## Implemented scope

- Public active-business search and detail, restricted to businesses whose linked place is `PUBLISHED`.
- Public published business-post search, business filter, detail, ordered media, and tags.
- Public current-promotion list and detail. A promotion is visible only when its business is active, its place is published, its status is `ACTIVE`, and `start_at <= now < end_at`.
- Public active article categories, published article search/category filter/detail, linked published place, and ordered media.
- Public upcoming published-event search and detail with optional linked published place and ordered media.
- React list/detail routes for businesses, posts, promotions, articles, and events, including loading, empty, error, pagination, and responsive states.
- Relic Manager article authoring for published places: own-article pagination, draft creation, draft/rejected editing, soft deletion, and submission through the existing moderation workflow.

## API and data boundaries

REST controllers return DTOs in the shared API envelope and cap page sizes at 50. JPA entities are never serialized directly. All public visibility constraints are applied in repository queries and rechecked where a detail lookup crosses aggregates.

Article or event records may reference a non-public place without exposing that place: the public response returns no linked-place DTO unless the place itself is `PUBLISHED`. Business content is stricter because an active business is public only when its required linked place is public.

Media is read from the existing association tables and canonical media assets. This phase does not mutate media or introduce another storage format.

## Deferred scope

- Business registration, owner assignment, profile editing, suspension, and ownership transfer.
- Post, promotion, event, tag, and media authoring. Article authoring is implemented for Relic Managers, and article-category CRUD is implemented for Moderators and Administrators.
- Content authoring and archive transitions. Submit/cancel/approve/reject are now implemented by the Phase 5 workflow for existing target rows.
- Media upload, validation, primary-media mutation, and orphan cleanup.
- Permission-specific management routes and dashboards.

Those remaining workflows require the approved permission catalog, complete authoring ownership rules, and storage policy. See `docs/phase5-moderation.md` for the implemented state-transition boundary.
