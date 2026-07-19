-- Demo data for local development and UI testing.
-- Password for all demo users: Test@123
-- Keep foundation data in V2; this file intentionally contains sample/domain data.

SET NAMES utf8mb4;
SET time_zone = '+07:00';

SET @demo_password_hash = '$2a$10$VTSumtVCSKxBHlKQA4F3U.JpuitXtEh8iCgrU17IFBqlmiicZeLt6';

-- ---------------------------------------------------------------------------
-- RBAC permissions
-- ---------------------------------------------------------------------------
INSERT INTO permissions (permission_code, permission_name, description)
VALUES
    ('PLACE_READ', 'Read places', 'View published tourism places'),
    ('PLACE_MANAGE', 'Manage places', 'Create and moderate tourism places'),
    ('CONTENT_MODERATE', 'Moderate content', 'Approve or reject submitted content'),
    ('BUSINESS_MANAGE', 'Manage business profile', 'Manage business profile, posts and promotions'),
    ('SYSTEM_ADMIN', 'System administration', 'Manage system-level configuration')
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    description = VALUES(description);

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.permission_code IN ('PLACE_READ')
WHERE role.role_code = 'TOURIST';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.permission_code IN ('PLACE_READ', 'BUSINESS_MANAGE')
WHERE role.role_code = 'BUSINESS_OWNER';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.permission_code IN ('PLACE_READ', 'PLACE_MANAGE')
WHERE role.role_code = 'RELIC_MANAGER';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.permission_code IN ('PLACE_READ', 'PLACE_MANAGE', 'CONTENT_MODERATE')
WHERE role.role_code = 'MODERATOR';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.permission_code IN ('PLACE_READ', 'PLACE_MANAGE', 'CONTENT_MODERATE', 'BUSINESS_MANAGE', 'SYSTEM_ADMIN')
WHERE role.role_code = 'ADMINISTRATOR';

-- ---------------------------------------------------------------------------
-- Users and account data
-- ---------------------------------------------------------------------------
INSERT INTO users (
    full_name, display_name, email, password_hash, phone, avatar_url, address,
    status, email_verified_at, last_login_at, password_changed_at, policy_version, policy_accepted_at
)
VALUES
    ('Nguyen Minh An', 'Minh An', 'tourist@gmail.com', @demo_password_hash, '0900000001',
     'https://cdn.ltss.local/demo/users/tourist.jpg', 'Pho Quang Trung, Son Tay, Ha Noi',
     'ACTIVE', '2026-01-01 08:00:00', '2026-07-01 08:00:00', '2026-01-01 08:00:00', '2026.1', '2026-01-01 08:00:00'),
    ('Tran Thu Ha', 'Thu Ha', 'owner@gmail.com', @demo_password_hash, '0900000002',
     'https://cdn.ltss.local/demo/users/owner.jpg', 'Lang co Duong Lam, Son Tay, Ha Noi',
     'ACTIVE', '2026-01-01 08:00:00', '2026-07-01 08:10:00', '2026-01-01 08:00:00', '2026.1', '2026-01-01 08:00:00'),
    ('Le Quang Huy', 'Quang Huy', 'moderator@gmail.com', @demo_password_hash, '0900000003',
     'https://cdn.ltss.local/demo/users/moderator.jpg', 'Thanh co Son Tay, Ha Noi',
     'ACTIVE', '2026-01-01 08:00:00', '2026-07-01 08:20:00', '2026-01-01 08:00:00', '2026.1', '2026-01-01 08:00:00'),
    ('Pham Ngoc Lan', 'Ngoc Lan', 'relic@gmail.com', @demo_password_hash, '0900000004',
     'https://cdn.ltss.local/demo/users/relic.jpg', 'Ban quan ly di tich Son Tay, Ha Noi',
     'ACTIVE', '2026-01-01 08:00:00', '2026-07-01 08:30:00', '2026-01-01 08:00:00', '2026.1', '2026-01-01 08:00:00'),
    ('Hoang Anh Khoa', 'Admin Khoa', 'admin@gmail.com', @demo_password_hash, '0900000005',
     'https://cdn.ltss.local/demo/users/admin.jpg', 'Trung tam dieu hanh LTSS, Son Tay, Ha Noi',
     'ACTIVE', '2026-01-01 08:00:00', '2026-07-01 08:40:00', '2026-01-01 08:00:00', '2026.1', '2026-01-01 08:00:00')
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    display_name = VALUES(display_name),
    password_hash = VALUES(password_hash),
    phone = VALUES(phone),
    avatar_url = VALUES(avatar_url),
    address = VALUES(address),
    status = VALUES(status),
    email_verified_at = VALUES(email_verified_at),
    last_login_at = VALUES(last_login_at),
    password_changed_at = VALUES(password_changed_at),
    policy_version = VALUES(policy_version),
    policy_accepted_at = VALUES(policy_accepted_at);

