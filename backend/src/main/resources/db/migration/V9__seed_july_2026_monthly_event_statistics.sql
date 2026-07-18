-- Fixed July 2026 UC-58 demo data. This keeps the default monthly event
-- analytics page populated for the project presentation date instead of
-- depending only on the database server's CURRENT_DATE at migration time.
SET NAMES utf8mb4;

INSERT INTO engagement_event_types (event_type_code, event_type_name, description)
VALUES ('VIEW', 'View', 'User views a supported public content target')
ON DUPLICATE KEY UPDATE
    event_type_name = VALUES(event_type_name),
    description = VALUES(description),
    is_active = TRUE;

SET @uc58_july_author_id = (SELECT id FROM users WHERE email = 'moderator@gmail.com' LIMIT 1);
SET @uc58_july_tourist_id = (SELECT id FROM users WHERE email = 'tourist@gmail.com' LIMIT 1);
SET @uc58_july_place_id = (
    SELECT id FROM places
    WHERE status = 'PUBLISHED'
    ORDER BY CASE WHEN slug = 'demo-thanh-co-son-tay' THEN 0 ELSE 1 END, id
    LIMIT 1
);

INSERT INTO events (
    place_id, created_by_user_id, updated_by_user_id, title, slug, description,
    start_at, end_at, location_note, status, submitted_at, published_at
)
SELECT
    @uc58_july_place_id,
    @uc58_july_author_id,
    @uc58_july_author_id,
    seed.title,
    seed.slug,
    seed.description,
    seed.start_at,
    seed.end_at,
    seed.location_note,
    'PUBLISHED',
    DATE_SUB(seed.start_at, INTERVAL 14 DAY),
    DATE_SUB(seed.start_at, INTERVAL 10 DAY)
