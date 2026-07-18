-- Demo analytics data for UC-55. The data intentionally spans the current
-- and previous 30-day windows used by the admin monument statistics page.
INSERT INTO engagement_event_types (event_type_code, event_type_name, description)
VALUES ('PLACE_VIEW', 'Place view', 'User views a place detail page')
ON DUPLICATE KEY UPDATE
    event_type_name = VALUES(event_type_name),
    description = VALUES(description),
    is_active = TRUE;

SET @demo_tourist_user_id = (SELECT id FROM users WHERE email = 'tourist@gmail.com' LIMIT 1);

INSERT INTO engagement_events (event_type_code, user_id, session_key, place_id, metadata, occurred_at)
WITH RECURSIVE
day_series AS (
    SELECT 0 AS day_index, DATE_SUB(CURRENT_DATE, INTERVAL 59 DAY) AS occurred_day
    UNION ALL
    SELECT day_index + 1, DATE_ADD(occurred_day, INTERVAL 1 DAY)
    FROM day_series
    WHERE day_index < 59
),
slot_series AS (
    SELECT 0 AS slot_index
    UNION ALL
    SELECT slot_index + 1
    FROM slot_series
    WHERE slot_index < 34
),
ranked_places AS (
    SELECT
        p.id AS place_id,
        p.slug,
        ROW_NUMBER() OVER (
            ORDER BY
                CASE
                    WHEN LOWER(p.name) LIKE '%thanh co%' OR LOWER(p.slug) LIKE '%thanh-co%' THEN 1
                    WHEN LOWER(p.name) LIKE '%den va%' OR LOWER(p.slug) LIKE '%den-va%' THEN 2
                    WHEN LOWER(p.name) LIKE '%chua mia%' OR LOWER(p.slug) LIKE '%chua-mia%' THEN 3
                    WHEN LOWER(p.name) LIKE '%duong lam%' OR LOWER(p.slug) LIKE '%duong-lam%' THEN 4
                    WHEN LOWER(p.name) LIKE '%dong mo%' OR LOWER(p.slug) LIKE '%dong-mo%' THEN 5
                    ELSE 20
                END,
                p.id
        ) AS place_rank
    FROM places p
    WHERE p.status = 'PUBLISHED'
      AND p.deleted_at IS NULL
    ORDER BY p.id
    LIMIT 30
),
visit_plan AS (
    SELECT
        d.day_index,
        d.occurred_day,
        rp.place_id,
        rp.place_rank,
        GREATEST(1, 18 - rp.place_rank)
            + CASE WHEN DAYOFWEEK(d.occurred_day) IN (1, 7) THEN 5 ELSE 0 END
            + CASE WHEN d.day_index >= 30 THEN 4 ELSE 0 END
            + CASE WHEN MOD(d.day_index + rp.place_rank, 11) = 0 THEN 7 ELSE 0 END
            + CASE WHEN rp.place_rank IN (1, 2, 3) AND d.day_index IN (38, 45, 52, 59) THEN 5 ELSE 0 END
            AS planned_visits
    FROM day_series d
    JOIN ranked_places rp
)
SELECT
    'PLACE_VIEW',
    CASE WHEN MOD(vp.day_index + ss.slot_index + vp.place_rank, 4) = 0 THEN @demo_tourist_user_id ELSE NULL END,
    CONCAT('uc55-demo-', DATE_FORMAT(vp.occurred_day, '%Y%m%d'), '-', vp.place_id, '-', LPAD(ss.slot_index, 2, '0')),
    vp.place_id,
    JSON_OBJECT(
        'source', 'uc55-demo-statistics-seed',
        'channel', CASE
            WHEN MOD(ss.slot_index, 5) = 0 THEN 'search'
            WHEN MOD(ss.slot_index, 5) = 1 THEN 'map'
            WHEN MOD(ss.slot_index, 5) = 2 THEN 'article'
            WHEN MOD(ss.slot_index, 5) = 3 THEN 'tour'
            ELSE 'direct'
        END
    ),
    TIMESTAMP(
        vp.occurred_day,
        MAKETIME(8 + MOD(ss.slot_index, 10), MOD(vp.place_rank * 7 + ss.slot_index * 11, 60), 0)
    )
FROM visit_plan vp
JOIN slot_series ss ON ss.slot_index < vp.planned_visits
WHERE NOT EXISTS (
    SELECT 1
    FROM engagement_events existing
    WHERE existing.event_type_code = 'PLACE_VIEW'
      AND existing.session_key LIKE 'uc55-demo-%'
);