SET @tourist_user_id = (SELECT id FROM users WHERE email = 'tourist@gmail.com');
SET @owner_user_id = (SELECT id FROM users WHERE email = 'owner@gmail.com');
SET @moderator_user_id = (SELECT id FROM users WHERE email = 'moderator@gmail.com');
SET @relic_user_id = (SELECT id FROM users WHERE email = 'relic@gmail.com');
SET @admin_user_id = (SELECT id FROM users WHERE email = 'admin@gmail.com');

INSERT IGNORE INTO user_roles (user_id, role_id, is_active, assigned_by_user_id)
SELECT @tourist_user_id, id, TRUE, @admin_user_id FROM roles WHERE role_code = 'TOURIST';
INSERT IGNORE INTO user_roles (user_id, role_id, is_active, assigned_by_user_id)
SELECT @owner_user_id, id, TRUE, @admin_user_id FROM roles WHERE role_code = 'BUSINESS_OWNER';
INSERT IGNORE INTO user_roles (user_id, role_id, is_active, assigned_by_user_id)
SELECT @moderator_user_id, id, TRUE, @admin_user_id FROM roles WHERE role_code = 'MODERATOR';
INSERT IGNORE INTO user_roles (user_id, role_id, is_active, assigned_by_user_id)
SELECT @relic_user_id, id, TRUE, @admin_user_id FROM roles WHERE role_code = 'RELIC_MANAGER';
INSERT IGNORE INTO user_roles (user_id, role_id, is_active, assigned_by_user_id)
SELECT @admin_user_id, id, TRUE, @admin_user_id FROM roles WHERE role_code = 'ADMINISTRATOR';

INSERT INTO password_history (user_id, password_hash, change_reason, created_at)
SELECT @tourist_user_id, @demo_password_hash, 'REGISTRATION', '2026-01-01 08:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM password_history
    WHERE user_id = @tourist_user_id
      AND change_reason = 'REGISTRATION'
      AND created_at = '2026-01-01 08:00:00'
);

INSERT INTO account_tokens (user_id, token_type, token_hash, expires_at, used_at, created_ip, created_at)
VALUES (
    @tourist_user_id, 'EMAIL_VERIFICATION', SHA2('ltss-demo-email-verification-token', 256),
    '2026-12-31 23:59:59', '2026-01-01 08:05:00', '127.0.0.1', '2026-01-01 08:00:00'
)
ON DUPLICATE KEY UPDATE used_at = VALUES(used_at), revoked_at = NULL;

INSERT INTO notifications (recipient_user_id, title, message, notification_type, action_url, is_read, read_at)
SELECT @tourist_user_id, 'Chào mừng đến LTSS', 'Tài khoản demo đã sẵn sàng để khám phá dữ liệu mẫu Sơn Tây.',
       'SYSTEM', '/places/demo-thanh-co-son-tay', FALSE, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM notifications
    WHERE recipient_user_id = @tourist_user_id
      AND title = 'Chào mừng đến LTSS'
      AND action_url = '/places/demo-thanh-co-son-tay'
);

INSERT INTO audit_logs (actor_user_id, action_code, entity_type, entity_id, old_values, new_values, ip_address, user_agent, request_id, created_at)
SELECT @admin_user_id, 'DEMO_SEED', 'DATABASE', NULL, NULL, JSON_OBJECT('migration', 'V3__seed_demo_data.sql'),
       '127.0.0.1', 'LTSS demo seed', 'demo-seed-0001', '2026-01-01 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM audit_logs WHERE request_id = 'demo-seed-0001');

INSERT INTO search_history (user_id, keyword, normalized_keyword, searched_at)
VALUES (@tourist_user_id, 'Thành cổ Sơn Tây', 'thanh-co-son-tay', '2026-07-01 09:00:00')
ON DUPLICATE KEY UPDATE searched_at = VALUES(searched_at);

INSERT INTO prohibited_terms (term, normalized_term, severity, is_active, created_by_user_id)
VALUES ('spam đặt tour', 'spam-dat-tour', 'WARN', TRUE, @moderator_user_id)
ON DUPLICATE KEY UPDATE term = VALUES(term), severity = VALUES(severity), is_active = VALUES(is_active);

-- ---------------------------------------------------------------------------
-- Places, business, events and articles
-- ---------------------------------------------------------------------------
INSERT INTO place_categories (category_name, slug, description, marker_icon_key, created_by_user_id, updated_by_user_id)
VALUES
    ('Demo Di tích lịch sử', 'demo-di-tich-lich-su', 'Danh mục demo cho di tích lịch sử Sơn Tây', 'relic', @relic_user_id, @relic_user_id),
    ('Demo Chùa', 'demo-chua', 'Danh mục demo cho chùa và điểm tâm linh', 'pagoda', @relic_user_id, @relic_user_id),
    ('Demo Nhà hàng', 'demo-nha-hang', 'Danh mục demo cho nhà hàng địa phương', 'restaurant', @owner_user_id, @owner_user_id)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    marker_icon_key = VALUES(marker_icon_key),
    updated_by_user_id = VALUES(updated_by_user_id),
    is_active = TRUE;

