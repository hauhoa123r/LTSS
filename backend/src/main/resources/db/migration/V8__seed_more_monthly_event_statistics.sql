-- Additional UC-58 demo data so monthly event analytics has enough density for
-- reports, ranking, period breakdowns and daily engagement charts.
INSERT INTO engagement_event_types (event_type_code, event_type_name, description)
VALUES ('VIEW', 'View', 'User views a supported public content target')
ON DUPLICATE KEY UPDATE
    event_type_name = VALUES(event_type_name),
    description = VALUES(description),
    is_active = TRUE;

SET @uc58_extra_author_id = (SELECT id FROM users WHERE email = 'moderator@gmail.com' LIMIT 1);
SET @uc58_extra_tourist_id = (SELECT id FROM users WHERE email = 'tourist@gmail.com' LIMIT 1);
SET @uc58_extra_place_id = (
    SELECT id FROM places
    WHERE status = 'PUBLISHED'
    ORDER BY CASE WHEN slug = 'demo-thanh-co-son-tay' THEN 0 ELSE 1 END, id
    LIMIT 1
);
SET @uc58_extra_month_start = DATE_FORMAT(CURRENT_DATE, '%Y-%m-01');
SET @uc58_extra_month_end = LAST_DAY(CURRENT_DATE);

INSERT INTO events (
    place_id, created_by_user_id, updated_by_user_id, title, slug, description,
    start_at, end_at, location_note, status, submitted_at, published_at
)
SELECT
    @uc58_extra_place_id,
    @uc58_extra_author_id,
    @uc58_extra_author_id,
    seed.title,
    CONCAT(seed.slug_prefix, '-', DATE_FORMAT(CURRENT_DATE, '%Y%m')),
    seed.description,
    TIMESTAMP(seed.start_day, seed.start_time),
    TIMESTAMP(seed.end_day, seed.end_time),
    seed.location_note,
    'PUBLISHED',
    TIMESTAMP(DATE_SUB(seed.start_day, INTERVAL 18 DAY), '08:00:00'),
    TIMESTAMP(DATE_SUB(seed.start_day, INTERVAL 12 DAY), '08:00:00')