FROM (
    SELECT 'Đêm văn hóa Thành cổ Sơn Tây' AS title, 'uc58-july-dem-van-hoa-thanh-co-202607' AS slug,
           'Biểu diễn nghệ thuật truyền thống phục vụ du khách mùa hè.' AS description,
           TIMESTAMP('2026-07-02', '19:00:00') AS start_at, TIMESTAMP('2026-07-02', '22:00:00') AS end_at,
           'Thành cổ Sơn Tây' AS location_note
    UNION ALL SELECT 'Tuần lễ ảnh Sơn Tây xưa và nay', 'uc58-july-tuan-le-anh-son-tay-202607',
           'Triển lãm ảnh hỗ trợ báo cáo truyền thông du lịch theo tháng.',
           TIMESTAMP('2026-07-04', '09:00:00'), TIMESTAMP('2026-07-06', '17:00:00'), 'Nhà truyền thống Sơn Tây'
    UNION ALL SELECT 'Workshop làm chè lam Đường Lâm', 'uc58-july-workshop-che-lam-202607',
           'Hoạt động trải nghiệm ẩm thực địa phương cho khách tham quan.',
           TIMESTAMP('2026-07-07', '08:30:00'), TIMESTAMP('2026-07-07', '11:30:00'), 'Đường Lâm'
    UNION ALL SELECT 'Đạp xe di sản ngoại thành', 'uc58-july-dap-xe-di-san-202607',
           'Tour trải nghiệm kết nối Thành cổ, làng cổ và vùng ven.',
           TIMESTAMP('2026-07-08', '06:00:00'), TIMESTAMP('2026-07-08', '10:00:00'), 'Trung tâm Sơn Tây'
    UNION ALL SELECT 'Đêm nhạc dân gian xứ Đoài', 'uc58-july-dem-nhac-dan-gian-202607',
           'Chương trình biểu diễn phục vụ khách cuối tuần.',
           TIMESTAMP('2026-07-10', '19:30:00'), TIMESTAMP('2026-07-10', '21:30:00'), 'Phố đi bộ Sơn Tây'
    UNION ALL SELECT 'Tọa đàm bảo tồn đá ong', 'uc58-july-toa-dam-da-ong-202607',
           'Tọa đàm chuyên đề cho nhóm quản lý di sản và hướng dẫn viên.',
           TIMESTAMP('2026-07-12', '14:00:00'), TIMESTAMP('2026-07-12', '16:30:00'), 'Làng cổ Đường Lâm'
    UNION ALL SELECT 'Phiên chợ sản vật xứ Đoài', 'uc58-july-phien-cho-san-vat-202607',
           'Kết nối hộ kinh doanh địa phương với khách du lịch mùa vụ.',
           TIMESTAMP('2026-07-14', '07:00:00'), TIMESTAMP('2026-07-14', '12:00:00'), 'Sân đình Mông Phụ'
    UNION ALL SELECT 'Tour kể chuyện Thành cổ', 'uc58-july-tour-ke-chuyen-thanh-co-202607',
           'Hoạt động kể chuyện lịch sử dành cho gia đình và học sinh.',
           TIMESTAMP('2026-07-16', '18:30:00'), TIMESTAMP('2026-07-16', '20:30:00'), 'Thành cổ Sơn Tây'
    UNION ALL SELECT 'Ngày hội du lịch xanh Sơn Tây', 'uc58-july-du-lich-xanh-202607',
           'Sự kiện đang diễn ra để kiểm thử trạng thái hoạt động trong tháng.',
           TIMESTAMP('2026-07-18', '07:30:00'), TIMESTAMP('2026-07-18', '23:00:00'), 'Sơn Tây, Hà Nội'
    UNION ALL SELECT 'Lớp hướng dẫn viên nhí', 'uc58-july-huong-dan-vien-nhi-202607',
           'Chương trình giáo dục di sản cho học sinh địa phương.',
           TIMESTAMP('2026-07-20', '08:00:00'), TIMESTAMP('2026-07-20', '10:30:00'), 'Bảo tàng Sơn Tây'
    UNION ALL SELECT 'Không gian ẩm thực phố cổ', 'uc58-july-khong-gian-am-thuc-202607',
           'Sự kiện ẩm thực buổi tối kết hợp doanh nghiệp địa phương.',
           TIMESTAMP('2026-07-22', '17:00:00'), TIMESTAMP('2026-07-22', '21:30:00'), 'Phố đi bộ Sơn Tây'
    UNION ALL SELECT 'Trại sáng tác ký họa di sản', 'uc58-july-ky-hoa-di-san-202607',
           'Hoạt động nghệ thuật ngoài trời tại các điểm di tích.',
           TIMESTAMP('2026-07-24', '07:30:00'), TIMESTAMP('2026-07-24', '11:30:00'), 'Đường Lâm'
    UNION ALL SELECT 'Ngày hội thể thao ven hồ Đồng Mô', 'uc58-july-the-thao-dong-mo-202607',
           'Sự kiện sắp diễn ra phục vụ báo cáo tác động du lịch mùa hè.',
           TIMESTAMP('2026-07-26', '06:00:00'), TIMESTAMP('2026-07-26', '11:00:00'), 'Đồng Mô'
    UNION ALL SELECT 'Đêm hội ánh sáng xứ Đoài', 'uc58-july-dem-hoi-anh-sang-202607',
           'Chương trình cuối tháng có lượng quan tâm cao trên lịch sự kiện.',
           TIMESTAMP('2026-07-28', '19:00:00'), TIMESTAMP('2026-07-28', '22:00:00'), 'Thành cổ Sơn Tây'
    UNION ALL SELECT 'Hội nghị xúc tiến du lịch cộng đồng', 'uc58-july-xuc-tien-du-lich-202607',
           'Sự kiện tổng kết tháng dành cho đối tác và cơ sở dịch vụ.',
           TIMESTAMP('2026-07-30', '08:00:00'), TIMESTAMP('2026-07-30', '11:30:00'), 'Trung tâm văn hóa Sơn Tây'
) seed
WHERE @uc58_july_place_id IS NOT NULL
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
    SELECT slot_index + 1 FROM slot_series WHERE slot_index < 72
),
ranked_events AS (
    SELECT
        event.id AS event_id,
        event.start_at,
        ROW_NUMBER() OVER (ORDER BY event.start_at ASC, event.id ASC) AS event_rank
    FROM events event
    WHERE event.slug LIKE 'uc58-july-%-202607'
),
visit_plan AS (
    SELECT
        event_id,
        start_at,
        event_rank,
        CASE
            WHEN event_rank IN (1, 5, 9, 14) THEN 64
            WHEN event_rank IN (2, 7, 11, 15) THEN 52
            WHEN event_rank IN (3, 6, 10, 13) THEN 40
            ELSE 30
        END AS planned_views
    FROM ranked_events
)
SELECT
    'VIEW',
    CASE WHEN MOD(slot.slot_index + plan.event_rank, 3) = 0 THEN @uc58_july_tourist_id ELSE NULL END,
    CONCAT('uc58-july-202607-', plan.event_id, '-', LPAD(slot.slot_index, 2, '0')),
    plan.event_id,
    JSON_OBJECT(
        'source', 'uc58-july-statistics-seed',
        'channel', CASE
            WHEN MOD(slot.slot_index, 4) = 0 THEN 'calendar'
            WHEN MOD(slot.slot_index, 4) = 1 THEN 'event-page'
            WHEN MOD(slot.slot_index, 4) = 2 THEN 'article'
            ELSE 'social'
        END
    ),
    TIMESTAMP(
        LEAST(DATE(plan.start_at), DATE('2026-07-18')),
        MAKETIME(7 + MOD(slot.slot_index, 13), MOD(plan.event_rank * 5 + slot.slot_index * 7, 60), 0)
    )
FROM visit_plan plan
JOIN slot_series slot ON slot.slot_index < plan.planned_views
WHERE NOT EXISTS (
    SELECT 1
    FROM engagement_events existing
    WHERE existing.event_type_code = 'VIEW'
      AND existing.session_key LIKE 'uc58-july-202607-%'
);