SET @relic_category_id = (SELECT id FROM place_categories WHERE slug = 'demo-di-tich-lich-su');
SET @pagoda_category_id = (SELECT id FROM place_categories WHERE slug = 'demo-chua');
SET @restaurant_category_id = (SELECT id FROM place_categories WHERE slug = 'demo-nha-hang');

INSERT INTO places (
    category_id, created_by_user_id, updated_by_user_id, name, slug, summary, description,
    address, latitude, longitude, opening_hours, entrance_fee, contact_phone,
    status, submitted_at, published_at
)
VALUES
    (@relic_category_id, @relic_user_id, @relic_user_id, 'Demo Thành cổ Sơn Tây', 'demo-thanh-co-son-tay',
     'Di tích quân sự cổ tại trung tâm Sơn Tây.',
     'Điểm demo dùng để kiểm thử màn hình địa điểm, media, review, quiz và moderation.',
     'Pho Hoang Dieu, Son Tay, Ha Noi', 21.1385000, 105.5056000, '07:00-17:30 hằng ngày', 0.00, '0900000101',
     'PUBLISHED', '2026-01-02 08:00:00', '2026-01-03 08:00:00'),
    (@pagoda_category_id, @relic_user_id, @relic_user_id, 'Demo Chùa Mía', 'demo-chua-mia',
     'Ngôi chùa cổ nổi tiếng ở Đường Lâm.',
     'Điểm demo dùng làm chặng phụ trong tour mẫu.',
     'Duong Lam, Son Tay, Ha Noi', 21.1587000, 105.4726000, '06:30-18:00 hằng ngày', 0.00, '0900000102',
     'PUBLISHED', '2026-01-02 08:00:00', '2026-01-03 08:00:00'),
    (@restaurant_category_id, @owner_user_id, @owner_user_id, 'Demo Bếp Làng Đường Lâm', 'demo-bep-lang-duong-lam',
     'Nhà hàng demo phục vụ món địa phương.',
     'Cơ sở demo dùng để kiểm thử business, bài đăng và khuyến mãi.',
     'Mong Phu, Duong Lam, Son Tay, Ha Noi', 21.1579000, 105.4735000, '08:00-21:30 hằng ngày', 0.00, '0900000103',
     'PUBLISHED', '2026-01-02 08:00:00', '2026-01-03 08:00:00')
ON DUPLICATE KEY UPDATE
    category_id = VALUES(category_id),
    updated_by_user_id = VALUES(updated_by_user_id),
    name = VALUES(name),
    summary = VALUES(summary),
    description = VALUES(description),
    address = VALUES(address),
    latitude = VALUES(latitude),
    longitude = VALUES(longitude),
    opening_hours = VALUES(opening_hours),
    entrance_fee = VALUES(entrance_fee),
    contact_phone = VALUES(contact_phone),
    status = VALUES(status),
    submitted_at = VALUES(submitted_at),
    published_at = VALUES(published_at);

SET @thanh_co_place_id = (SELECT id FROM places WHERE slug = 'demo-thanh-co-son-tay');
SET @chua_mia_place_id = (SELECT id FROM places WHERE slug = 'demo-chua-mia');
SET @restaurant_place_id = (SELECT id FROM places WHERE slug = 'demo-bep-lang-duong-lam');

INSERT INTO relic_details (place_id, historical_period, history, architecture, recognition_level, recognized_at, preservation_note)
VALUES
    (@thanh_co_place_id, 'Thời Nguyễn', 'Di tích gắn với lịch sử quân sự và đô thị cổ Sơn Tây.',
     'Kiến trúc thành cổ sử dụng đá ong, hào nước và cổng thành đặc trưng.', 'Quốc gia', '1994-10-16',
     'Cần bảo tồn vật liệu gốc và hướng dẫn khách tham quan văn minh.'),
    (@chua_mia_place_id, 'Thế kỷ XVII', 'Chùa cổ trong không gian văn hóa Đường Lâm.',
     'Không gian chùa Việt truyền thống với nhiều pho tượng cổ.', 'Quốc gia', '1964-04-29',
     'Cần kiểm soát lưu lượng khách vào các dịp lễ.')
ON DUPLICATE KEY UPDATE
    historical_period = VALUES(historical_period),
    history = VALUES(history),
    architecture = VALUES(architecture),
    recognition_level = VALUES(recognition_level),
    recognized_at = VALUES(recognized_at),
    preservation_note = VALUES(preservation_note);

