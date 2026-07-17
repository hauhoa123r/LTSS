# Phase 3 — Discovery & Place

## Implemented scope

- Public active-category list.
- Public published-place search by name, summary, description, and category.
- Exact case-insensitive name matches are ordered first. The database's Vietnamese accent-insensitive collation supplies accent-insensitive comparison.
- Nearby search uses the Haversine distance and rejects radii above 5 km.
- Public place detail includes relic extension, ordered media, audio/video, panorama metadata, and active hotspots.
- Transition hotspots are returned only when the target panorama is associated with the same place.
- Authenticated favorite add/remove/list operations are ownership-scoped and idempotent.
- Authenticated search history uses an atomic MySQL upsert and transactionally retains the ten latest normalized unique keywords.
- Frontend discovery supports search, category filters, browser geolocation, coordinate map, pagination, favorite state, detail/media states, and a protected favorites page.

## API and data boundaries

Only `places.status = PUBLISHED` and active categories are exposed publicly. Entities are not serialized through REST; controllers return response DTOs in the shared envelope. Page sizes are limited to 50 and nearby input validates coordinate ranges.

Media assets are read from their canonical `media_url`; this phase does not add upload or object-storage mutation. The panorama UI provides an equirectangular horizontal viewer and hotspot descriptions without adding an unapproved viewer SDK.

## Deferred scope

- Place/category/relic authoring and moderation.
- Media upload, server-side MIME/dimension inspection, primary-media mutation, and orphan cleanup.
- Panorama hotspot authoring and overlap validation.
- Relic Manager ownership/assignment rules and permission codes.
- A versioned authoritative Sơn Tây boundary polygon and geofence validation.
- Typo-tolerant fuzzy search acceptance. Current search is accent-insensitive substring search backed by the approved MySQL schema.

These functions require the official permission catalog, ownership policy, storage provider, or geographic reference data and are not hard-coded in this phase.
