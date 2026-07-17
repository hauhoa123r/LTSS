# Phase 6 — Community & Tour

## Implemented scope

### Reviews and official replies

- Authenticated users can review a public `PLACE`, `BUSINESS`, `ARTICLE`, or `TOUR` target.
- A review is always created as `PENDING` and registered in the Phase 5 moderation workflow in the same transaction.
- Rating is 1–5; the trimmed comment is 20–5000 characters; `BLOCK` prohibited terms are matched case- and accent-insensitively.
- One review per user and target is enforced in the service and protected by the existing database unique keys.
- A review can reference at most three existing active `IMAGE` media assets. Phase 6 does not add a media-upload pipeline.
- Public lists return only `VISIBLE` reviews. Users can list their own reviews regardless of moderation state.
- A business owner can create one official reply only for a visible review whose target business belongs to that owner.
- Review approval and rejection use the existing moderator queue, notifications, history, audit, locks, and terminal-decision rules.

### Personal tours

- Owners can create and update `DRAFT` tours containing 2–10 unique `PUBLISHED` places.
- The request owns the complete ordered stop list, so reorder and metadata changes are one version-checked aggregate update.
- Owners can page through their tours and soft-delete editable drafts.
- Public search returns only `PUBLISHED` and `PUBLIC` tours. Direct detail supports `PUBLISHED` `PUBLIC` or `UNLISTED` tours.
- A published, non-private tour can be copied into a new private draft with `source_tour_id` preserved. The source owner can also copy their own published private tour.
- Only the owner can change a `PUBLISHED` tour's visibility between `PRIVATE`, `UNLISTED`, and `PUBLIC`.

## API contract

Public reads:

- `GET /api/v1/reviews?targetType=&targetId=&page=&size=`
- `GET /api/v1/tours?q=&page=&size=`
- `GET /api/v1/tours/{tourId}`

Authenticated community commands:

- `POST /api/v1/reviews/{targetType}/{targetId}`
- `GET /api/v1/account/reviews?page=&size=`
- `POST /api/v1/reviews/{reviewId}/reply`

Authenticated tour commands:

- `GET|POST /api/v1/account/tours`
- `PUT /api/v1/account/tours/{tourId}`
- `PUT /api/v1/account/tours/{tourId}/visibility`
- `DELETE /api/v1/account/tours/{tourId}?version=`
- `POST /api/v1/tours/{tourId}/copy`

All endpoints use the shared response envelope. Mutation failures use the existing validation, forbidden, not-found, conflict, and stale-version responses.

## Authorization and transaction boundaries

Target visibility, ownership, duplicate review, prohibited terms, media type/state, item count, unique stops, published-place eligibility, and optimistic versions are enforced by backend services. Frontend navigation and disabled controls are only UX aids.

Review creation, review media links, moderation registration, moderator notifications, and audit writes share one transaction. Tour aggregate mutations and their audit writes also share one transaction. Tour copy validates the source before writing the new tour and all ordered items.

## Deliberately deferred

- “Đã trải nghiệm” review eligibility: no check-in, booking, or payment evidence model exists, so no synthetic rule is introduced.
- Tour submit, reviewer assignment, approval, rejection, completion, and publication: the analysis report explicitly marks the reviewer and publication policy TBD.
- Review edit/delete/re-review and moderator hide/remove: actor and state-transition rules are still open.
- Reply edit/delete and review-media upload/ownership flow.
- Tour media, map estimation, transport configuration, and collaborative editing.

These are policy boundaries, not silently incomplete state transitions. Published tours can be read, shared, and copied when such rows already exist through reviewed data administration, but this phase does not create an unauthorized publication path.

## Verification coverage

- Public versus authenticated controller boundaries and validation envelopes.
- Duplicate reviews, prohibited terms, pending moderation registration, and wrong-business reply rejection.
- Review moderation approval to `VISIBLE`.
- Unique tour stops, optimistic-lock conflicts, private-copy rejection, and draft-share rejection.
- Full backend regression suite and production frontend build.
