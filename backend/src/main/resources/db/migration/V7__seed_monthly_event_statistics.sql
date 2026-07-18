-- Demo data for UC-58 monthly event analytics. Events are anchored to the
-- month when this migration runs so the default current-month dashboard is useful.
INSERT INTO engagement_event_types (event_type_code, event_type_name, description)
VALUES ('VIEW', 'View', 'User views a supported public content target')
ON DUPLICATE KEY UPDATE
    event_type_name = VALUES(event_type_name),
    description = VALUES(description),
    is_active = TRUE;

SET @uc58_author_id = (SELECT id FROM users WHERE email = 'moderator@gmail.com' LIMIT 1);
SET @uc58_tourist_id = (SELECT id FROM users WHERE email = 'tourist@gmail.com' LIMIT 1);
SET @uc58_place_id = (
    SELECT id FROM places
    WHERE status = 'PUBLISHED'
    ORDER BY CASE WHEN slug = 'demo-thanh-co-son-tay' THEN 0 ELSE 1 END, id
    LIMIT 1
);
SET @uc58_month_start = DATE_FORMAT(CURRENT_DATE, '%Y-%m-01');

INSERT INTO events (
    place_id, created_by_user_id, updated_by_user_id, title, slug, description,
    start_at, end_at, location_note, status, submitted_at, published_at
)
SELECT @uc58_place_id, @uc58_author_id, @uc58_author_id,
       event_seed.title,
       CONCAT(event_seed.slug_prefix, '-', DATE_FORMAT(CURRENT_DATE, '%Y%m')),
       event_seed.description,
       TIMESTAMP(event_seed.start_day, event_seed.start_time),
       TIMESTAMP(event_seed.end_day, event_seed.end_time),
       event_seed.location_note,
       'PUBLISHED',
       TIMESTAMP(DATE_SUB(event_seed.start_day, INTERVAL 14 DAY), '08:00:00'),
       TIMESTAMP(DATE_SUB(event_seed.start_day, INTERVAL 10 DAY), '08:00:00')
FROM (
    SELECT
        'Đêm văn hóa Thành cổ Sơn Tây' AS title,
        'uc58-demo-dem-van-hoa-thanh-co' AS slug_prefix,
        'Chương trình biểu diễn văn hóa phục vụ báo cáo du lịch theo tháng.' AS description,
        DATE_ADD(@uc58_month_start, INTERVAL 3 DAY) AS start_day,
        DATE_ADD(@uc58_month_start, INTERVAL 3 DAY) AS end_day,
        '19:00:00' AS start_time,
        '21:30:00' AS end_time,
        'Thành cổ Sơn Tây' AS location_note
    UNION ALL
    SELECT 'Chợ phiên làng cổ Đường Lâm', 'uc58-demo-cho-phien-duong-lam',
           'Hoạt động giới thiệu sản vật địa phương và làng nghề.',
           DATE_ADD(@uc58_month_start, INTERVAL 9 DAY), DATE_ADD(@uc58_month_start, INTERVAL 9 DAY),
           '08:00:00', '11:30:00', 'Làng cổ Đường Lâm'
    UNION ALL
    SELECT 'Ngày hội trải nghiệm xứ Đoài', 'uc58-demo-ngay-hoi-xu-doai',
           'Sự kiện đang diễn ra trong ngày để kiểm thử trạng thái active.',
           CURRENT_DATE, CURRENT_DATE,
           '07:30:00', '23:30:00', 'Sơn Tây, Hà Nội'
    UNION ALL
    SELECT 'Tour mùa sen Đồng Mô', 'uc58-demo-tour-mua-sen-dong-mo',
           'Hoạt động sắp diễn ra phục vụ nhóm khách du lịch mùa vụ.',
           DATE_ADD(@uc58_month_start, INTERVAL 22 DAY), DATE_ADD(@uc58_month_start, INTERVAL 22 DAY),
           '06:30:00', '10:30:00', 'Khu du lịch Đồng Mô'
    UNION ALL
    SELECT 'Lễ hội ẩm thực gà Mía', 'uc58-demo-le-hoi-ga-mia',
           'Sự kiện kết nối doanh nghiệp địa phương với khách tham quan.',
           DATE_ADD(@uc58_month_start, INTERVAL 27 DAY), DATE_ADD(@uc58_month_start, INTERVAL 27 DAY),
           '16:00:00', '21:00:00', 'Phố đi bộ Sơn Tây'
) event_seed
WHERE @uc58_place_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    place_id = VALUES(place_id),
    description = VALUES(description),
    start_at = VALUES(start_at),
    end_at = VALUES(end_at),
    location_note = VALUES(location_note),
    status = VALUES(status),
    submitted_at = VALUES(submitted_at),
    published_at = VALUES(published_at);

INSERT INTO engagement_events (event_type_code, user_id, session_key, event_id, metadata, occurred_at)
WITH RECURSIVE slot_series AS (
    SELECT 0 AS slot_index
    UNION ALL
    SELECT slot_index + 1 FROM slot_series WHERE slot_index < 34
),
ranked_events AS (
    SELECT
        event.id AS event_id,
        event.slug,
        event.start_at,
        ROW_NUMBER() OVER (ORDER BY event.start_at ASC, event.id ASC) AS event_rank
    FROM events event
    WHERE event.slug LIKE CONCAT('uc58-demo-%-', DATE_FORMAT(CURRENT_DATE, '%Y%m'))
),
visit_plan AS (
    SELECT
        event_id,
        slug,
        start_at,
        event_rank,
        CASE event_rank
            WHEN 1 THEN 30
            WHEN 2 THEN 24
            WHEN 3 THEN 34
            WHEN 4 THEN 18
            ELSE 14
        END AS planned_views
    FROM ranked_events
)
SELECT
    'VIEW',
    CASE WHEN MOD(slot.slot_index + plan.event_rank, 3) = 0 THEN @uc58_tourist_id ELSE NULL END,
    CONCAT('uc58-demo-', DATE_FORMAT(CURRENT_DATE, '%Y%m'), '-', plan.event_id, '-', LPAD(slot.slot_index, 2, '0')),
    plan.event_id,
    JSON_OBJECT('source', 'uc58-demo-statistics-seed', 'channel', CASE WHEN MOD(slot.slot_index, 2) = 0 THEN 'event-page' ELSE 'calendar' END),
    TIMESTAMP(DATE(plan.start_at), MAKETIME(8 + MOD(slot.slot_index, 10), MOD(slot.slot_index * 7, 60), 0))
FROM visit_plan plan
JOIN slot_series slot ON slot.slot_index < plan.planned_views
WHERE NOT EXISTS (
    SELECT 1
    FROM engagement_events existing
    WHERE existing.event_type_code = 'VIEW'
      AND existing.session_key LIKE CONCAT('uc58-demo-', DATE_FORMAT(CURRENT_DATE, '%Y%m'), '-%')
);