INSERT INTO businesses (place_id, owner_user_id, registration_number, contact_email, website_url, status, approved_by_user_id, approved_at)
VALUES (@restaurant_place_id, @owner_user_id, 'LTSS-DEMO-BIZ-001', 'owner@gmail.com',
        'https://dulichsontay.local/demo-bep-lang-duong-lam', 'ACTIVE', @moderator_user_id, '2026-01-04 08:00:00')
ON DUPLICATE KEY UPDATE
    place_id = VALUES(place_id),
    contact_email = VALUES(contact_email),
    website_url = VALUES(website_url),
    status = VALUES(status),
    approved_by_user_id = VALUES(approved_by_user_id),
    approved_at = VALUES(approved_at);

SET @business_id = (SELECT id FROM businesses WHERE registration_number = 'LTSS-DEMO-BIZ-001');

INSERT INTO events (place_id, created_by_user_id, updated_by_user_id, title, slug, description, start_at, end_at, location_note, status, submitted_at, published_at)
VALUES (@thanh_co_place_id, @moderator_user_id, @moderator_user_id, 'Demo Tour đêm Thành cổ Sơn Tây',
        'demo-tour-dem-thanh-co-son-tay',
        'Chương trình demo giới thiệu lịch sử Thành cổ Sơn Tây vào buổi tối.',
        '2026-08-15 19:00:00', '2026-08-15 21:00:00', 'Thành cổ Sơn Tây, Hà Nội',
        'PUBLISHED', '2026-07-01 08:00:00', '2026-07-05 08:00:00')
ON DUPLICATE KEY UPDATE
    place_id = VALUES(place_id),
    description = VALUES(description),
    start_at = VALUES(start_at),
    end_at = VALUES(end_at),
    location_note = VALUES(location_note),
    status = VALUES(status),
    submitted_at = VALUES(submitted_at),
    published_at = VALUES(published_at);

SET @event_id = (SELECT id FROM events WHERE slug = 'demo-tour-dem-thanh-co-son-tay');

INSERT INTO article_categories (category_name, slug, description, created_by_user_id, updated_by_user_id)
VALUES ('Demo Kinh nghiệm du lịch', 'demo-kinh-nghiem-du-lich',
        'Bài viết demo cho lịch trình và kinh nghiệm tham quan Sơn Tây', @moderator_user_id, @moderator_user_id)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    updated_by_user_id = VALUES(updated_by_user_id),
    is_active = TRUE;

SET @article_category_id = (SELECT id FROM article_categories WHERE slug = 'demo-kinh-nghiem-du-lich');

INSERT INTO articles (category_id, place_id, event_id, author_user_id, updated_by_user_id, title, slug, summary, content, status, submitted_at, published_at)
VALUES (@article_category_id, @thanh_co_place_id, @event_id, @moderator_user_id, @moderator_user_id,
        'Demo một ngày khám phá Thành cổ Sơn Tây', 'demo-mot-ngay-kham-pha-thanh-co-son-tay',
        'Lịch trình demo cho khách lần đầu tới Sơn Tây.',
        'Du khách có thể bắt đầu từ Thành cổ Sơn Tây, tiếp tục ghé Đường Lâm và kết thúc bằng bữa tối với đặc sản địa phương. Nội dung này dùng cho dữ liệu demo.',
        'PUBLISHED', '2026-07-01 08:00:00', '2026-07-05 08:00:00')
ON DUPLICATE KEY UPDATE
    category_id = VALUES(category_id),
    place_id = VALUES(place_id),
    event_id = VALUES(event_id),
    updated_by_user_id = VALUES(updated_by_user_id),
    title = VALUES(title),
    summary = VALUES(summary),
    content = VALUES(content),
    status = VALUES(status),
    submitted_at = VALUES(submitted_at),
    published_at = VALUES(published_at);

SET @article_id = (SELECT id FROM articles WHERE slug = 'demo-mot-ngay-kham-pha-thanh-co-son-tay');

