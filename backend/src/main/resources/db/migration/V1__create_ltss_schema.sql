-- Local Tourism Support System (LTSS)
-- Flyway baseline for MySQL 8.0.16+.
-- Database creation and schema selection are provisioning concerns and are intentionally omitted.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE roles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_code VARCHAR(30) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_roles_code UNIQUE (role_code),
    CONSTRAINT uq_roles_name UNIQUE (role_name),
    CONSTRAINT chk_roles_code_not_blank CHECK (CHAR_LENGTH(TRIM(role_code)) > 0),
    CONSTRAINT chk_roles_name_not_blank CHECK (CHAR_LENGTH(TRIM(role_name)) > 0)
) ENGINE=InnoDB COMMENT='RBAC role definitions; Guest is an unauthenticated state, not a persisted role.';

CREATE TABLE permissions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_permissions_code UNIQUE (permission_code),
    CONSTRAINT chk_permissions_code_not_blank CHECK (CHAR_LENGTH(TRIM(permission_code)) > 0),
    CONSTRAINT chk_permissions_name_not_blank CHECK (CHAR_LENGTH(TRIM(permission_name)) > 0)
) ENGINE=InnoDB COMMENT='Atomic permissions used by the RBAC mechanism.';

CREATE TABLE role_permissions (
    role_id BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    granted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Many-to-many mapping between roles and permissions.';

-- APP INVARIANT: role_id must differ from inherited_role_id and the inheritance
-- graph must remain acyclic. MySQL prohibits CHECK constraints on columns that
-- participate in FK referential actions, so the RBAC service validates both in
-- one transaction before inserting this row.
CREATE TABLE role_inheritances (
    role_id BIGINT UNSIGNED NOT NULL,
    inherited_role_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (role_id, inherited_role_id),
    CONSTRAINT fk_role_inheritance_role
        FOREIGN KEY (role_id) REFERENCES roles(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_role_inheritance_parent
        FOREIGN KEY (inherited_role_id) REFERENCES roles(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Role hierarchy, e.g. BUSINESS_OWNER inherits TOURIST permissions.';

CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NULL,
    avatar_url VARCHAR(1000) NULL,
    address VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified_at DATETIME(6) NULL,
    failed_login_count SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    locked_until DATETIME(6) NULL,
    last_login_at DATETIME(6) NULL,
    password_changed_at DATETIME(6) NULL,
    deactivated_at DATETIME(6) NULL,
    deactivated_by_user_id BIGINT UNSIGNED NULL,
    policy_version VARCHAR(50) NULL,
    policy_accepted_at DATETIME(6) NULL,
    version INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_full_name_not_blank CHECK (CHAR_LENGTH(TRIM(full_name)) > 0),
    CONSTRAINT chk_users_display_name_not_blank CHECK (CHAR_LENGTH(TRIM(display_name)) > 0),
    CONSTRAINT chk_users_phone CHECK (
        phone IS NULL OR (CHAR_LENGTH(phone) = 10 AND phone NOT REGEXP '[^0-9]')
    ),
    CONSTRAINT chk_users_status CHECK (
        status IN ('PENDING_VERIFICATION', 'ACTIVE', 'DEACTIVATED', 'SUSPENDED', 'DELETED')
    ),
    CONSTRAINT fk_users_deactivated_by
        FOREIGN KEY (deactivated_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Authentication identity, personal profile and current account state.';

CREATE TABLE user_roles (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_by_user_id BIGINT UNSIGNED NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    revoked_by_user_id BIGINT UNSIGNED NULL,
    revoked_at DATETIME(6) NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT chk_user_roles_revocation CHECK (
        (is_active = TRUE AND revoked_at IS NULL) OR
        (is_active = FALSE AND revoked_at IS NOT NULL)
    ),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_user_roles_assigned_by
        FOREIGN KEY (assigned_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_user_roles_revoked_by
        FOREIGN KEY (revoked_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    INDEX idx_user_roles_role_active (role_id, is_active)
) ENGINE=InnoDB COMMENT='Current role assignments; audit_logs preserves the complete change history.';

CREATE TABLE password_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    change_reason VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_password_history_reason CHECK (
        change_reason IN ('REGISTRATION', 'USER_CHANGE', 'PASSWORD_RESET', 'ADMIN_RESET')
    ),
    CONSTRAINT fk_password_history_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    INDEX idx_password_history_user_time (user_id, created_at DESC)
) ENGINE=InnoDB COMMENT='Previous password hashes used to enforce the last-three-password rule.';

CREATE TABLE account_tokens (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    token_type VARCHAR(40) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    created_ip VARCHAR(45) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_account_tokens_hash UNIQUE (token_hash),
    CONSTRAINT chk_account_tokens_type CHECK (
        token_type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'REFRESH_TOKEN', 'CHANGE_PASSWORD_OTP')
    ),
    CONSTRAINT chk_account_tokens_expiry CHECK (expires_at > created_at),
    CONSTRAINT fk_account_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    INDEX idx_account_tokens_user_type_time (user_id, token_type, created_at DESC),
    INDEX idx_account_tokens_expiry (expires_at)
) ENGINE=InnoDB COMMENT='Hashed verification, recovery, OTP and refresh tokens.';

CREATE TABLE notifications (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    recipient_user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(40) NOT NULL DEFAULT 'SYSTEM',
    action_url VARCHAR(1000) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_notifications_read_state CHECK (
        (is_read = FALSE AND read_at IS NULL) OR
        (is_read = TRUE AND read_at IS NOT NULL)
    ),
    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    INDEX idx_notifications_recipient_read_time (recipient_user_id, is_read, created_at DESC)
) ENGINE=InnoDB COMMENT='In-application notifications for account, moderation and business workflows.';

CREATE TABLE audit_logs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT UNSIGNED NULL,
    action_code VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT UNSIGNED NULL,
    old_values JSON NULL,
    new_values JSON NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    request_id VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_audit_action_not_blank CHECK (CHAR_LENGTH(TRIM(action_code)) > 0),
    CONSTRAINT chk_audit_entity_not_blank CHECK (CHAR_LENGTH(TRIM(entity_type)) > 0),
    CONSTRAINT fk_audit_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    INDEX idx_audit_actor_time (actor_user_id, created_at DESC),
    INDEX idx_audit_entity_time (entity_type, entity_id, created_at DESC),
    INDEX idx_audit_action_time (action_code, created_at DESC),
    INDEX idx_audit_request_id (request_id)
) ENGINE=InnoDB COMMENT='Append-only audit trail. Application permissions must prohibit UPDATE and DELETE.';

CREATE TABLE search_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    normalized_keyword VARCHAR(255) NOT NULL,
    searched_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_search_history_user_keyword UNIQUE (user_id, normalized_keyword),
    CONSTRAINT chk_search_keyword_not_blank CHECK (CHAR_LENGTH(TRIM(keyword)) > 0),
    CONSTRAINT fk_search_history_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    INDEX idx_search_history_user_time (user_id, searched_at DESC)
) ENGINE=InnoDB COMMENT='At most ten recent unique search keywords per authenticated user; trimming is transactional in backend.';

CREATE TABLE prohibited_terms (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    term VARCHAR(255) NOT NULL,
    normalized_term VARCHAR(255) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'BLOCK',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id BIGINT UNSIGNED NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_prohibited_terms_normalized UNIQUE (normalized_term),
    CONSTRAINT chk_prohibited_term_not_blank CHECK (CHAR_LENGTH(TRIM(term)) > 0),
    CONSTRAINT chk_prohibited_term_severity CHECK (severity IN ('WARN', 'BLOCK')),
    CONSTRAINT fk_prohibited_terms_creator
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Configurable blacklist used for offensive/prohibited keyword filtering.';

CREATE TABLE place_categories (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    marker_icon_key VARCHAR(100) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id BIGINT UNSIGNED NULL,
    updated_by_user_id BIGINT UNSIGNED NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_place_categories_name UNIQUE (category_name),
    CONSTRAINT uq_place_categories_slug UNIQUE (slug),
    CONSTRAINT chk_place_category_name_not_blank CHECK (CHAR_LENGTH(TRIM(category_name)) > 0),
    CONSTRAINT fk_place_categories_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_place_categories_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Extensible categories for relics, restaurants, hotels, craft villages and other map places.';

CREATE TABLE places (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_id BIGINT UNSIGNED NOT NULL,
    created_by_user_id BIGINT UNSIGNED NULL,
    updated_by_user_id BIGINT UNSIGNED NULL,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL,
    summary VARCHAR(700) NULL,
    description LONGTEXT NULL,
    address VARCHAR(500) NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    opening_hours VARCHAR(500) NULL,
    entrance_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    contact_phone VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME(6) NULL,
    published_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    version INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_places_slug UNIQUE (slug),
    CONSTRAINT chk_places_name_not_blank CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_places_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90.0000000 AND 90.0000000),
    CONSTRAINT chk_places_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180.0000000 AND 180.0000000),
    CONSTRAINT chk_places_fee CHECK (entrance_fee >= 0),
    CONSTRAINT chk_places_phone CHECK (
        contact_phone IS NULL OR (CHAR_LENGTH(contact_phone) = 10 AND contact_phone NOT REGEXP '[^0-9]')
    ),
    CONSTRAINT chk_places_status CHECK (
        status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'ARCHIVED', 'DELETED')
    ),
    CONSTRAINT chk_places_published_timestamp CHECK (
        status <> 'PUBLISHED' OR published_at IS NOT NULL
    ),
    CONSTRAINT chk_places_deleted_timestamp CHECK (
        status <> 'DELETED' OR deleted_at IS NOT NULL
    ),
    CONSTRAINT fk_places_category
        FOREIGN KEY (category_id) REFERENCES place_categories(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_places_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_places_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    INDEX idx_places_status_category_name (status, category_id, name),
    INDEX idx_places_coordinates (latitude, longitude),
    FULLTEXT INDEX ftx_places_search (name, summary, description)
) ENGINE=InnoDB COMMENT='Authoritative map-visible tourism and service locations.';

CREATE TABLE relic_details (
    place_id BIGINT UNSIGNED NOT NULL,
    historical_period VARCHAR(150) NULL,
    history LONGTEXT NULL,
    architecture LONGTEXT NULL,
    recognition_level VARCHAR(100) NULL,
    recognized_at DATE NULL,
    preservation_note TEXT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (place_id),
    CONSTRAINT fk_relic_details_place
        FOREIGN KEY (place_id) REFERENCES places(id)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='One-to-one heritage-specific extension of a place.';

CREATE TABLE businesses (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    place_id BIGINT UNSIGNED NOT NULL,
    owner_user_id BIGINT UNSIGNED NOT NULL,
    registration_number VARCHAR(100) NULL,
    contact_email VARCHAR(255) NULL,
    website_url VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by_user_id BIGINT UNSIGNED NULL,
    approved_at DATETIME(6) NULL,
    version INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_businesses_place UNIQUE (place_id),
    CONSTRAINT uq_businesses_owner UNIQUE (owner_user_id),
    CONSTRAINT uq_businesses_registration_number UNIQUE (registration_number),
    CONSTRAINT chk_businesses_status CHECK (
        status IN ('PENDING', 'ACTIVE', 'REJECTED', 'SUSPENDED', 'INACTIVE')
    ),
    CONSTRAINT fk_businesses_place
        FOREIGN KEY (place_id) REFERENCES places(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_businesses_owner
        FOREIGN KEY (owner_user_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_businesses_approved_by
        FOREIGN KEY (approved_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    INDEX idx_businesses_status_created (status, created_at DESC)
) ENGINE=InnoDB COMMENT='Business ownership and approval data; common profile/location fields live in places.';

CREATE TABLE events (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    place_id BIGINT UNSIGNED NULL,
    created_by_user_id BIGINT UNSIGNED NULL,
    updated_by_user_id BIGINT UNSIGNED NULL,
    title VARCHAR(250) NOT NULL,
    slug VARCHAR(280) NOT NULL,
    description LONGTEXT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    location_note VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME(6) NULL,
    published_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    version INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_events_slug UNIQUE (slug),
    CONSTRAINT chk_events_title_not_blank CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_events_period CHECK (end_at > start_at),
    CONSTRAINT chk_events_status CHECK (
        status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'CANCELLED', 'ARCHIVED', 'DELETED')
    ),
    CONSTRAINT chk_events_published_timestamp CHECK (
        status <> 'PUBLISHED' OR published_at IS NOT NULL
    ),
    CONSTRAINT chk_events_deleted_timestamp CHECK (
        status <> 'DELETED' OR deleted_at IS NOT NULL
    ),
    CONSTRAINT fk_events_place
        FOREIGN KEY (place_id) REFERENCES places(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_events_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_events_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    INDEX idx_events_status_start (status, start_at),
    INDEX idx_events_place_start (place_id, start_at),
    INDEX idx_events_period (start_at, end_at)
) ENGINE=InnoDB COMMENT='Festival and tourism event records used for public discovery and monthly reports.';

CREATE TABLE article_categories (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id BIGINT UNSIGNED NULL,
    updated_by_user_id BIGINT UNSIGNED NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_article_categories_name UNIQUE (category_name),
    CONSTRAINT uq_article_categories_slug UNIQUE (slug),
    CONSTRAINT chk_article_category_name_not_blank CHECK (CHAR_LENGTH(TRIM(category_name)) > 0),
    CONSTRAINT fk_article_categories_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_article_categories_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Moderator-managed article categories.';

CREATE TABLE articles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_id BIGINT UNSIGNED NOT NULL,
    place_id BIGINT UNSIGNED NULL,
    event_id BIGINT UNSIGNED NULL,
    author_user_id BIGINT UNSIGNED NULL,
    updated_by_user_id BIGINT UNSIGNED NULL,
    title VARCHAR(250) NOT NULL,
    slug VARCHAR(280) NOT NULL,
    summary VARCHAR(700) NULL,
    content LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME(6) NULL,
    published_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    version INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_articles_slug UNIQUE (slug),
    CONSTRAINT chk_articles_title_not_blank CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_articles_content_not_blank CHECK (CHAR_LENGTH(TRIM(content)) > 0),
    CONSTRAINT chk_articles_status CHECK (
        status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'ARCHIVED', 'DELETED')
    ),
    CONSTRAINT chk_articles_published_timestamp CHECK (
        status <> 'PUBLISHED' OR published_at IS NOT NULL
    ),
    CONSTRAINT chk_articles_deleted_timestamp CHECK (
        status <> 'DELETED' OR deleted_at IS NOT NULL
    ),
    CONSTRAINT fk_articles_category
        FOREIGN KEY (category_id) REFERENCES article_categories(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_articles_place
        FOREIGN KEY (place_id) REFERENCES places(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_articles_event
        FOREIGN KEY (event_id) REFERENCES events(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_articles_author
        FOREIGN KEY (author_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_articles_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    INDEX idx_articles_status_published (status, published_at DESC),
    INDEX idx_articles_category_status (category_id, status, published_at DESC),
    INDEX idx_articles_place_status (place_id, status),
    FULLTEXT INDEX ftx_articles_search (title, summary, content)
) ENGINE=InnoDB COMMENT='Moderated heritage, festival and local cultural articles.';

CREATE TABLE media_assets (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    uploaded_by_user_id BIGINT UNSIGNED NULL,
    media_type VARCHAR(30) NOT NULL,
    storage_provider VARCHAR(50) NULL,
    storage_key VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    storage_key_hash BINARY(32)
        GENERATED ALWAYS AS (UNHEX(SHA2(storage_key, 256))) STORED
        COMMENT 'SHA-256 of the canonical, exact-case storage key',
    media_url VARCHAR(1500) NOT NULL,
    thumbnail_url VARCHAR(1500) NULL,
    mime_type VARCHAR(150) NOT NULL,
    file_size_bytes BIGINT UNSIGNED NOT NULL,
    width_px INT UNSIGNED NULL,
    height_px INT UNSIGNED NULL,
    duration_seconds DECIMAL(10,3) NULL,
    checksum_sha256 CHAR(64) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_media_assets_storage_key_hash UNIQUE (storage_key_hash),
    CONSTRAINT chk_media_assets_type CHECK (
        media_type IN ('IMAGE', 'PANORAMA_360', 'AUDIO', 'VIDEO')
    ),
    CONSTRAINT chk_media_assets_size CHECK (file_size_bytes > 0),
    CONSTRAINT chk_media_assets_audio_limit CHECK (
        media_type <> 'AUDIO' OR file_size_bytes <= 20971520
    ),
    CONSTRAINT chk_media_assets_panorama_limit CHECK (
        media_type <> 'PANORAMA_360' OR file_size_bytes <= 52428800
    ),
    CONSTRAINT chk_media_assets_panorama_ratio CHECK (
        media_type <> 'PANORAMA_360' OR
        (width_px IS NOT NULL AND height_px IS NOT NULL AND width_px = 2 * height_px)
    ),
    CONSTRAINT fk_media_assets_uploader
        FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    INDEX idx_media_assets_type_created (media_type, created_at DESC),
    INDEX idx_media_assets_checksum (checksum_sha256)
) ENGINE=InnoDB COMMENT='Reusable metadata for images, panorama files, audio guides and videos.';

CREATE TABLE business_posts (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id BIGINT UNSIGNED NOT NULL,
    created_by_user_id BIGINT UNSIGNED NULL,
    updated_by_user_id BIGINT UNSIGNED NULL,
    title VARCHAR(250) NOT NULL,
    slug VARCHAR(280) NOT NULL,
    summary VARCHAR(700) NULL,
    content LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME(6) NULL,
    published_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    version INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_business_posts_slug UNIQUE (slug),
    CONSTRAINT chk_business_posts_title_not_blank CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_business_posts_content_not_blank CHECK (CHAR_LENGTH(TRIM(content)) > 0),
    CONSTRAINT chk_business_posts_status CHECK (
        status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'ARCHIVED', 'DELETED')
    ),
    CONSTRAINT chk_business_posts_published_timestamp CHECK (
        status <> 'PUBLISHED' OR published_at IS NOT NULL
    ),
    CONSTRAINT chk_business_posts_deleted_timestamp CHECK (
        status <> 'DELETED' OR deleted_at IS NOT NULL
    ),
    CONSTRAINT fk_business_posts_business
        FOREIGN KEY (business_id) REFERENCES businesses(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_business_posts_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_business_posts_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    INDEX idx_business_posts_business_status_time (business_id, status, created_at DESC),
    INDEX idx_business_posts_public_time (status, published_at DESC),
    FULLTEXT INDEX ftx_business_posts_search (title, summary, content)
) ENGINE=InnoDB COMMENT='Business-owned informational/promotional posts with moderation lifecycle.';

CREATE TABLE tags (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tag_name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_tags_name UNIQUE (tag_name),
    CONSTRAINT uq_tags_slug UNIQUE (slug),
    CONSTRAINT chk_tags_name_not_blank CHECK (CHAR_LENGTH(TRIM(tag_name)) > 0)
) ENGINE=InnoDB COMMENT='Reusable tags for business posts.';

CREATE TABLE business_post_tags (
    business_post_id BIGINT UNSIGNED NOT NULL,
    tag_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (business_post_id, tag_id),
    CONSTRAINT fk_business_post_tags_post
        FOREIGN KEY (business_post_id) REFERENCES business_posts(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_business_post_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_business_post_tags_tag (tag_id, business_post_id)
) ENGINE=InnoDB COMMENT='Many-to-many mapping between business posts and tags.';

CREATE TABLE promotions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id BIGINT UNSIGNED NOT NULL,
    created_by_user_id BIGINT UNSIGNED NULL,
    updated_by_user_id BIGINT UNSIGNED NULL,
    title VARCHAR(250) NOT NULL,
    description LONGTEXT NOT NULL,
    discount_type VARCHAR(30) NULL,
    discount_value DECIMAL(12,2) NULL,
    promo_code VARCHAR(50) NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME(6) NULL,
    published_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    version INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_promotions_promo_code UNIQUE (promo_code),
    CONSTRAINT chk_promotions_title_not_blank CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_promotions_description_not_blank CHECK (CHAR_LENGTH(TRIM(description)) > 0),
    CONSTRAINT chk_promotions_period CHECK (end_at > start_at),
    CONSTRAINT chk_promotions_discount_type CHECK (
        discount_type IS NULL OR discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'OTHER')
    ),
    CONSTRAINT chk_promotions_discount_value CHECK (
        discount_value IS NULL OR discount_value >= 0
    ),
    CONSTRAINT chk_promotions_percentage CHECK (
        discount_type IS NULL OR discount_type <> 'PERCENTAGE' OR
        (discount_value IS NOT NULL AND discount_value BETWEEN 0 AND 100)
    ),
    CONSTRAINT chk_promotions_status CHECK (
        status IN ('DRAFT', 'PENDING', 'ACTIVE', 'REJECTED', 'EXPIRED', 'ARCHIVED', 'DELETED')
    ),
    CONSTRAINT chk_promotions_active_timestamp CHECK (
        status <> 'ACTIVE' OR published_at IS NOT NULL
    ),
    CONSTRAINT chk_promotions_deleted_timestamp CHECK (
        status <> 'DELETED' OR deleted_at IS NOT NULL
    ),
    CONSTRAINT fk_promotions_business
        FOREIGN KEY (business_id) REFERENCES businesses(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_promotions_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_promotions_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    INDEX idx_promotions_business_status_period (business_id, status, start_at, end_at),
    INDEX idx_promotions_public_period (status, start_at, end_at)
) ENGINE=InnoDB COMMENT='Time-bounded offers and campaigns created by a business.';

-- APP INVARIANT: a copied tour cannot reference itself. The original CHECK
-- referenced both the AUTO_INCREMENT id and an FK with ON DELETE SET NULL,
-- which MySQL rejects. Tour creation/copy validation enforces the rule.
CREATE TABLE tours (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT UNSIGNED NOT NULL,
    source_tour_id BIGINT UNSIGNED NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    region VARCHAR(150) NULL,
    difficulty_level VARCHAR(30) NULL,
    estimated_distance_km DECIMAL(10,2) NULL,
    estimated_duration_minutes INT UNSIGNED NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    submitted_at DATETIME(6) NULL,
    published_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    version INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_tours_title_not_blank CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_tours_distance CHECK (estimated_distance_km IS NULL OR estimated_distance_km >= 0),
    CONSTRAINT chk_tours_status CHECK (
        status IN ('DRAFT', 'SUBMITTED', 'PUBLISHED', 'REJECTED', 'COMPLETED', 'CANCELLED', 'ARCHIVED', 'DELETED')
    ),
    CONSTRAINT chk_tours_visibility CHECK (visibility IN ('PRIVATE', 'UNLISTED', 'PUBLIC')),
    CONSTRAINT chk_tours_published_timestamp CHECK (
        status <> 'PUBLISHED' OR published_at IS NOT NULL
    ),
    CONSTRAINT chk_tours_completed_timestamp CHECK (
        status <> 'COMPLETED' OR completed_at IS NOT NULL
    ),
    CONSTRAINT chk_tours_deleted_timestamp CHECK (
        status <> 'DELETED' OR deleted_at IS NOT NULL
    ),
    CONSTRAINT fk_tours_owner
        FOREIGN KEY (owner_user_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_tours_source
        FOREIGN KEY (source_tour_id) REFERENCES tours(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    INDEX idx_tours_owner_status_time (owner_user_id, status, updated_at DESC),
    INDEX idx_tours_public_time (visibility, status, published_at DESC),
    INDEX idx_tours_source (source_tour_id)
) ENGINE=InnoDB COMMENT='Personal itineraries and derived copies of shared tours.';

CREATE TABLE tour_items (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tour_id BIGINT UNSIGNED NOT NULL,
    place_id BIGINT UNSIGNED NOT NULL,
    visit_order TINYINT UNSIGNED NOT NULL,
    planned_start_at DATETIME(6) NULL,
    duration_minutes INT UNSIGNED NULL,
    transport_method VARCHAR(50) NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_tour_items_place UNIQUE (tour_id, place_id),
    CONSTRAINT uq_tour_items_order UNIQUE (tour_id, visit_order),
    CONSTRAINT chk_tour_items_order CHECK (visit_order BETWEEN 1 AND 10),
    CONSTRAINT chk_tour_items_duration CHECK (duration_minutes IS NULL OR duration_minutes > 0),
    CONSTRAINT fk_tour_items_tour
        FOREIGN KEY (tour_id) REFERENCES tours(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_tour_items_place
        FOREIGN KEY (place_id) REFERENCES places(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_tour_items_place (place_id, tour_id)
) ENGINE=InnoDB COMMENT='Ordered many-to-many relationship between tours and map places.';

CREATE TABLE favorites (
    user_id BIGINT UNSIGNED NOT NULL,
    place_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, place_id),
    CONSTRAINT fk_favorites_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_favorites_place
        FOREIGN KEY (place_id) REFERENCES places(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    INDEX idx_favorites_place (place_id, created_at DESC)
) ENGINE=InnoDB COMMENT='User favorite places.';

CREATE TABLE quizzes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    place_id BIGINT UNSIGNED NOT NULL,
    created_by_user_id BIGINT UNSIGNED NULL,
    updated_by_user_id BIGINT UNSIGNED NULL,
    title VARCHAR(250) NOT NULL,
    description TEXT NULL,
    time_limit_seconds SMALLINT UNSIGNED NOT NULL DEFAULT 600,
    passing_score_percent DECIMAL(5,2) NOT NULL DEFAULT 60.00,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME(6) NULL,
    published_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    version INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_quizzes_title_not_blank CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_quizzes_time_limit CHECK (time_limit_seconds BETWEEN 1 AND 600),
    CONSTRAINT chk_quizzes_passing_score CHECK (passing_score_percent BETWEEN 0 AND 100),
    CONSTRAINT chk_quizzes_status CHECK (
        status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'ARCHIVED', 'DELETED')
    ),
    CONSTRAINT chk_quizzes_published_timestamp CHECK (
        status <> 'PUBLISHED' OR published_at IS NOT NULL
    ),
    CONSTRAINT chk_quizzes_deleted_timestamp CHECK (
        status <> 'DELETED' OR deleted_at IS NOT NULL
    ),
    CONSTRAINT fk_quizzes_place
        FOREIGN KEY (place_id) REFERENCES places(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_quizzes_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_quizzes_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    INDEX idx_quizzes_place_status (place_id, status, published_at DESC),
    INDEX idx_quizzes_status_time (status, created_at DESC)
) ENGINE=InnoDB COMMENT='Destination/relic-based quiz sets moderated before publication.';

CREATE TABLE questions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    quiz_id BIGINT UNSIGNED NOT NULL,
    content VARCHAR(250) NOT NULL,
    explanation TEXT NULL,
    display_order SMALLINT UNSIGNED NOT NULL,
    points DECIMAL(6,2) NOT NULL DEFAULT 1.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_questions_order UNIQUE (quiz_id, display_order),
    CONSTRAINT chk_questions_content_not_blank CHECK (CHAR_LENGTH(TRIM(content)) > 0),
    CONSTRAINT chk_questions_points CHECK (points > 0),
    CONSTRAINT fk_questions_quiz
        FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    INDEX idx_questions_quiz_active (quiz_id, is_active, display_order)
) ENGINE=InnoDB COMMENT='Questions belonging to a quiz; deletion is soft when attempts exist.';

CREATE TABLE answers (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    question_id BIGINT UNSIGNED NOT NULL,
    content VARCHAR(100) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    display_order TINYINT UNSIGNED NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_answers_order UNIQUE (question_id, display_order),
    CONSTRAINT chk_answers_content_not_blank CHECK (CHAR_LENGTH(TRIM(content)) > 0),
    CONSTRAINT chk_answers_order CHECK (display_order BETWEEN 1 AND 4),
    CONSTRAINT fk_answers_question
        FOREIGN KEY (question_id) REFERENCES questions(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    INDEX idx_answers_question_active (question_id, is_active, display_order)
) ENGINE=InnoDB COMMENT='Two-to-four answer choices per question; exactly one correct answer is transactionally validated.';

CREATE TABLE badges (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    badge_code VARCHAR(60) NOT NULL,
    badge_name VARCHAR(150) NOT NULL,
    description VARCHAR(700) NULL,
    icon_url VARCHAR(1000) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_badges_code UNIQUE (badge_code),
    CONSTRAINT uq_badges_name UNIQUE (badge_name),
    CONSTRAINT chk_badges_code_not_blank CHECK (CHAR_LENGTH(TRIM(badge_code)) > 0),
    CONSTRAINT chk_badges_name_not_blank CHECK (CHAR_LENGTH(TRIM(badge_name)) > 0)
) ENGINE=InnoDB COMMENT='Achievement badge definitions.';

CREATE TABLE quiz_badges (
    quiz_id BIGINT UNSIGNED NOT NULL,
    badge_id BIGINT UNSIGNED NOT NULL,
    minimum_score_percent DECIMAL(5,2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (quiz_id, badge_id),
    CONSTRAINT chk_quiz_badges_score CHECK (minimum_score_percent BETWEEN 0 AND 100),
    CONSTRAINT fk_quiz_badges_quiz
        FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_quiz_badges_badge
        FOREIGN KEY (badge_id) REFERENCES badges(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Badge rewards and score thresholds configured for a quiz.';

CREATE TABLE quiz_attempts (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    quiz_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'IN_PROGRESS',
    randomization_seed VARCHAR(64) NULL,
    started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NOT NULL,
    submitted_at DATETIME(6) NULL,
    score DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    total_points DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    score_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    is_passed BOOLEAN NOT NULL DEFAULT FALSE,
    location_verified_at DATETIME(6) NULL,
    distance_to_place_meters DECIMAL(10,2) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_quiz_attempts_status CHECK (
        status IN ('IN_PROGRESS', 'SUBMITTED', 'AUTO_SUBMITTED', 'ABANDONED')
    ),
    CONSTRAINT chk_quiz_attempts_expiry CHECK (expires_at > started_at),
    CONSTRAINT chk_quiz_attempts_scores CHECK (
        score >= 0 AND total_points >= 0 AND score_percent BETWEEN 0 AND 100
    ),
    CONSTRAINT chk_quiz_attempts_distance CHECK (
        distance_to_place_meters IS NULL OR distance_to_place_meters >= 0
    ),
    CONSTRAINT chk_quiz_attempts_submission_timestamp CHECK (
        (status = 'IN_PROGRESS' AND submitted_at IS NULL) OR
        (status IN ('SUBMITTED', 'AUTO_SUBMITTED') AND submitted_at IS NOT NULL) OR
        status = 'ABANDONED'
    ),
    CONSTRAINT fk_quiz_attempts_quiz
        FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_quiz_attempts_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_quiz_attempts_user_time (user_id, started_at DESC),
    INDEX idx_quiz_attempts_quiz_time (quiz_id, started_at DESC),
    INDEX idx_quiz_attempts_status_expiry (status, expires_at)
) ENGINE=InnoDB COMMENT='Immutable attempt header and grading result for each quiz play.';

CREATE TABLE quiz_attempt_answers (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT UNSIGNED NOT NULL,
    question_id BIGINT UNSIGNED NULL,
    selected_answer_id BIGINT UNSIGNED NULL,
    question_order SMALLINT UNSIGNED NOT NULL,
    question_text_snapshot VARCHAR(250) NOT NULL,
    selected_answer_text_snapshot VARCHAR(100) NULL,
    correct_answer_text_snapshot VARCHAR(100) NOT NULL,
    explanation_snapshot TEXT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    awarded_points DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    answered_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_quiz_attempt_answers_order UNIQUE (attempt_id, question_order),
    CONSTRAINT chk_quiz_attempt_answers_points CHECK (awarded_points >= 0),
    CONSTRAINT fk_quiz_attempt_answers_attempt
        FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_quiz_attempt_answers_question
        FOREIGN KEY (question_id) REFERENCES questions(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_quiz_attempt_answers_selected
        FOREIGN KEY (selected_answer_id) REFERENCES answers(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    INDEX idx_quiz_attempt_answers_question (question_id),
    INDEX idx_quiz_attempt_answers_selected (selected_answer_id)
) ENGINE=InnoDB COMMENT='Per-question attempt result with text snapshots to preserve history after content edits.';

CREATE TABLE user_badges (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    badge_id BIGINT UNSIGNED NOT NULL,
    awarded_by_quiz_id BIGINT UNSIGNED NULL,
    awarded_attempt_id BIGINT UNSIGNED NULL,
    awarded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_user_badges_once UNIQUE (user_id, badge_id),
    CONSTRAINT fk_user_badges_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_user_badges_badge
        FOREIGN KEY (badge_id) REFERENCES badges(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_user_badges_quiz
        FOREIGN KEY (awarded_by_quiz_id) REFERENCES quizzes(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_user_badges_attempt
        FOREIGN KEY (awarded_attempt_id) REFERENCES quiz_attempts(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    INDEX idx_user_badges_user_time (user_id, awarded_at DESC)
) ENGINE=InnoDB COMMENT='Award history; unique user/badge enforces one award per badge.';

-- APP INVARIANT: exactly one of place_id, business_id, article_id, and tour_id
-- must be non-null. MySQL cannot combine that CHECK with the preserved FK
-- actions. Review creation validates the target and inserts review/media/
-- moderation records atomically; the four per-target unique keys remain.
CREATE TABLE reviews (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    place_id BIGINT UNSIGNED NULL,
    business_id BIGINT UNSIGNED NULL,
    article_id BIGINT UNSIGNED NULL,
    tour_id BIGINT UNSIGNED NULL,
    rating TINYINT UNSIGNED NOT NULL,
    comment TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    version INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_reviews_user_place UNIQUE (user_id, place_id),
    CONSTRAINT uq_reviews_user_business UNIQUE (user_id, business_id),
    CONSTRAINT uq_reviews_user_article UNIQUE (user_id, article_id),
    CONSTRAINT uq_reviews_user_tour UNIQUE (user_id, tour_id),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_reviews_comment_length CHECK (CHAR_LENGTH(TRIM(comment)) >= 20),
    CONSTRAINT chk_reviews_status CHECK (
        status IN ('PENDING', 'VISIBLE', 'REJECTED', 'HIDDEN', 'REMOVED')
    ),
    CONSTRAINT chk_reviews_visible_timestamp CHECK (
        status <> 'VISIBLE' OR published_at IS NOT NULL
    ),
    CONSTRAINT fk_reviews_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reviews_place
        FOREIGN KEY (place_id) REFERENCES places(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reviews_business
        FOREIGN KEY (business_id) REFERENCES businesses(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reviews_article
        FOREIGN KEY (article_id) REFERENCES articles(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reviews_tour
        FOREIGN KEY (tour_id) REFERENCES tours(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_reviews_place_status_time (place_id, status, created_at DESC),
    INDEX idx_reviews_business_status_time (business_id, status, created_at DESC),
    INDEX idx_reviews_article_status_time (article_id, status, created_at DESC),
    INDEX idx_reviews_tour_status_time (tour_id, status, created_at DESC),
    INDEX idx_reviews_status_time (status, created_at DESC)
) ENGINE=InnoDB COMMENT='One review per user and target, covering places, businesses, articles or tours.';

CREATE TABLE review_replies (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    review_id BIGINT UNSIGNED NOT NULL,
    replied_by_user_id BIGINT UNSIGNED NULL,
    content TEXT NOT NULL,
    version INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_review_replies_review UNIQUE (review_id),
    CONSTRAINT chk_review_replies_content CHECK (CHAR_LENGTH(TRIM(content)) > 0),
    CONSTRAINT fk_review_replies_review
        FOREIGN KEY (review_id) REFERENCES reviews(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_review_replies_user
        FOREIGN KEY (replied_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='At most one official reply to a review.';

CREATE TABLE engagement_event_types (
    event_type_code VARCHAR(40) NOT NULL,
    event_type_name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (event_type_code),
    CONSTRAINT chk_engagement_event_type_code CHECK (CHAR_LENGTH(TRIM(event_type_code)) > 0),
    CONSTRAINT chk_engagement_event_type_name CHECK (CHAR_LENGTH(TRIM(event_type_name)) > 0)
) ENGINE=InnoDB COMMENT='Extensible event-type configuration for views, clicks, shares and route requests.';

-- APP INVARIANT: exactly one target FK must be non-null. The ingestion service
-- validates the supported event/target pair and inserts it transactionally.
-- The original CHECK cannot coexist with these preserved FK actions in MySQL.
CREATE TABLE engagement_events (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    event_type_code VARCHAR(40) NOT NULL,
    user_id BIGINT UNSIGNED NULL,
    session_key VARCHAR(100) NOT NULL,
    place_id BIGINT UNSIGNED NULL,
    business_id BIGINT UNSIGNED NULL,
    event_id BIGINT UNSIGNED NULL,
    article_id BIGINT UNSIGNED NULL,
    business_post_id BIGINT UNSIGNED NULL,
    promotion_id BIGINT UNSIGNED NULL,
    tour_id BIGINT UNSIGNED NULL,
    metadata JSON NULL,
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_engagement_type
        FOREIGN KEY (event_type_code) REFERENCES engagement_event_types(event_type_code)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_engagement_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_engagement_place
        FOREIGN KEY (place_id) REFERENCES places(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_engagement_business
        FOREIGN KEY (business_id) REFERENCES businesses(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_engagement_event
        FOREIGN KEY (event_id) REFERENCES events(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_engagement_article
        FOREIGN KEY (article_id) REFERENCES articles(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_engagement_business_post
        FOREIGN KEY (business_post_id) REFERENCES business_posts(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_engagement_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotions(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_engagement_tour
        FOREIGN KEY (tour_id) REFERENCES tours(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_engagement_type_time (event_type_code, occurred_at DESC),
    INDEX idx_engagement_session_time (session_key, occurred_at DESC),
    INDEX idx_engagement_user_time (user_id, occurred_at DESC),
    INDEX idx_engagement_place_time (place_id, occurred_at DESC),
    INDEX idx_engagement_business_time (business_id, occurred_at DESC),
    INDEX idx_engagement_event_time (event_id, occurred_at DESC),
    INDEX idx_engagement_article_time (article_id, occurred_at DESC),
    INDEX idx_engagement_business_post_time (business_post_id, occurred_at DESC),
    INDEX idx_engagement_promotion_time (promotion_id, occurred_at DESC),
    INDEX idx_engagement_tour_time (tour_id, occurred_at DESC)
) ENGINE=InnoDB COMMENT='Raw analytics events for dashboards and visit statistics.';

-- APP INVARIANT: exactly one target FK must be non-null. Submission and
-- resolution services lock the target/case, reject duplicate pending cases,
-- and update the target, moderation record, notification, and audit atomically.
-- A target CHECK cannot coexist with the preserved FK actions in MySQL.
CREATE TABLE moderation_records (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    submitted_by_user_id BIGINT UNSIGNED NULL,
    moderator_user_id BIGINT UNSIGNED NULL,
    place_id BIGINT UNSIGNED NULL,
    business_id BIGINT UNSIGNED NULL,
    event_id BIGINT UNSIGNED NULL,
    article_id BIGINT UNSIGNED NULL,
    business_post_id BIGINT UNSIGNED NULL,
    promotion_id BIGINT UNSIGNED NULL,
    tour_id BIGINT UNSIGNED NULL,
    quiz_id BIGINT UNSIGNED NULL,
    review_id BIGINT UNSIGNED NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decision VARCHAR(20) NULL,
    submission_note VARCHAR(1000) NULL,
    decision_reason VARCHAR(1000) NULL,
    submitted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_moderation_status CHECK (status IN ('PENDING', 'RESOLVED', 'CANCELLED')),
    CONSTRAINT chk_moderation_decision CHECK (decision IS NULL OR decision IN ('APPROVED', 'REJECTED')),
    CONSTRAINT chk_moderation_resolution CHECK (
        (status = 'PENDING' AND decision IS NULL AND resolved_at IS NULL) OR
        (status = 'CANCELLED' AND decision IS NULL AND resolved_at IS NOT NULL) OR
        (status = 'RESOLVED' AND decision IS NOT NULL AND resolved_at IS NOT NULL)
    ),
    CONSTRAINT chk_moderation_rejection_reason CHECK (
        decision IS NULL OR decision <> 'REJECTED' OR
        (decision_reason IS NOT NULL AND CHAR_LENGTH(TRIM(decision_reason)) > 0)
    ),
    CONSTRAINT fk_moderation_submitted_by
        FOREIGN KEY (submitted_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_moderation_moderator
        FOREIGN KEY (moderator_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_moderation_place
        FOREIGN KEY (place_id) REFERENCES places(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_moderation_business
        FOREIGN KEY (business_id) REFERENCES businesses(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_moderation_event
        FOREIGN KEY (event_id) REFERENCES events(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_moderation_article
        FOREIGN KEY (article_id) REFERENCES articles(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_moderation_business_post
        FOREIGN KEY (business_post_id) REFERENCES business_posts(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_moderation_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotions(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_moderation_tour
        FOREIGN KEY (tour_id) REFERENCES tours(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_moderation_quiz
        FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_moderation_review
        FOREIGN KEY (review_id) REFERENCES reviews(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_moderation_queue (status, submitted_at),
    INDEX idx_moderation_moderator_time (moderator_user_id, resolved_at DESC),
    INDEX idx_moderation_place (place_id, submitted_at DESC),
    INDEX idx_moderation_business (business_id, submitted_at DESC),
    INDEX idx_moderation_event (event_id, submitted_at DESC),
    INDEX idx_moderation_article (article_id, submitted_at DESC),
    INDEX idx_moderation_business_post (business_post_id, submitted_at DESC),
    INDEX idx_moderation_promotion (promotion_id, submitted_at DESC),
    INDEX idx_moderation_tour (tour_id, submitted_at DESC),
    INDEX idx_moderation_quiz (quiz_id, submitted_at DESC),
    INDEX idx_moderation_review (review_id, submitted_at DESC)
) ENGINE=InnoDB COMMENT='Appendable moderation submission and decision history with enforceable target FKs.';

CREATE TABLE place_media (
    place_id BIGINT UNSIGNED NOT NULL,
    media_asset_id BIGINT UNSIGNED NOT NULL,
    usage_type VARCHAR(40) NOT NULL,
    display_order INT UNSIGNED NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (place_id, media_asset_id),
    CONSTRAINT fk_place_media_place
        FOREIGN KEY (place_id) REFERENCES places(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_place_media_asset
        FOREIGN KEY (media_asset_id) REFERENCES media_assets(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_place_media_order (place_id, usage_type, display_order)
) ENGINE=InnoDB COMMENT='Images, audio guides and panoramas attached to a place.';

CREATE TABLE event_media (
    event_id BIGINT UNSIGNED NOT NULL,
    media_asset_id BIGINT UNSIGNED NOT NULL,
    usage_type VARCHAR(40) NOT NULL,
    display_order INT UNSIGNED NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (event_id, media_asset_id),
    CONSTRAINT fk_event_media_event
        FOREIGN KEY (event_id) REFERENCES events(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_event_media_asset
        FOREIGN KEY (media_asset_id) REFERENCES media_assets(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_event_media_order (event_id, usage_type, display_order)
) ENGINE=InnoDB COMMENT='Media attached to an event.';

CREATE TABLE article_media (
    article_id BIGINT UNSIGNED NOT NULL,
    media_asset_id BIGINT UNSIGNED NOT NULL,
    usage_type VARCHAR(40) NOT NULL,
    display_order INT UNSIGNED NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (article_id, media_asset_id),
    CONSTRAINT fk_article_media_article
        FOREIGN KEY (article_id) REFERENCES articles(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_article_media_asset
        FOREIGN KEY (media_asset_id) REFERENCES media_assets(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_article_media_order (article_id, usage_type, display_order)
) ENGINE=InnoDB COMMENT='Media attached to a cultural article.';

CREATE TABLE business_post_media (
    business_post_id BIGINT UNSIGNED NOT NULL,
    media_asset_id BIGINT UNSIGNED NOT NULL,
    usage_type VARCHAR(40) NOT NULL,
    display_order INT UNSIGNED NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (business_post_id, media_asset_id),
    CONSTRAINT fk_business_post_media_post
        FOREIGN KEY (business_post_id) REFERENCES business_posts(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_business_post_media_asset
        FOREIGN KEY (media_asset_id) REFERENCES media_assets(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_business_post_media_order (business_post_id, usage_type, display_order)
) ENGINE=InnoDB COMMENT='Featured and inline media attached to a business post.';

CREATE TABLE promotion_media (
    promotion_id BIGINT UNSIGNED NOT NULL,
    media_asset_id BIGINT UNSIGNED NOT NULL,
    usage_type VARCHAR(40) NOT NULL,
    display_order INT UNSIGNED NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (promotion_id, media_asset_id),
    CONSTRAINT fk_promotion_media_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotions(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_promotion_media_asset
        FOREIGN KEY (media_asset_id) REFERENCES media_assets(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_promotion_media_order (promotion_id, usage_type, display_order)
) ENGINE=InnoDB COMMENT='Images attached to a promotion.';

CREATE TABLE tour_media (
    tour_id BIGINT UNSIGNED NOT NULL,
    media_asset_id BIGINT UNSIGNED NOT NULL,
    usage_type VARCHAR(40) NOT NULL,
    display_order INT UNSIGNED NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (tour_id, media_asset_id),
    CONSTRAINT fk_tour_media_tour
        FOREIGN KEY (tour_id) REFERENCES tours(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_tour_media_asset
        FOREIGN KEY (media_asset_id) REFERENCES media_assets(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_tour_media_order (tour_id, usage_type, display_order)
) ENGINE=InnoDB COMMENT='Cover and gallery media attached to a tour.';

CREATE TABLE review_media (
    review_id BIGINT UNSIGNED NOT NULL,
    media_asset_id BIGINT UNSIGNED NOT NULL,
    display_order TINYINT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (review_id, media_asset_id),
    CONSTRAINT uq_review_media_order UNIQUE (review_id, display_order),
    CONSTRAINT chk_review_media_order CHECK (display_order BETWEEN 1 AND 3),
    CONSTRAINT fk_review_media_review
        FOREIGN KEY (review_id) REFERENCES reviews(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_review_media_asset
        FOREIGN KEY (media_asset_id) REFERENCES media_assets(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Up to three review images; count validation is completed in the backend transaction.';

CREATE TABLE quiz_media (
    quiz_id BIGINT UNSIGNED NOT NULL,
    media_asset_id BIGINT UNSIGNED NOT NULL,
    usage_type VARCHAR(40) NOT NULL,
    display_order INT UNSIGNED NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (quiz_id, media_asset_id),
    CONSTRAINT fk_quiz_media_quiz
        FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_quiz_media_asset
        FOREIGN KEY (media_asset_id) REFERENCES media_assets(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_quiz_media_order (quiz_id, usage_type, display_order)
) ENGINE=InnoDB COMMENT='Cover or explanatory media attached to a quiz.';

-- APP INVARIANT: TRANSITION requires a non-null target, INFO requires no
-- target, and source/target must differ. The media service also verifies both
-- assets are PANORAMA_360 media on the same place and prevents overlap. These
-- checks involve FK-action columns and therefore cannot be MySQL CHECKs.
CREATE TABLE panorama_hotspots (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_media_asset_id BIGINT UNSIGNED NOT NULL,
    target_media_asset_id BIGINT UNSIGNED NULL,
    hotspot_type VARCHAR(20) NOT NULL,
    yaw_degrees DECIMAL(8,4) NOT NULL,
    pitch_degrees DECIMAL(8,4) NOT NULL,
    label VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NULL,
    display_order INT UNSIGNED NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_panorama_hotspots_order UNIQUE (source_media_asset_id, display_order),
    CONSTRAINT chk_panorama_hotspots_type CHECK (hotspot_type IN ('INFO', 'TRANSITION')),
    CONSTRAINT chk_panorama_hotspots_yaw CHECK (yaw_degrees BETWEEN -180.0000 AND 180.0000),
    CONSTRAINT chk_panorama_hotspots_pitch CHECK (pitch_degrees BETWEEN -90.0000 AND 90.0000),
    CONSTRAINT fk_panorama_hotspots_source
        FOREIGN KEY (source_media_asset_id) REFERENCES media_assets(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_panorama_hotspots_target
        FOREIGN KEY (target_media_asset_id) REFERENCES media_assets(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_panorama_hotspots_source_active (source_media_asset_id, is_active, display_order)
) ENGINE=InnoDB COMMENT='Information and transition hotspots for 360-degree panoramas.';