FROM (
    SELECT
        'Tuần lễ ảnh Sơn Tây xưa và nay' AS title,
        'uc58-extra-tuan-le-anh-son-tay' AS slug_prefix,
        'Triển lãm ảnh phục vụ báo cáo truyền thông du lịch theo tháng.' AS description,
        LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 1 DAY), @uc58_extra_month_end) AS start_day,
        LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 3 DAY), @uc58_extra_month_end) AS end_day,
        '09:00:00' AS start_time,
        '17:00:00' AS end_time,
        'Nhà truyền thống Sơn Tây' AS location_note
    UNION ALL
    SELECT 'Workshop làm chè lam Đường Lâm', 'uc58-extra-workshop-che-lam',
           'Hoạt động trải nghiệm ẩm thực địa phương cho khách tham quan.',
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 4 DAY), @uc58_extra_month_end),
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 4 DAY), @uc58_extra_month_end),
           '08:30:00', '11:30:00', 'Đường Lâm'
    UNION ALL
    SELECT 'Đạp xe di sản ngoại thành', 'uc58-extra-dap-xe-di-san',
           'Tour trải nghiệm kết nối Thành cổ, làng cổ và vùng ven.',
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 6 DAY), @uc58_extra_month_end),
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 6 DAY), @uc58_extra_month_end),
           '06:00:00', '10:00:00', 'Trung tâm Sơn Tây'
    UNION ALL
    SELECT 'Đêm nhạc dân gian xứ Đoài', 'uc58-extra-dem-nhac-dan-gian',
           'Chương trình biểu diễn phục vụ khách cuối tuần.',
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 8 DAY), @uc58_extra_month_end),
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 8 DAY), @uc58_extra_month_end),
           '19:30:00', '21:30:00', 'Phố đi bộ Sơn Tây'
    UNION ALL
    SELECT 'Tọa đàm bảo tồn đá ong', 'uc58-extra-toa-dam-da-ong',
           'Tọa đàm chuyên đề cho nhóm quản lý di sản và hướng dẫn viên.',
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 11 DAY), @uc58_extra_month_end),
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 11 DAY), @uc58_extra_month_end),
           '14:00:00', '16:30:00', 'Làng cổ Đường Lâm'
    UNION ALL
    SELECT 'Phiên chợ sản vật xứ Đoài', 'uc58-extra-phien-cho-san-vat',
           'Kết nối hộ kinh doanh địa phương với khách du lịch mùa vụ.',
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 13 DAY), @uc58_extra_month_end),
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 13 DAY), @uc58_extra_month_end),
           '07:00:00', '12:00:00', 'Sân đình Mông Phụ'
    UNION ALL
    SELECT 'Tour kể chuyện Thành cổ', 'uc58-extra-tour-ke-chuyen-thanh-co',
           'Hoạt động kể chuyện lịch sử dành cho gia đình và học sinh.',
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 15 DAY), @uc58_extra_month_end),
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 15 DAY), @uc58_extra_month_end),
           '18:30:00', '20:30:00', 'Thành cổ Sơn Tây'
    UNION ALL
    SELECT 'Ngày hội du lịch xanh Sơn Tây', 'uc58-extra-du-lich-xanh',
           'Sự kiện đang diễn ra để kiểm thử trạng thái hoạt động trong tháng.',
           CURRENT_DATE, CURRENT_DATE,
           '07:30:00', '23:00:00', 'Sơn Tây, Hà Nội'
    UNION ALL
    SELECT 'Lớp hướng dẫn viên nhí', 'uc58-extra-huong-dan-vien-nhi',
           'Chương trình giáo dục di sản cho học sinh địa phương.',
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 19 DAY), @uc58_extra_month_end),
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 19 DAY), @uc58_extra_month_end),
           '08:00:00', '10:30:00', 'Bảo tàng Sơn Tây'
    UNION ALL
    SELECT 'Không gian ẩm thực phố cổ', 'uc58-extra-khong-gian-am-thuc',
           'Sự kiện ẩm thực buổi tối kết hợp doanh nghiệp địa phương.',
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 21 DAY), @uc58_extra_month_end),
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 21 DAY), @uc58_extra_month_end),
           '17:00:00', '21:30:00', 'Phố đi bộ Sơn Tây'
    UNION ALL
    SELECT 'Trại sáng tác ký họa di sản', 'uc58-extra-ky-hoa-di-san',
           'Hoạt động nghệ thuật ngoài trời tại các điểm di tích.',
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 23 DAY), @uc58_extra_month_end),
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 23 DAY), @uc58_extra_month_end),
           '07:30:00', '11:30:00', 'Đường Lâm'
    UNION ALL
    SELECT 'Ngày hội thể thao ven hồ Đồng Mô', 'uc58-extra-the-thao-dong-mo',
           'Sự kiện sắp diễn ra phục vụ báo cáo tác động du lịch mùa hè.',
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 25 DAY), @uc58_extra_month_end),
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 25 DAY), @uc58_extra_month_end),
           '06:00:00', '11:00:00', 'Đồng Mô'
    UNION ALL
    SELECT 'Đêm hội ánh sáng xứ Đoài', 'uc58-extra-dem-hoi-anh-sang',
           'Chương trình cuối tháng có lượng quan tâm cao trên lịch sự kiện.',
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 27 DAY), @uc58_extra_month_end),
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 27 DAY), @uc58_extra_month_end),
           '19:00:00', '22:00:00', 'Thành cổ Sơn Tây'
    UNION ALL
    SELECT 'Hội nghị xúc tiến du lịch cộng đồng', 'uc58-extra-xuc-tien-du-lich',
           'Sự kiện tổng kết tháng dành cho đối tác và cơ sở dịch vụ.',
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 29 DAY), @uc58_extra_month_end),
           LEAST(DATE_ADD(@uc58_extra_month_start, INTERVAL 29 DAY), @uc58_extra_month_end),
           '08:00:00', '11:30:00', 'Trung tâm văn hóa Sơn Tây'
) seed
WHERE @uc58_extra_place_id IS NOT NULL
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
    SELECT slot_index + 1 FROM slot_series WHERE slot_index < 64
),
ranked_events AS (
    SELECT
        event.id AS event_id,
        event.slug,
        event.start_at,
        ROW_NUMBER() OVER (ORDER BY event.start_at ASC, event.id ASC) AS event_rank
    FROM events event
    WHERE event.slug LIKE CONCAT('uc58-extra-%-', DATE_FORMAT(CURRENT_DATE, '%Y%m'))
),
visit_plan AS (
    SELECT
        event_id,
        slug,
        start_at,
        event_rank,
        CASE
            WHEN event_rank IN (1, 4, 8, 13) THEN 58
            WHEN event_rank IN (2, 6, 10) THEN 44
            WHEN event_rank IN (3, 7, 12, 14) THEN 36
            ELSE 26
        END AS planned_views
    FROM ranked_events
)
SELECT
    'VIEW',
    CASE WHEN MOD(slot.slot_index + plan.event_rank, 3) = 0 THEN @uc58_extra_tourist_id ELSE NULL END,
    CONCAT('uc58-extra-', DATE_FORMAT(CURRENT_DATE, '%Y%m'), '-', plan.event_id, '-', LPAD(slot.slot_index, 2, '0')),
    plan.event_id,
    JSON_OBJECT(
        'source', 'uc58-extra-statistics-seed',
        'channel', CASE
            WHEN MOD(slot.slot_index, 4) = 0 THEN 'calendar'
            WHEN MOD(slot.slot_index, 4) = 1 THEN 'event-page'
            WHEN MOD(slot.slot_index, 4) = 2 THEN 'article'
            ELSE 'social'
        END
    ),
    TIMESTAMP(
        LEAST(DATE(plan.start_at), CURRENT_DATE),
        MAKETIME(7 + MOD(slot.slot_index, 13), MOD(plan.event_rank * 5 + slot.slot_index * 7, 60), 0)
    )
FROM visit_plan plan
JOIN slot_series slot ON slot.slot_index < plan.planned_views
WHERE NOT EXISTS (
    SELECT 1
    FROM engagement_events existing
    WHERE existing.event_type_code = 'VIEW'
      AND existing.session_key LIKE CONCAT('uc58-extra-', DATE_FORMAT(CURRENT_DATE, '%Y%m'), '-%')
);