-- ---------------------------------------------------------------------------
-- Media and content extensions
-- ---------------------------------------------------------------------------
INSERT INTO media_assets (
    uploaded_by_user_id, media_type, storage_provider, storage_key, media_url,
    thumbnail_url, mime_type, file_size_bytes, width_px, height_px, duration_seconds, checksum_sha256
)
VALUES
    (@moderator_user_id, 'IMAGE', 'LTSS_CDN', 'demo/places/thanh-co-son-tay-cover.jpg',
     'https://commons.wikimedia.org/wiki/Special:FilePath/Th%C3%A0nh_c%E1%BB%95_S%C6%A1n_T%C3%A2y_2021.jpg?width=1280',
     'https://commons.wikimedia.org/wiki/Special:FilePath/Th%C3%A0nh_c%E1%BB%95_S%C6%A1n_T%C3%A2y_2021.jpg?width=500',
     'image/jpeg', 173015, 1280, 1279, NULL, SHA2('demo-places-thanh-co-son-tay-cover', 256)),
    (@moderator_user_id, 'PANORAMA_360', 'LTSS_CDN', 'demo/places/thanh-co-son-tay-panorama.jpg',
     'https://commons.wikimedia.org/wiki/Special:FilePath/Th%C3%A0nh_c%E1%BB%95_S%C6%A1n_T%C3%A2y_2021.jpg?width=1280',
     'https://commons.wikimedia.org/wiki/Special:FilePath/Th%C3%A0nh_c%E1%BB%95_S%C6%A1n_T%C3%A2y_2021.jpg?width=500',
     'image/jpeg', 173015, 1280, 640, NULL, SHA2('demo-places-thanh-co-son-tay-panorama', 256)),
    (@moderator_user_id, 'IMAGE', 'LTSS_CDN', 'demo/places/chua-mia-cover.jpg',
     'https://commons.wikimedia.org/wiki/Special:FilePath/Ch%C3%B9a_M%C3%ADa.jpg?width=1280',
     'https://commons.wikimedia.org/wiki/Special:FilePath/Ch%C3%B9a_M%C3%ADa.jpg?width=640',
     'image/jpeg', 651320, 1280, 951, NULL, SHA2('demo-places-chua-mia-cover', 256)),
    (@owner_user_id, 'IMAGE', 'LTSS_CDN', 'demo/business/bep-lang-cover.jpg',
     'https://commons.wikimedia.org/wiki/Special:FilePath/Street_Food_Life_%28Unsplash%29.jpg?width=1280',
     'https://commons.wikimedia.org/wiki/Special:FilePath/Street_Food_Life_%28Unsplash%29.jpg?width=640',
     'image/jpeg', 534132, 1280, 853, NULL, SHA2('demo-business-bep-lang-cover', 256))
ON DUPLICATE KEY UPDATE
    uploaded_by_user_id = VALUES(uploaded_by_user_id),
    media_url = VALUES(media_url),
    thumbnail_url = VALUES(thumbnail_url),
    mime_type = VALUES(mime_type),
    file_size_bytes = VALUES(file_size_bytes),
    width_px = VALUES(width_px),
    height_px = VALUES(height_px),
    duration_seconds = VALUES(duration_seconds),
    checksum_sha256 = VALUES(checksum_sha256),
    deleted_at = NULL;

SET @place_cover_media_id = (SELECT id FROM media_assets WHERE storage_key = 'demo/places/thanh-co-son-tay-cover.jpg');
SET @place_panorama_media_id = (SELECT id FROM media_assets WHERE storage_key = 'demo/places/thanh-co-son-tay-panorama.jpg');
SET @chua_mia_cover_media_id = (SELECT id FROM media_assets WHERE storage_key = 'demo/places/chua-mia-cover.jpg');
SET @business_cover_media_id = (SELECT id FROM media_assets WHERE storage_key = 'demo/business/bep-lang-cover.jpg');

INSERT INTO business_posts (business_id, created_by_user_id, updated_by_user_id, title, slug, summary, content, status, submitted_at, published_at)
VALUES (@business_id, @owner_user_id, @owner_user_id, 'Demo đặc sản gà Mía Đường Lâm',
        'demo-dac-san-ga-mia-duong-lam', 'Bài đăng demo cho nhà hàng địa phương.',
        'Bếp Làng Đường Lâm giới thiệu thực đơn demo gồm gà Mía, chè lam và các món ăn Xứ Đoài phục vụ khách tham quan.',
        'PUBLISHED', '2026-07-01 08:00:00', '2026-07-05 08:00:00')
ON DUPLICATE KEY UPDATE
    business_id = VALUES(business_id),
    updated_by_user_id = VALUES(updated_by_user_id),
    title = VALUES(title),
    summary = VALUES(summary),
    content = VALUES(content),
    status = VALUES(status),
    submitted_at = VALUES(submitted_at),
    published_at = VALUES(published_at);

SET @business_post_id = (SELECT id FROM business_posts WHERE slug = 'demo-dac-san-ga-mia-duong-lam');

INSERT INTO tags (tag_name, slug)
VALUES ('Demo Sơn Tây', 'demo-son-tay')
ON DUPLICATE KEY UPDATE tag_name = VALUES(tag_name);

SET @tag_id = (SELECT id FROM tags WHERE slug = 'demo-son-tay');

INSERT IGNORE INTO business_post_tags (business_post_id, tag_id)
VALUES (@business_post_id, @tag_id);

INSERT INTO promotions (
    business_id, created_by_user_id, updated_by_user_id, title, description,
    discount_type, discount_value, promo_code, start_at, end_at, status, submitted_at, published_at
)
VALUES (@business_id, @owner_user_id, @owner_user_id, 'Demo giảm 10% set ăn Xứ Đoài',
        'Khuyến mãi demo áp dụng cho nhóm khách đặt bàn trước trong tháng 8.',
        'PERCENTAGE', 10.00, 'LTSSDEMO10', '2026-08-01 00:00:00', '2026-08-31 23:59:59',
        'ACTIVE', '2026-07-01 08:00:00', '2026-07-05 08:00:00')
ON DUPLICATE KEY UPDATE
    business_id = VALUES(business_id),
    description = VALUES(description),
    discount_type = VALUES(discount_type),
    discount_value = VALUES(discount_value),
    status = VALUES(status),
    submitted_at = VALUES(submitted_at),
    published_at = VALUES(published_at);

SET @promotion_id = (SELECT id FROM promotions WHERE promo_code = 'LTSSDEMO10');

-- ---------------------------------------------------------------------------
-- Tours, quizzes, reviews and rewards
-- ---------------------------------------------------------------------------
INSERT INTO tours (
    owner_user_id, title, description, region, difficulty_level, estimated_distance_km,
    estimated_duration_minutes, status, visibility, submitted_at, published_at
)
SELECT @tourist_user_id, 'Demo một ngày ở Sơn Tây',
       'Tour demo đi qua Thành cổ Sơn Tây, Chùa Mía và bữa tối tại Đường Lâm.',
       'Sơn Tây', 'EASY', 12.50, 360, 'PUBLISHED', 'PUBLIC', '2026-07-01 08:00:00', '2026-07-05 08:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM tours
    WHERE owner_user_id = @tourist_user_id
      AND title = 'Demo một ngày ở Sơn Tây'
);

SET @tour_id = (SELECT id FROM tours WHERE owner_user_id = @tourist_user_id AND title = 'Demo một ngày ở Sơn Tây' ORDER BY id LIMIT 1);

INSERT INTO tour_items (tour_id, place_id, visit_order, planned_start_at, duration_minutes, transport_method, note)
VALUES
    (@tour_id, @thanh_co_place_id, 1, '2026-08-20 08:00:00', 90, 'WALK', 'Bắt đầu tại cổng chính Thành cổ.'),
    (@tour_id, @chua_mia_place_id, 2, '2026-08-20 10:30:00', 60, 'CAR', 'Di chuyển sang Đường Lâm.'),
    (@tour_id, @restaurant_place_id, 3, '2026-08-20 12:00:00', 90, 'WALK', 'Ăn trưa tại nhà hàng demo.')
ON DUPLICATE KEY UPDATE
    planned_start_at = VALUES(planned_start_at),
    duration_minutes = VALUES(duration_minutes),
    transport_method = VALUES(transport_method),
    note = VALUES(note);

INSERT IGNORE INTO favorites (user_id, place_id)
VALUES (@tourist_user_id, @thanh_co_place_id);

INSERT INTO quizzes (
    place_id, created_by_user_id, updated_by_user_id, title, description,
    time_limit_seconds, passing_score_percent, status, submitted_at, published_at
)
SELECT @thanh_co_place_id, @moderator_user_id, @moderator_user_id, 'Demo quiz Thành cổ Sơn Tây',
       'Bộ câu hỏi demo về lịch sử Thành cổ Sơn Tây.', 300, 60.00, 'PUBLISHED',
       '2026-07-01 08:00:00', '2026-07-05 08:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM quizzes
    WHERE place_id = @thanh_co_place_id
      AND title = 'Demo quiz Thành cổ Sơn Tây'
);

SET @quiz_id = (SELECT id FROM quizzes WHERE place_id = @thanh_co_place_id AND title = 'Demo quiz Thành cổ Sơn Tây' ORDER BY id LIMIT 1);

INSERT INTO questions (quiz_id, content, explanation, display_order, points)
VALUES (@quiz_id, 'Thành cổ Sơn Tây nổi bật với vật liệu xây dựng nào?', 'Thành cổ Sơn Tây nổi tiếng với kiến trúc đá ong.', 1, 1.00)
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    explanation = VALUES(explanation),
    points = VALUES(points),
    is_active = TRUE,
    deleted_at = NULL;

SET @question_id = (SELECT id FROM questions WHERE quiz_id = @quiz_id AND display_order = 1);

INSERT INTO answers (question_id, content, is_correct, display_order)
VALUES
    (@question_id, 'Đá ong', TRUE, 1),
    (@question_id, 'Gạch men', FALSE, 2),
    (@question_id, 'Kính màu', FALSE, 3),
    (@question_id, 'Thép tấm', FALSE, 4)
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    is_correct = VALUES(is_correct),
    is_active = TRUE,
    deleted_at = NULL;

SET @correct_answer_id = (SELECT id FROM answers WHERE question_id = @question_id AND display_order = 1);

INSERT INTO badges (badge_code, badge_name, description, icon_url)
VALUES ('DEMO_SON_TAY_EXPLORER', 'Demo Người khám phá Sơn Tây',
        'Huy hiệu demo cho người hoàn thành quiz Thành cổ Sơn Tây.',
        'https://cdn.ltss.local/demo/badges/son-tay-explorer.png')
ON DUPLICATE KEY UPDATE
    badge_name = VALUES(badge_name),
    description = VALUES(description),
    icon_url = VALUES(icon_url),
    is_active = TRUE;

SET @badge_id = (SELECT id FROM badges WHERE badge_code = 'DEMO_SON_TAY_EXPLORER');

INSERT IGNORE INTO quiz_badges (quiz_id, badge_id, minimum_score_percent)
VALUES (@quiz_id, @badge_id, 60.00);

INSERT INTO quiz_attempts (
    quiz_id, user_id, status, randomization_seed, started_at, expires_at, submitted_at,
    score, total_points, score_percent, is_passed, location_verified_at, distance_to_place_meters, created_at
)
SELECT @quiz_id, @tourist_user_id, 'SUBMITTED', 'demo-seed-quiz-attempt-001',
       '2026-07-10 09:00:00', '2026-07-10 09:05:00', '2026-07-10 09:02:00',
       1.00, 1.00, 100.00, TRUE, '2026-07-10 09:01:00', 80.00, '2026-07-10 09:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM quiz_attempts
    WHERE quiz_id = @quiz_id AND user_id = @tourist_user_id AND randomization_seed = 'demo-seed-quiz-attempt-001'
);

SET @attempt_id = (
    SELECT id FROM quiz_attempts
    WHERE quiz_id = @quiz_id AND user_id = @tourist_user_id AND randomization_seed = 'demo-seed-quiz-attempt-001'
    ORDER BY id LIMIT 1
);

INSERT INTO quiz_attempt_answers (
    attempt_id, question_id, selected_answer_id, question_order, question_text_snapshot,
    selected_answer_text_snapshot, correct_answer_text_snapshot, explanation_snapshot,
    is_correct, awarded_points, answered_at
)
VALUES (
    @attempt_id, @question_id, @correct_answer_id, 1,
    'Thành cổ Sơn Tây nổi bật với vật liệu xây dựng nào?',
    'Đá ong', 'Đá ong', 'Thành cổ Sơn Tây nổi tiếng với kiến trúc đá ong.',
    TRUE, 1.00, '2026-07-10 09:02:00'
)
ON DUPLICATE KEY UPDATE
    selected_answer_id = VALUES(selected_answer_id),
    selected_answer_text_snapshot = VALUES(selected_answer_text_snapshot),
    is_correct = VALUES(is_correct),
    awarded_points = VALUES(awarded_points),
    answered_at = VALUES(answered_at);

INSERT IGNORE INTO user_badges (user_id, badge_id, awarded_by_quiz_id, awarded_attempt_id, awarded_at)
VALUES (@tourist_user_id, @badge_id, @quiz_id, @attempt_id, '2026-07-10 09:03:00');

INSERT INTO reviews (user_id, place_id, rating, comment, status, submitted_at, published_at)
VALUES (@tourist_user_id, @thanh_co_place_id, 5,
        'Không gian demo rất phù hợp để kiểm thử đánh giá địa điểm và luồng phản hồi.',
        'VISIBLE', '2026-07-10 09:00:00', '2026-07-10 10:00:00')
ON DUPLICATE KEY UPDATE
    rating = VALUES(rating),
    comment = VALUES(comment),
    status = VALUES(status),
    submitted_at = VALUES(submitted_at),
    published_at = VALUES(published_at),
    deleted_at = NULL;

SET @review_id = (SELECT id FROM reviews WHERE user_id = @tourist_user_id AND place_id = @thanh_co_place_id);

INSERT INTO review_replies (review_id, replied_by_user_id, content)
VALUES (@review_id, @owner_user_id, 'Cảm ơn bạn đã dùng dữ liệu demo để kiểm thử trải nghiệm tại Sơn Tây.')
ON DUPLICATE KEY UPDATE
    replied_by_user_id = VALUES(replied_by_user_id),
    content = VALUES(content);

-- ---------------------------------------------------------------------------
-- Engagement, moderation and media links
-- ---------------------------------------------------------------------------
INSERT INTO engagement_event_types (event_type_code, event_type_name, description)
VALUES
    ('PLACE_VIEW', 'Place view', 'User views a place detail page'),
    ('ARTICLE_VIEW', 'Article view', 'User views an article'),
    ('PROMOTION_CLICK', 'Promotion click', 'User clicks a promotion')
ON DUPLICATE KEY UPDATE
    event_type_name = VALUES(event_type_name),
    description = VALUES(description),
    is_active = TRUE;

INSERT INTO engagement_events (event_type_code, user_id, session_key, place_id, metadata, occurred_at)
SELECT 'PLACE_VIEW', @tourist_user_id, 'demo-session-001', @thanh_co_place_id,
       JSON_OBJECT('source', 'demo-seed'), '2026-07-10 09:05:00'
WHERE NOT EXISTS (SELECT 1 FROM engagement_events WHERE session_key = 'demo-session-001' AND event_type_code = 'PLACE_VIEW');

INSERT INTO engagement_events (event_type_code, user_id, session_key, article_id, metadata, occurred_at)
SELECT 'ARTICLE_VIEW', @tourist_user_id, 'demo-session-001', @article_id,
       JSON_OBJECT('source', 'demo-seed'), '2026-07-10 09:06:00'
WHERE NOT EXISTS (SELECT 1 FROM engagement_events WHERE session_key = 'demo-session-001' AND event_type_code = 'ARTICLE_VIEW');

INSERT INTO engagement_events (event_type_code, user_id, session_key, promotion_id, metadata, occurred_at)
SELECT 'PROMOTION_CLICK', @tourist_user_id, 'demo-session-001', @promotion_id,
       JSON_OBJECT('source', 'demo-seed'), '2026-07-10 09:07:00'
WHERE NOT EXISTS (SELECT 1 FROM engagement_events WHERE session_key = 'demo-session-001' AND event_type_code = 'PROMOTION_CLICK');

INSERT INTO moderation_records (
    submitted_by_user_id, moderator_user_id, place_id, status, decision,
    submission_note, decision_reason, submitted_at, resolved_at
)
SELECT @relic_user_id, @moderator_user_id, @thanh_co_place_id, 'RESOLVED', 'APPROVED',
       'Demo moderation case for place publishing.', NULL, '2026-07-01 08:00:00', '2026-07-01 09:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM moderation_records
    WHERE place_id = @thanh_co_place_id
      AND submission_note = 'Demo moderation case for place publishing.'
);

INSERT IGNORE INTO place_media (place_id, media_asset_id, usage_type, display_order, is_primary)
VALUES
    (@thanh_co_place_id, @place_cover_media_id, 'COVER', 0, TRUE),
    (@thanh_co_place_id, @place_panorama_media_id, 'PANORAMA', 1, FALSE),
    (@chua_mia_place_id, @chua_mia_cover_media_id, 'COVER', 0, TRUE),
    (@restaurant_place_id, @business_cover_media_id, 'COVER', 0, TRUE);

INSERT IGNORE INTO event_media (event_id, media_asset_id, usage_type, display_order, is_primary)
VALUES (@event_id, @place_cover_media_id, 'COVER', 0, TRUE);

INSERT IGNORE INTO article_media (article_id, media_asset_id, usage_type, display_order, is_primary)
VALUES (@article_id, @place_cover_media_id, 'COVER', 0, TRUE);

INSERT IGNORE INTO business_post_media (business_post_id, media_asset_id, usage_type, display_order, is_primary)
VALUES (@business_post_id, @business_cover_media_id, 'COVER', 0, TRUE);

INSERT IGNORE INTO promotion_media (promotion_id, media_asset_id, usage_type, display_order, is_primary)
VALUES (@promotion_id, @business_cover_media_id, 'COVER', 0, TRUE);

INSERT IGNORE INTO tour_media (tour_id, media_asset_id, usage_type, display_order, is_primary)
VALUES (@tour_id, @place_cover_media_id, 'COVER', 0, TRUE);

INSERT IGNORE INTO review_media (review_id, media_asset_id, display_order)
VALUES (@review_id, @place_cover_media_id, 1);

INSERT IGNORE INTO quiz_media (quiz_id, media_asset_id, usage_type, display_order, is_primary)
VALUES (@quiz_id, @place_cover_media_id, 'COVER', 0, TRUE);

INSERT INTO panorama_hotspots (
    source_media_asset_id, target_media_asset_id, hotspot_type, yaw_degrees,
    pitch_degrees, label, description, display_order, is_active
)
VALUES (
    @place_panorama_media_id, NULL, 'INFO', 15.0000, 3.0000,
    'Cổng Thành cổ', 'Hotspot demo giới thiệu cổng chính Thành cổ Sơn Tây.', 0, TRUE
)
ON DUPLICATE KEY UPDATE
    hotspot_type = VALUES(hotspot_type),
    yaw_degrees = VALUES(yaw_degrees),
    pitch_degrees = VALUES(pitch_degrees),
    label = VALUES(label),
    description = VALUES(description),
    is_active = VALUES(is_active);
