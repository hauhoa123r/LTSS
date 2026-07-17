-- ============================================================================
-- LOCAL TOURISM SUPPORT SYSTEM (LTSS) - COMPLETE MYSQL 8 SCRIPT
-- Database: ltss
-- Contains: full 49-table schema + complete Son Tay test dataset
-- Test login password: Test@123 (only BCrypt hashes are stored)
--
-- EXECUTION:
--   MySQL Workbench: open this file and execute all statements.
--   CLI: mysql -u root -p --default-character-set=utf8mb4 < ltss_database_mysql8.sql
--
-- RERUN BEHAVIOR:
--   Tables are created only when missing. Seeded/domain data in the selected
--   database is deleted in FK-safe order and replaced with the deterministic
--   dataset. Foundation roles are preserved/upserted.
-- ============================================================================

SET NAMES utf8mb4;
SET time_zone = '+07:00';

CREATE DATABASE IF NOT EXISTS `ltss`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE `ltss`;

-- ============================================================================
-- PART A. COMPLETE SCHEMA
-- ============================================================================
-- Local Tourism Support System (LTSS)
-- Flyway baseline for MySQL 8.0.16+.
-- Database creation and schema selection are provisioning concerns and are intentionally omitted.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS roles (
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

CREATE TABLE IF NOT EXISTS permissions (
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

CREATE TABLE IF NOT EXISTS role_permissions (
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
CREATE TABLE IF NOT EXISTS role_inheritances (
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

CREATE TABLE IF NOT EXISTS users (
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

CREATE TABLE IF NOT EXISTS user_roles (
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

CREATE TABLE IF NOT EXISTS password_history (
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

CREATE TABLE IF NOT EXISTS account_tokens (
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

CREATE TABLE IF NOT EXISTS notifications (
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

CREATE TABLE IF NOT EXISTS audit_logs (
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

CREATE TABLE IF NOT EXISTS search_history (
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

CREATE TABLE IF NOT EXISTS prohibited_terms (
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

CREATE TABLE IF NOT EXISTS place_categories (
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

CREATE TABLE IF NOT EXISTS places (
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

CREATE TABLE IF NOT EXISTS relic_details (
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

CREATE TABLE IF NOT EXISTS businesses (
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

CREATE TABLE IF NOT EXISTS events (
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

CREATE TABLE IF NOT EXISTS article_categories (
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

CREATE TABLE IF NOT EXISTS articles (
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

CREATE TABLE IF NOT EXISTS media_assets (
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

CREATE TABLE IF NOT EXISTS business_posts (
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

CREATE TABLE IF NOT EXISTS tags (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tag_name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_tags_name UNIQUE (tag_name),
    CONSTRAINT uq_tags_slug UNIQUE (slug),
    CONSTRAINT chk_tags_name_not_blank CHECK (CHAR_LENGTH(TRIM(tag_name)) > 0)
) ENGINE=InnoDB COMMENT='Reusable tags for business posts.';

CREATE TABLE IF NOT EXISTS business_post_tags (
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

CREATE TABLE IF NOT EXISTS promotions (
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
CREATE TABLE IF NOT EXISTS tours (
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

CREATE TABLE IF NOT EXISTS tour_items (
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

CREATE TABLE IF NOT EXISTS favorites (
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

CREATE TABLE IF NOT EXISTS quizzes (
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

CREATE TABLE IF NOT EXISTS questions (
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

CREATE TABLE IF NOT EXISTS answers (
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

CREATE TABLE IF NOT EXISTS badges (
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

CREATE TABLE IF NOT EXISTS quiz_badges (
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

CREATE TABLE IF NOT EXISTS quiz_attempts (
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

CREATE TABLE IF NOT EXISTS quiz_attempt_answers (
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

CREATE TABLE IF NOT EXISTS user_badges (
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
CREATE TABLE IF NOT EXISTS reviews (
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

CREATE TABLE IF NOT EXISTS review_replies (
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

CREATE TABLE IF NOT EXISTS engagement_event_types (
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
CREATE TABLE IF NOT EXISTS engagement_events (
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
CREATE TABLE IF NOT EXISTS moderation_records (
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

CREATE TABLE IF NOT EXISTS place_media (
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

CREATE TABLE IF NOT EXISTS event_media (
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

CREATE TABLE IF NOT EXISTS article_media (
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

CREATE TABLE IF NOT EXISTS business_post_media (
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

CREATE TABLE IF NOT EXISTS promotion_media (
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

CREATE TABLE IF NOT EXISTS tour_media (
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

CREATE TABLE IF NOT EXISTS review_media (
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

CREATE TABLE IF NOT EXISTS quiz_media (
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
CREATE TABLE IF NOT EXISTS panorama_hotspots (
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
-- ============================================================================
-- PART B. COMPLETE SON TAY TEST DATA
-- ============================================================================
-- LTSS large deterministic test dataset for MySQL 8.0.16+
-- Scope: tourism in Son Tay, Ha Noi. Run after V1 (and optionally V2).
-- Test password for every seeded account: Test@123
-- BCrypt hash only; the plain password is never stored in password_hash.

SET NAMES utf8mb4;
SET time_zone = '+07:00';

-- =========================================================
-- 00. RERUN-SAFE CLEANUP (children before parents)
-- =========================================================
DELETE FROM panorama_hotspots WHERE 1 = 1;
DELETE FROM quiz_media WHERE 1 = 1;
DELETE FROM review_media WHERE 1 = 1;
DELETE FROM tour_media WHERE 1 = 1;
DELETE FROM promotion_media WHERE 1 = 1;
DELETE FROM business_post_media WHERE 1 = 1;
DELETE FROM article_media WHERE 1 = 1;
DELETE FROM event_media WHERE 1 = 1;
DELETE FROM place_media WHERE 1 = 1;
DELETE FROM moderation_records WHERE 1 = 1;
DELETE FROM engagement_events WHERE 1 = 1;
DELETE FROM engagement_event_types WHERE 1 = 1;
DELETE FROM review_replies WHERE 1 = 1;
DELETE FROM reviews WHERE 1 = 1;
DELETE FROM user_badges WHERE 1 = 1;
DELETE FROM quiz_attempt_answers WHERE 1 = 1;
DELETE FROM quiz_attempts WHERE 1 = 1;
DELETE FROM quiz_badges WHERE 1 = 1;
DELETE FROM answers WHERE 1 = 1;
DELETE FROM questions WHERE 1 = 1;
DELETE FROM badges WHERE 1 = 1;
DELETE FROM quizzes WHERE 1 = 1;
DELETE FROM favorites WHERE 1 = 1;
DELETE FROM tour_items WHERE 1 = 1;
UPDATE tours SET source_tour_id = NULL WHERE source_tour_id IS NOT NULL;
DELETE FROM tours WHERE 1 = 1;
DELETE FROM business_post_tags WHERE 1 = 1;
DELETE FROM tags WHERE 1 = 1;
DELETE FROM promotions WHERE 1 = 1;
DELETE FROM business_posts WHERE 1 = 1;
DELETE FROM media_assets WHERE 1 = 1;
DELETE FROM articles WHERE 1 = 1;
DELETE FROM article_categories WHERE 1 = 1;
DELETE FROM events WHERE 1 = 1;
DELETE FROM businesses WHERE 1 = 1;
DELETE FROM relic_details WHERE 1 = 1;
DELETE FROM places WHERE 1 = 1;
DELETE FROM place_categories WHERE 1 = 1;
DELETE FROM prohibited_terms WHERE 1 = 1;
DELETE FROM search_history WHERE 1 = 1;
DELETE FROM audit_logs WHERE 1 = 1;
DELETE FROM notifications WHERE 1 = 1;
DELETE FROM account_tokens WHERE 1 = 1;
DELETE FROM password_history WHERE 1 = 1;
DELETE FROM user_roles WHERE 1 = 1;
UPDATE users SET deactivated_by_user_id = NULL WHERE deactivated_by_user_id IS NOT NULL;
DELETE FROM users WHERE 1 = 1;
DELETE FROM role_permissions WHERE 1 = 1;
DELETE FROM permissions WHERE 1 = 1;

-- Foundation roles are preserved/upserted, including when V2 already ran.
INSERT INTO roles (role_code, role_name, description, is_active) VALUES
('TOURIST', 'Tourist', 'Khach du lich su dung LTSS', TRUE),
('BUSINESS_OWNER', 'Business Owner', 'Chu co so du lich tai Son Tay', TRUE),
('RELIC_MANAGER', 'Relic Manager', 'Can bo quan ly di tich Son Tay', TRUE),
('MODERATOR', 'Moderator', 'Kiem duyet noi dung du lich', TRUE),
('ADMINISTRATOR', 'Administrator', 'Quan tri he thong LTSS', TRUE)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description), is_active = TRUE;

INSERT IGNORE INTO role_inheritances (role_id, inherited_role_id)
SELECT c.id, p.id FROM roles c JOIN roles p
WHERE c.role_code = 'BUSINESS_OWNER' AND p.role_code = 'TOURIST';
INSERT IGNORE INTO role_inheritances (role_id, inherited_role_id)
SELECT c.id, p.id FROM roles c JOIN roles p
WHERE c.role_code = 'ADMINISTRATOR' AND p.role_code = 'MODERATOR';

DROP PROCEDURE IF EXISTS seed_ltss_son_tay;
DELIMITER $$
CREATE PROCEDURE seed_ltss_son_tay()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE j INT DEFAULT 0;
    DECLARE k INT DEFAULT 0;
    DECLARE v_base BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_users BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_places BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_businesses BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_events BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_articles BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_media BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_posts BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_promotions BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_tours BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_quizzes BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_questions BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_answers BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_badges BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_attempts BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_reviews BIGINT UNSIGNED DEFAULT 0;
    DECLARE v_status VARCHAR(30);
    DECLARE v_started DATETIME(6);
    DECLARE v_correct INT;
    DECLARE v_selected INT;

    -- =========================================================
    -- 01. SECURITY AND RBAC
    -- =========================================================
    INSERT INTO permissions (permission_code, permission_name, description) VALUES
    ('PLACE_VIEW','Xem dia diem','Xem diem den cong khai'),('PLACE_FAVORITE','Luu dia diem','Quan ly dia diem yeu thich'),
    ('PLACE_CREATE','Tao dia diem','Tao ho so diem den'),('PLACE_EDIT','Sua dia diem','Cap nhat ho so diem den'),
    ('PLACE_PUBLISH','Xuat ban dia diem','Duyet va xuat ban diem den'),('PLACE_DELETE','Xoa dia diem','Xoa mem diem den'),
    ('BUSINESS_VIEW','Xem doanh nghiep','Xem ho so co so'),('BUSINESS_CREATE','Tao doanh nghiep','Dang ky co so dia phuong'),
    ('BUSINESS_EDIT','Sua doanh nghiep','Cap nhat co so'),('BUSINESS_APPROVE','Duyet doanh nghiep','Duyet ho so kinh doanh'),
    ('EVENT_VIEW','Xem su kien','Xem lich su kien'),('EVENT_CREATE','Tao su kien','Tao su kien Son Tay'),
    ('EVENT_EDIT','Sua su kien','Cap nhat su kien'),('EVENT_PUBLISH','Xuat ban su kien','Duyet su kien'),
    ('ARTICLE_VIEW','Xem bai viet','Doc bai viet'),('ARTICLE_CREATE','Tao bai viet','Viet bai van hoa'),
    ('ARTICLE_EDIT','Sua bai viet','Bien tap bai viet'),('ARTICLE_PUBLISH','Xuat ban bai viet','Duyet bai viet'),
    ('POST_VIEW','Xem bai doanh nghiep','Xem tin co so'),('POST_CREATE','Tao bai doanh nghiep','Dang tin co so'),
    ('POST_EDIT','Sua bai doanh nghiep','Cap nhat tin co so'),('POST_PUBLISH','Xuat ban bai doanh nghiep','Duyet tin co so'),
    ('PROMOTION_VIEW','Xem khuyen mai','Xem uu dai'),('PROMOTION_CREATE','Tao khuyen mai','Tao uu dai dia phuong'),
    ('PROMOTION_EDIT','Sua khuyen mai','Cap nhat uu dai'),('PROMOTION_PUBLISH','Xuat ban khuyen mai','Duyet uu dai'),
    ('TOUR_VIEW','Xem tour','Xem lich trinh'),('TOUR_CREATE','Tao tour','Tao lich trinh ca nhan'),
    ('TOUR_COPY','Sao chep tour','Sao chep lich trinh'),('TOUR_PUBLISH','Chia se tour','Cong khai lich trinh'),
    ('QUIZ_VIEW','Xem quiz','Xem cau hoi'),('QUIZ_PLAY','Lam quiz','Tham gia thu thach'),
    ('QUIZ_CREATE','Tao quiz','Soan bo cau hoi'),('QUIZ_PUBLISH','Xuat ban quiz','Duyet bo cau hoi'),
    ('REVIEW_CREATE','Tao danh gia','Danh gia dich vu'),('REVIEW_REPLY','Phan hoi danh gia','Tra loi chinh thuc'),
    ('REVIEW_MODERATE','Duyet danh gia','Kiem duyet danh gia'),('MEDIA_UPLOAD','Tai media','Tai anh am thanh video'),
    ('MEDIA_MANAGE','Quan ly media','Quan ly tai san so'),('PANORAMA_MANAGE','Quan ly panorama','Cau hinh diem tuong tac'),
    ('MODERATION_VIEW','Xem hang doi','Xem noi dung cho duyet'),('MODERATION_DECIDE','Xu ly kiem duyet','Chap thuan hoac tu choi'),
    ('ANALYTICS_VIEW','Xem thong ke','Xem dashboard du lich'),('USER_VIEW','Xem nguoi dung','Tra cuu tai khoan'),
    ('USER_MANAGE','Quan ly nguoi dung','Cap nhat trang thai tai khoan'),('ROLE_MANAGE','Quan ly vai tro','Gan va thu hoi vai tro'),
    ('AUDIT_VIEW','Xem nhat ky','Tra cuu nhat ky he thong'),('CONFIG_MANAGE','Quan ly cau hinh','Quan ly danh muc he thong'),
    ('NOTIFICATION_VIEW','Xem thong bao','Doc thong bao ca nhan'),('PROFILE_EDIT','Sua ho so','Cap nhat ho so ca nhan');

    INSERT INTO role_permissions (role_id, permission_id)
    SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
    WHERE r.role_code = 'ADMINISTRATOR'
       OR (r.role_code = 'TOURIST' AND p.permission_code IN ('PLACE_VIEW','PLACE_FAVORITE','BUSINESS_VIEW','EVENT_VIEW','ARTICLE_VIEW','POST_VIEW','PROMOTION_VIEW','TOUR_VIEW','TOUR_CREATE','TOUR_COPY','QUIZ_VIEW','QUIZ_PLAY','REVIEW_CREATE','NOTIFICATION_VIEW','PROFILE_EDIT'))
       OR (r.role_code = 'BUSINESS_OWNER' AND (p.permission_code LIKE 'BUSINESS_%' OR p.permission_code LIKE 'POST_%' OR p.permission_code LIKE 'PROMOTION_%' OR p.permission_code IN ('MEDIA_UPLOAD','REVIEW_REPLY','ANALYTICS_VIEW')))
       OR (r.role_code = 'RELIC_MANAGER' AND (p.permission_code LIKE 'PLACE_%' OR p.permission_code LIKE 'EVENT_%' OR p.permission_code LIKE 'ARTICLE_%' OR p.permission_code LIKE 'QUIZ_%' OR p.permission_code IN ('MEDIA_UPLOAD','MEDIA_MANAGE','PANORAMA_MANAGE','ANALYTICS_VIEW')))
       OR (r.role_code = 'MODERATOR' AND (p.permission_code LIKE 'MODERATION_%' OR p.permission_code LIKE '%_PUBLISH' OR p.permission_code IN ('REVIEW_MODERATE','ANALYTICS_VIEW','AUDIT_VIEW','NOTIFICATION_VIEW')));

    -- =========================================================
    -- 02. USERS AND ACCOUNT DATA
    -- =========================================================
    SET i = 1;
    WHILE i <= 120 DO
        SET v_status = IF(i >= 117,'ACTIVE',ELT(MOD(i - 1, 10) + 1,'ACTIVE','ACTIVE','ACTIVE','ACTIVE','ACTIVE','ACTIVE','PENDING_VERIFICATION','SUSPENDED','DEACTIVATED','DELETED'));
        INSERT INTO users(full_name,display_name,email,password_hash,phone,avatar_url,address,status,
            email_verified_at,failed_login_count,locked_until,last_login_at,password_changed_at,deactivated_at,
            policy_version,policy_accepted_at,created_at)
        VALUES(
            CONCAT(ELT(MOD(i-1,12)+1,'Nguyen Minh','Tran Thu','Le Quang','Pham Ngoc','Hoang Anh','Doan Duc','Bui Thanh','Dang Ha','Vu Bao','Ngo Phuong','Duong Tuan','Khuat Mai'),' ',LPAD(i,3,'0')),
            CONCAT(ELT(MOD(i-1,10)+1,'Minh An','Thu Ha','Quang Huy','Ngoc Lan','Anh Khoa','Duc Manh','Thanh Tam','Ha Linh','Bao Chau','Phuong Nam'),' ',LPAD(i,3,'0')),
            CONCAT(CASE WHEN i <= 40 THEN 'tourist' WHEN i <= 100 THEN 'owner' WHEN i <= 108 THEN 'relic' WHEN i <= 116 THEN 'moderator' ELSE 'admin' END,LPAD(i,3,'0'),'@ltss.local'),
            '$2a$10$VTSumtVCSKxBHlKQA4F3U.JpuitXtEh8iCgrU17IFBqlmiicZeLt6',
            CONCAT('09',LPAD(10000000+i,8,'0')),
            CONCAT('https://cdn.ltss.local/son-tay/users/',LPAD(i,3,'0'),'/avatar.jpg'),
            CONCAT(ELT(MOD(i-1,10)+1,'Pho Quang Trung','Pho Le Loi','Pho Phung Khac Khoan','Pho Pho Duc Chinh','Mong Phu - Duong Lam','Dong Sang - Duong Lam','Trung Hung','Vien Son','Son Loc','Thanh My'),', Son Tay, Ha Noi'),
            v_status,
            IF(v_status='PENDING_VERIFICATION',NULL,TIMESTAMP('2025-01-05 08:00:00') + INTERVAL i DAY),
            MOD(i,6),IF(v_status='SUSPENDED' OR MOD(i,17)=0,'2026-08-01 08:00:00',NULL),
            IF(v_status='PENDING_VERIFICATION',NULL,TIMESTAMP('2026-06-01 07:00:00') + INTERVAL i HOUR),
            TIMESTAMP('2025-02-01 09:00:00') + INTERVAL i DAY,
            IF(v_status IN ('DEACTIVATED','DELETED'),TIMESTAMP('2026-05-01 10:00:00') + INTERVAL i HOUR,NULL),
            IF(v_status='PENDING_VERIFICATION',NULL,'2026.1'),IF(v_status='PENDING_VERIFICATION',NULL,'2026-01-01 08:00:00'),
            TIMESTAMP('2024-08-01 08:00:00') + INTERVAL i DAY);
        IF i = 1 THEN SET v_users = LAST_INSERT_ID(); END IF;
        SET i = i + 1;
    END WHILE;

    UPDATE users SET deactivated_by_user_id = v_users + 119 WHERE status IN ('DEACTIVATED','DELETED');

    INSERT INTO user_roles(user_id,role_id,is_active,assigned_by_user_id,assigned_at)
    SELECT v_users+n.i-1,r.id,TRUE,v_users+119,'2025-01-10 08:00:00'
    FROM (SELECT 1 i UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) d1
    CROSS JOIN (SELECT 0 o UNION SELECT 10 UNION SELECT 20 UNION SELECT 30 UNION SELECT 40 UNION SELECT 50 UNION SELECT 60 UNION SELECT 70 UNION SELECT 80 UNION SELECT 90 UNION SELECT 100 UNION SELECT 110) d2
    CROSS JOIN roles r
    CROSS JOIN LATERAL (SELECT d1.i+d2.o i) n
    WHERE n.i <= 120 AND r.role_code='TOURIST';

    INSERT INTO user_roles(user_id,role_id,is_active,assigned_by_user_id,assigned_at)
    SELECT v_users+x.i-1,r.id,TRUE,v_users+119,'2025-01-10 08:00:00' FROM roles r
    JOIN (SELECT 41 i UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60 UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65 UNION SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70 UNION SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74 UNION SELECT 75 UNION SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79 UNION SELECT 80 UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84 UNION SELECT 85 UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90 UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95 UNION SELECT 96 UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100) x
    WHERE r.role_code='BUSINESS_OWNER';
    INSERT INTO user_roles(user_id,role_id,is_active,assigned_by_user_id) SELECT v_users+x.i-1,r.id,TRUE,v_users+119 FROM roles r JOIN (SELECT 101 i UNION SELECT 102 UNION SELECT 103 UNION SELECT 104 UNION SELECT 105 UNION SELECT 106 UNION SELECT 107 UNION SELECT 108) x WHERE r.role_code='RELIC_MANAGER';
    INSERT INTO user_roles(user_id,role_id,is_active,assigned_by_user_id) SELECT v_users+x.i-1,r.id,TRUE,v_users+119 FROM roles r JOIN (SELECT 109 i UNION SELECT 110 UNION SELECT 111 UNION SELECT 112 UNION SELECT 113 UNION SELECT 114 UNION SELECT 115 UNION SELECT 116) x WHERE r.role_code='MODERATOR';
    INSERT INTO user_roles(user_id,role_id,is_active,assigned_by_user_id) SELECT v_users+x.i-1,r.id,TRUE,v_users+119 FROM roles r JOIN (SELECT 117 i UNION SELECT 118 UNION SELECT 119 UNION SELECT 120) x WHERE r.role_code='ADMINISTRATOR';

    SET i=1; WHILE i<=300 DO
        INSERT INTO password_history(user_id,password_hash,change_reason,created_at) VALUES(v_users+MOD(i-1,120),ELT(MOD(i-1,4)+1,'$2a$10$h3GHwl0VM.4jA0R7UawnDu8Av0DApScDNU6I2REFGMcylsb11RWJG','$2a$10$OiXe5jp4KDVn5A0tblZSA.2MNx2A/436UifcgAIj1eDSlSueJTagK','$2a$10$8aGh18mP4TWEyEuLCehdr.hcpR398IUwbtsiAhvFzxOtN1LQ6Mc4S','$2a$10$uTSUtV3qxWWJt4ONzSY5tOZMSYFjdBJTrZvBj9S3unOFPlNxtrWAq'),ELT(MOD(i-1,4)+1,'REGISTRATION','USER_CHANGE','PASSWORD_RESET','ADMIN_RESET'),TIMESTAMP('2024-08-01 08:00:00')+INTERVAL i DAY);
        SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=200 DO
        INSERT INTO account_tokens(user_id,token_type,token_hash,expires_at,used_at,revoked_at,created_ip,created_at) VALUES(v_users+MOD(i-1,120),ELT(MOD(i-1,4)+1,'EMAIL_VERIFICATION','PASSWORD_RESET','REFRESH_TOKEN','CHANGE_PASSWORD_OTP'),SHA2(CONCAT('ltss-token-',i),256),TIMESTAMP('2026-01-01 08:00:00')+INTERVAL (i+30) DAY,IF(MOD(i,4)=0,TIMESTAMP('2026-01-02 09:00:00')+INTERVAL i DAY,NULL),IF(MOD(i,7)=0,TIMESTAMP('2026-01-03 09:00:00')+INTERVAL i DAY,NULL),CONCAT('10.20.',MOD(i,250),'.',MOD(i*7,250)),TIMESTAMP('2026-01-01 08:00:00')+INTERVAL i DAY);
        SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=400 DO
        INSERT INTO notifications(recipient_user_id,title,message,notification_type,action_url,is_read,read_at,created_at) VALUES(v_users+MOD(i-1,120),ELT(MOD(i-1,5)+1,'Goi y tham quan Son Tay','Noi dung dang cho duyet','Lich le hoi sap dien ra','Uu dai am thuc Xu Doai','Huy hieu moi'),ELT(MOD(i-1,5)+1,'Kham pha Thanh co va Lang co Duong Lam trong lich trinh cuoi tuan.','Ho so du lich cua ban da duoc tiep nhan.','Le hoi dia phuong sap dien ra, hay xem lich chi tiet.','Mot uu dai phu hop dang cho ban tai Son Tay.','Ban da dat thanh tich trong quiz van hoa Son Tay.'),ELT(MOD(i-1,5)+1,'SYSTEM','MODERATION','EVENT','PROMOTION','BADGE'),CONCAT('/notifications/',i),MOD(i,3)=0,IF(MOD(i,3)=0,TIMESTAMP('2026-03-01 09:00:00')+INTERVAL i HOUR,NULL),TIMESTAMP('2026-02-01 08:00:00')+INTERVAL i HOUR);
        SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=800 DO
        INSERT INTO audit_logs(actor_user_id,action_code,entity_type,entity_id,old_values,new_values,ip_address,user_agent,request_id,created_at) VALUES(IF(MOD(i,9)=0,NULL,v_users+MOD(i-1,120)),ELT(MOD(i-1,8)+1,'LOGIN_SUCCESS','PLACE_VIEW','CONTENT_SUBMIT','MODERATION_DECIDE','TOUR_COPY','QUIZ_SUBMIT','REVIEW_CREATE','PROFILE_UPDATE'),ELT(MOD(i-1,7)+1,'USER','PLACE','ARTICLE','EVENT','TOUR','QUIZ','REVIEW'),i,JSON_OBJECT('status','PENDING'),JSON_OBJECT('status',ELT(MOD(i-1,3)+1,'ACTIVE','PUBLISHED','RESOLVED')),CONCAT('172.16.',MOD(i,250),'.',MOD(i*3,250)),'Mozilla/5.0 LTSS QA',CONCAT('req-son-tay-',LPAD(i,6,'0')),TIMESTAMP('2025-07-01 00:00:00')+INTERVAL MOD(i*13,380) DAY+INTERVAL MOD(i*17,24) HOUR);
        SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=600 DO
        INSERT INTO search_history(user_id,keyword,normalized_keyword,searched_at) VALUES(v_users+MOD(i-1,120),ELT(MOD(FLOOR((i-1)/120),10)+1,'Thanh co Son Tay','Lang co Duong Lam','Chua Mia','Den Va','Ho Dong Mo','am thuc Xu Doai','homestay Duong Lam','tour xe dap','le hoi Son Tay','nha hang ga Mia'),CONCAT('son-tay-search-',LPAD(MOD(FLOOR((i-1)/120),10)+1,2,'0')),TIMESTAMP('2026-01-01 08:00:00')+INTERVAL i HOUR);
        SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=60 DO
        INSERT INTO prohibited_terms(term,normalized_term,severity,is_active,created_by_user_id) VALUES(CONCAT(ELT(MOD(i-1,6)+1,'noi dung xuc pham','quang cao lua dao','spam dat tour','gia mao huong dan vien','rao ban trai phep','tu ngu khong phu hop'),' ',LPAD(i,2,'0')),CONCAT('prohibited-son-tay-',LPAD(i,2,'0')),IF(MOD(i,4)=0,'WARN','BLOCK'),MOD(i,9)<>0,v_users+118);
        SET i=i+1;
    END WHILE;

    -- =========================================================
    -- 03. PLACES, CATEGORIES AND BUSINESSES
    -- =========================================================
    INSERT INTO place_categories(category_name,slug,description,marker_icon_key,created_by_user_id) VALUES
    ('Di tích lịch sử','di-tich-lich-su','Thành cổ, làng cổ và di tích lịch sử Sơn Tây','relic',v_users+100),
    ('Đình đền','dinh-den','Không gian tín ngưỡng Xứ Đoài','temple',v_users+100),
    ('Chùa','chua','Chùa cổ và điểm hành hương','pagoda',v_users+100),
    ('Cảnh quan sinh thái','canh-quan-sinh-thai','Hồ, đồi và không gian xanh','nature',v_users+100),
    ('Bảo tàng văn hóa','bao-tang-van-hoa','Không gian trưng bày văn hóa','museum',v_users+100),
    ('Nhà hàng ẩm thực','nha-hang-am-thuc','Ẩm thực Sơn Tây và Xứ Đoài','restaurant',v_users+100),
    ('Khách sạn','khach-san','Lưu trú tại Sơn Tây','hotel',v_users+100),
    ('Homestay','homestay','Lưu trú gần làng cổ','homestay',v_users+100),
    ('Quán cà phê','quan-ca-phe','Điểm dừng chân và check-in','cafe',v_users+100),
    ('Khu nghỉ dưỡng','khu-nghi-duong','Nghỉ dưỡng ven hồ Đồng Mô','resort',v_users+100),
    ('Cửa hàng lưu niệm','cua-hang-luu-niem','Sản phẩm địa phương và OCOP','gift',v_users+100),
    ('Dịch vụ du lịch','dich-vu-du-lich','Hướng dẫn, thuê xe và thông tin','service',v_users+100),
    ('Bãi đỗ xe','bai-do-xe','Bãi đỗ xe phục vụ du khách','parking',v_users+100),
    ('Điểm check-in','diem-check-in','Không gian chụp ảnh Sơn Tây','camera',v_users+100),
    ('Chợ địa phương','cho-dia-phuong','Chợ truyền thống và đặc sản','market',v_users+100);
    SET v_base=LAST_INSERT_ID();

    SET i=1; WHILE i<=200 DO
        SET v_status=ELT(MOD(i-1,12)+1,'PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','DRAFT','PENDING','REJECTED','ARCHIVED','DELETED','PUBLISHED');
        INSERT INTO places(category_id,created_by_user_id,updated_by_user_id,name,slug,summary,description,address,latitude,longitude,opening_hours,entrance_fee,contact_phone,status,submitted_at,published_at,deleted_at,created_at)
        VALUES(v_base+MOD(i-1,15),v_users+100+MOD(i,8),v_users+108+MOD(i,8),
          CASE i
            WHEN 1 THEN 'Thành cổ Sơn Tây' WHEN 2 THEN 'Làng cổ Đường Lâm' WHEN 3 THEN 'Đình Mông Phụ'
            WHEN 4 THEN 'Cổng làng Mông Phụ' WHEN 5 THEN 'Chùa Mía' WHEN 6 THEN 'Đền Và'
            WHEN 7 THEN 'Văn Miếu Sơn Tây' WHEN 8 THEN 'Lăng Ngô Quyền' WHEN 9 THEN 'Đền thờ Ngô Quyền'
            WHEN 10 THEN 'Đền thờ Phùng Hưng' WHEN 11 THEN 'Chùa Khai Nguyên' WHEN 12 THEN 'Hồ Đồng Mô'
            WHEN 13 THEN 'Khu du lịch Đồng Mô' WHEN 14 THEN 'Làng Văn hóa - Du lịch các dân tộc Việt Nam'
            WHEN 15 THEN 'Hồ Xuân Khanh' WHEN 16 THEN 'Nhà cổ ông Hùng tại Mông Phụ'
            WHEN 17 THEN 'Nhà cổ bà Điền tại Đường Lâm' WHEN 18 THEN 'Đình Đoài Giáp'
            WHEN 19 THEN 'Đình Phù Sa' WHEN 20 THEN 'Chùa Trì' WHEN 21 THEN 'Đền Măng Sơn'
            WHEN 22 THEN 'Nhà thờ Thám hoa Giang Văn Minh' WHEN 23 THEN 'Chợ Mía Đường Lâm'
            WHEN 24 THEN 'Phố đi bộ Thành cổ Sơn Tây' WHEN 25 THEN 'Bếp Làng Đường Lâm'
            WHEN 26 THEN 'Kẹo lạc Hiền Bao' WHEN 27 THEN 'Kẹo lạc Quý Thảo' WHEN 28 THEN 'Thịt quay đòn Hương Lương'
            ELSE CONCAT(ELT(MOD(i-29,12)+1,'Homestay đá ong','Nhà hàng gà Mía','Quán cà phê Thành cổ','Cửa hàng quà Xứ Đoài','Dịch vụ xe đạp Đường Lâm','Trạm thông tin du lịch','Bãi đỗ xe du lịch','Vườn trải nghiệm Kim Sơn','Nhà hàng ven Đền Và','Khách sạn Sơn Tây','Khu nghỉ dưỡng Đồng Mô','Điểm check-in đá ong'),' ',ELT(MOD(i-29,10)+1,'Mông Phụ','Đông Sàng','Cam Lâm','Đoài Giáp','Trung Hưng','Viên Sơn','Sơn Lộc','Thanh Mỹ','Cổ Đông','Kim Sơn'),' ',LPAD(i-28,3,'0')) END,
          CONCAT('son-tay-',LPAD(i,3,'0'),'-',ELT(MOD(i-1,8)+1,'di-san','duong-lam','xu-doai','dong-mo','am-thuc','luu-tru','trai-nghiem','dich-vu')),
          CONCAT('Điểm đến ',ELT(MOD(i-1,8)+1,'di sản','tâm linh','sinh thái','ẩm thực','lưu trú','mua sắm','check-in','dịch vụ'),' phục vụ hành trình khám phá Sơn Tây.'),
          'Không gian mang nét đặc trưng Xứ Đoài, phù hợp cho du khách tìm hiểu lịch sử, văn hóa và đời sống địa phương. Thông tin được xây dựng cho hệ thống hỗ trợ du lịch Sơn Tây.',
          CONCAT(10+MOD(i*7,190),' ',ELT(MOD(i-1,12)+1,'pho Quang Trung','pho Le Loi','pho Phung Khac Khoan','pho Pho Duc Chinh','Mong Phu - Duong Lam','Dong Sang - Duong Lam','Cam Lam - Duong Lam','Trung Hung','Vien Son','Son Loc','Thanh My','Dong Mo'),', Son Tay, Ha Noi'),
          21.1000000+MOD(i*37,1300)/100000.0,105.4000000+MOD(i*53,1300)/100000.0,
          ELT(MOD(i-1,5)+1,'07:00-17:30 hằng ngày','08:00-22:00 hằng ngày','06:30-18:00 hằng ngày','08:00-17:00, nghỉ thứ Hai','Mở cửa cả ngày'),
          CASE WHEN MOD(i,5)=0 THEN 100000 WHEN MOD(i,3)=0 THEN 20000 ELSE 0 END,
          CONCAT('09',LPAD(20000000+i,8,'0')),v_status,
          IF(v_status IN ('PENDING','PUBLISHED','REJECTED','ARCHIVED','DELETED'),'2025-06-01 08:00:00',NULL),
          IF(v_status='PUBLISHED','2025-06-05 08:00:00',NULL),IF(v_status='DELETED','2026-01-10 08:00:00',NULL),
          TIMESTAMP('2025-01-01 08:00:00')+INTERVAL i DAY);
        IF i=1 THEN SET v_places=LAST_INSERT_ID(); END IF;
        SET i=i+1;
    END WHILE;

    SET i=1; WHILE i<=50 DO
      INSERT INTO relic_details(place_id,historical_period,history,architecture,recognition_level,recognized_at,preservation_note)
      VALUES(v_places+i-1,ELT(MOD(i-1,5)+1,'The ky IX','The ky XVII','Thoi Nguyen','Thoi Phap thuoc','Nhieu giai doan lich su'),CONCAT('Di tich gan voi lich su Son Tay va van hoa Xu Doai, la noi luu giu ky uc cong dong qua nhieu the he. Ho so ',i,' duoc dung de kiem thu noi dung di san.'),CONCAT('Cong trinh su dung ',ELT(MOD(i-1,4)+1,'da ong va go lim','ket cau thanh co bang da ong','bo vi keo truyen thong','san vuon lang Viet'),' hai hoa voi canh quan Son Tay.'),ELT(MOD(i-1,3)+1,'Quoc gia','Cap thanh pho','Kiem ke di tich'),'2005-11-28','Can tiep tuc bao ton vat lieu goc, giu gin canh quan va huong dan khach tham quan van minh.');
      SET i=i+1;
    END WHILE;

    SET i=1; WHILE i<=60 DO
      INSERT INTO businesses(place_id,owner_user_id,registration_number,contact_email,website_url,status,approved_by_user_id,approved_at,created_at)
      VALUES(v_places+69+i,v_users+39+i,CONCAT('ST-',DATE_FORMAT('2025-01-01','%Y'),'-',LPAD(i,5,'0')),CONCAT('coso',LPAD(i,3,'0'),'@sontay.ltss.local'),CONCAT('https://dulichsontay.local/co-so/',LPAD(i,3,'0')),ELT(MOD(i-1,10)+1,'ACTIVE','ACTIVE','ACTIVE','ACTIVE','ACTIVE','ACTIVE','PENDING','REJECTED','SUSPENDED','INACTIVE'),IF(MOD(i-1,10)<6,v_users+116,NULL),IF(MOD(i-1,10)<6,'2025-03-15 09:00:00',NULL),TIMESTAMP('2025-02-01 08:00:00')+INTERVAL i DAY);
      IF i=1 THEN SET v_businesses=LAST_INSERT_ID(); END IF;
      SET i=i+1;
    END WHILE;

    -- =========================================================
    -- 04. EVENTS AND ARTICLES
    -- =========================================================
    SET i=1; WHILE i<=120 DO
      SET v_status=ELT(MOD(i-1,14)+1,'PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','DRAFT','PENDING','REJECTED','CANCELLED','ARCHIVED','DELETED','PUBLISHED','PUBLISHED','PUBLISHED');
      INSERT INTO events(place_id,created_by_user_id,updated_by_user_id,title,slug,description,start_at,end_at,location_note,status,submitted_at,published_at,deleted_at,created_at)
      VALUES(v_places+MOD(i-1,30),v_users+100+MOD(i,16),v_users+108+MOD(i,8),CONCAT(ELT(MOD(i-1,10)+1,'Lễ hội Đền Và','Lễ hội Làng cổ Đường Lâm','Hội làng Mông Phụ','Festival Văn hóa Xứ Đoài','Tuần lễ Du lịch Sơn Tây','Hội chợ OCOP Sơn Tây','Triển lãm ảnh Sơn Tây','Chương trình nghệ thuật dân gian','Giải chạy khám phá Thành cổ','Ngày hội ẩm thực gà Mía'),' ',YEAR('2025-01-01'+INTERVAL i DAY),'-',LPAD(i,3,'0')),CONCAT('su-kien-son-tay-',LPAD(i,3,'0')),'Hoạt động cộng đồng giới thiệu di sản, ẩm thực và nét đẹp Xứ Đoài đến du khách. Chương trình diễn ra tại Sơn Tây với thông tin lịch trình rõ ràng.',TIMESTAMP('2025-08-01 07:00:00')+INTERVAL MOD(i*9,365) DAY,TIMESTAMP('2025-08-01 07:00:00')+INTERVAL MOD(i*9,365) DAY+INTERVAL (4+MOD(i,20)) HOUR,CONCAT(ELT(MOD(i-1,6)+1,'Thành cổ Sơn Tây','Làng cổ Đường Lâm','Đền Và','Hồ Đồng Mô','Phố đi bộ Sơn Tây','Làng Văn hóa các dân tộc'),', Sơn Tây, Hà Nội'),v_status,IF(v_status IN ('PENDING','PUBLISHED','REJECTED','ARCHIVED','DELETED'),'2025-07-01 08:00:00',NULL),IF(v_status='PUBLISHED','2025-07-05 08:00:00',NULL),IF(v_status='DELETED','2026-01-05 08:00:00',NULL),TIMESTAMP('2025-06-01 08:00:00')+INTERVAL i HOUR);
      IF i=1 THEN SET v_events=LAST_INSERT_ID(); END IF; SET i=i+1;
    END WHILE;

    INSERT INTO article_categories(category_name,slug,description,created_by_user_id) VALUES
    ('Lịch sử Sơn Tây','lich-su-son-tay','Câu chuyện lịch sử địa phương',v_users+108),('Di sản kiến trúc','di-san-kien-truc','Kiến trúc đá ong và làng Việt',v_users+108),('Văn hóa Xứ Đoài','van-hoa-xu-doai','Phong tục và đời sống Xứ Đoài',v_users+108),('Tin sự kiện','tin-su-kien','Tin tức lễ hội Sơn Tây',v_users+108),('Kinh nghiệm du lịch','kinh-nghiem-du-lich','Hướng dẫn tham quan',v_users+108),('Ẩm thực Sơn Tây','am-thuc-son-tay','Gà Mía, chè lam và đặc sản',v_users+108),('Nhân vật lịch sử','nhan-vat-lich-su','Ngô Quyền, Phùng Hưng và danh nhân',v_users+108),('Du lịch sinh thái','du-lich-sinh-thai','Đồng Mô và Xuân Khanh',v_users+108),('Làng nghề OCOP','lang-nghe-ocop','Sản phẩm địa phương',v_users+108),('Ảnh đẹp Sơn Tây','anh-dep-son-tay','Góc nhìn Sơn Tây',v_users+108),('Lịch trình gợi ý','lich-trinh-goi-y','Tour và tuyến tham quan',v_users+108),('Bảo tồn di sản','bao-ton-di-san','Hoạt động gìn giữ di tích',v_users+108);
    SET v_base=LAST_INSERT_ID();
    SET i=1; WHILE i<=180 DO
      SET v_status=ELT(MOD(i-1,12)+1,'PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','DRAFT','PENDING','REJECTED','ARCHIVED','DELETED','PUBLISHED');
      INSERT INTO articles(category_id,place_id,event_id,author_user_id,updated_by_user_id,title,slug,summary,content,status,submitted_at,published_at,deleted_at,created_at)
      VALUES(v_base+MOD(i-1,12),v_places+MOD(i-1,50),IF(MOD(i,4)=0,v_events+MOD(i-1,120),NULL),v_users+100+MOD(i,16),v_users+108+MOD(i,8),CONCAT(ELT(MOD(i-1,12)+1,'Lịch sử Thành cổ Sơn Tây','Một ngày ở Làng cổ Đường Lâm','Nét đẹp kiến trúc đá ong','Văn hóa Xứ Đoài trong đời sống','Chùa Mía và nghệ thuật tượng cổ','Đền Và và tín ngưỡng Tản Viên','Dấu ấn Ngô Quyền ở Đường Lâm','Câu chuyện Bố Cái Đại Vương Phùng Hưng','Đặc sản gà Mía Sơn Tây','Kinh nghiệm du lịch Sơn Tây','Những điểm tham quan nổi bật','Ẩm thực Sơn Tây nên thử'),' - chuyên đề ',LPAD(i,3,'0')),CONCAT('bai-viet-son-tay-',LPAD(i,3,'0')),'Gợi ý hữu ích cho hành trình tìm hiểu Sơn Tây, di sản Xứ Đoài và đời sống của người dân địa phương.',CONCAT('Bài viết trình bày bằng tiếng Việt tự nhiên về lịch sử, kiến trúc, văn hóa và trải nghiệm du lịch tại Sơn Tây. Du khách nên tôn trọng không gian tín ngưỡng, giữ vệ sinh và ưu tiên dịch vụ địa phương. Nội dung chuyên đề ',i,' cung cấp gợi ý thực tế cho một chuyến đi an toàn.'),v_status,IF(v_status IN ('PENDING','PUBLISHED','REJECTED','ARCHIVED','DELETED'),'2025-08-01 08:00:00',NULL),IF(v_status='PUBLISHED',TIMESTAMP('2025-08-05 08:00:00')+INTERVAL i HOUR,NULL),IF(v_status='DELETED','2026-02-01 08:00:00',NULL),TIMESTAMP('2025-07-01 08:00:00')+INTERVAL i HOUR);
      IF i=1 THEN SET v_articles=LAST_INSERT_ID(); END IF; SET i=i+1;
    END WHILE;

    -- =========================================================
    -- 05. MEDIA AND BUSINESS CONTENT
    -- =========================================================
    SET i=1; WHILE i<=800 DO
      INSERT INTO media_assets(uploaded_by_user_id,media_type,storage_provider,storage_key,media_url,thumbnail_url,mime_type,file_size_bytes,width_px,height_px,duration_seconds,checksum_sha256,created_at)
      VALUES(v_users+MOD(i-1,120),
        CASE WHEN i<=200 THEN 'PANORAMA_360' WHEN i<=600 THEN 'IMAGE' WHEN i<=700 THEN 'AUDIO' ELSE 'VIDEO' END,
        'LTSS_CDN',
        CONCAT('son-tay/',CASE WHEN i<=200 THEN 'panorama/' WHEN i<=600 THEN 'image/' WHEN i<=700 THEN 'audio/' ELSE 'video/' END,LPAD(i,4,'0'),CASE WHEN i<=600 THEN '.jpg' WHEN i<=700 THEN '.mp3' ELSE '.mp4' END),
        CONCAT('https://cdn.ltss.local/son-tay/',CASE WHEN i<=200 THEN 'panorama/' WHEN i<=600 THEN 'image/' WHEN i<=700 THEN 'audio/' ELSE 'video/' END,LPAD(i,4,'0'),CASE WHEN i<=600 THEN '.jpg' WHEN i<=700 THEN '.mp3' ELSE '.mp4' END),
        IF(i<=700,CONCAT('https://cdn.ltss.local/son-tay/thumbnails/',LPAD(i,4,'0'),'.jpg'),NULL),
        CASE WHEN i<=600 THEN 'image/jpeg' WHEN i<=700 THEN 'audio/mpeg' ELSE 'video/mp4' END,
        CASE WHEN i<=200 THEN 12582912+MOD(i,1000000) WHEN i<=600 THEN 1048576+MOD(i,500000) WHEN i<=700 THEN 5242880+MOD(i,500000) ELSE 15728640+MOD(i,1000000) END,
        CASE WHEN i<=200 THEN 8192 WHEN i<=600 THEN 1920 ELSE NULL END,
        CASE WHEN i<=200 THEN 4096 WHEN i<=600 THEN 1080 ELSE NULL END,
        CASE WHEN i<=600 THEN NULL ELSE 45+MOD(i,240) END,
        SHA2(CONCAT('ltss-son-tay-media-',i),256),TIMESTAMP('2025-05-01 08:00:00')+INTERVAL i HOUR);
      IF i=1 THEN SET v_media=LAST_INSERT_ID(); END IF; SET i=i+1;
    END WHILE;

    SET i=1; WHILE i<=180 DO
      SET v_status=ELT(MOD(i-1,12)+1,'PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','DRAFT','PENDING','REJECTED','ARCHIVED','DELETED','PUBLISHED');
      INSERT INTO business_posts(business_id,created_by_user_id,updated_by_user_id,title,slug,summary,content,status,submitted_at,published_at,deleted_at,created_at)
      VALUES(v_businesses+MOD(i-1,60),v_users+40+MOD(i-1,60),v_users+40+MOD(i-1,60),
        CONCAT(ELT(MOD(i-1,10)+1,'Bua trua voi ga Mia','Trai nghiem o nha da ong','Cuoi tuan ben ho Dong Mo','Thuong thuc che lam Duong Lam','Thue xe dap tham lang co','Ca phe ngam Thanh co','Qua tang OCOP Xu Doai','Phong nghi cho gia dinh','Mon ngon gan Den Va','Goi y check-in Son Tay'),' - ',LPAD(i,3,'0')),
        CONCAT('tin-doanh-nghiep-son-tay-',LPAD(i,3,'0')),
        'Thong tin dich vu dia phuong danh cho du khach tham quan Son Tay.',
        CONCAT('Co so gioi thieu san pham va trai nghiem mang ban sac Son Tay. Gia dich vu duoc niem yet ro rang, nhan vien ho tro bang tieng Viet va san sang tu van lich trinh tai cac diem di san gan do. Ma noi dung ',i,'.'),
        v_status,IF(v_status IN ('PENDING','PUBLISHED','REJECTED','ARCHIVED','DELETED'),'2025-09-01 08:00:00',NULL),IF(v_status='PUBLISHED',TIMESTAMP('2025-09-05 08:00:00')+INTERVAL i HOUR,NULL),IF(v_status='DELETED','2026-03-01 08:00:00',NULL),TIMESTAMP('2025-08-01 08:00:00')+INTERVAL i HOUR);
      IF i=1 THEN SET v_posts=LAST_INSERT_ID(); END IF; SET i=i+1;
    END WHILE;

    SET i=1; WHILE i<=60 DO
      INSERT INTO tags(tag_name,slug) VALUES(CONCAT(ELT(MOD(i-1,12)+1,'Duong Lam','Thanh co','Chua Mia','Den Va','Dong Mo','Xu Doai','Ga Mia','Che lam','Da ong','OCOP Son Tay','Xe dap','Homestay'),' ',LPAD(i,2,'0')),CONCAT('tag-son-tay-',LPAD(i,2,'0')));
      IF i=1 THEN SET v_base=LAST_INSERT_ID(); END IF; SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=180 DO
      SET j=1; WHILE j<=4 DO
        INSERT INTO business_post_tags(business_post_id,tag_id) VALUES(v_posts+i-1,v_base+MOD((i-1)*4+j-1,60));
        SET j=j+1;
      END WHILE; SET i=i+1;
    END WHILE;

    SET i=1; WHILE i<=120 DO
      SET v_status=ELT(MOD(i-1,14)+1,'ACTIVE','ACTIVE','ACTIVE','ACTIVE','DRAFT','PENDING','REJECTED','EXPIRED','ARCHIVED','DELETED','ACTIVE','ACTIVE','ACTIVE','ACTIVE');
      INSERT INTO promotions(business_id,created_by_user_id,updated_by_user_id,title,description,discount_type,discount_value,promo_code,start_at,end_at,status,submitted_at,published_at,deleted_at,created_at)
      VALUES(v_businesses+MOD(i-1,60),v_users+40+MOD(i-1,60),v_users+40+MOD(i-1,60),
        CONCAT(ELT(MOD(i-1,8)+1,'Uu dai am thuc ga Mia','Giam gia homestay Duong Lam','Combo nghi duong Dong Mo','Tang qua che lam','Uu dai thue xe dap','Khuyen mai ca phe Thanh co','Combo tour va bua trua','Qua tang OCOP'),' ',LPAD(i,3,'0')),
        'Chuong trinh uu dai danh cho du khach kham pha Son Tay, dieu kien ap dung va muc gia duoc cong bo ro rang tai co so.',
        ELT(MOD(i-1,3)+1,'PERCENTAGE','FIXED_AMOUNT','OTHER'),CASE MOD(i-1,3) WHEN 0 THEN 5+MOD(i,36) WHEN 1 THEN 20000+MOD(i,8)*10000 ELSE NULL END,
        IF(MOD(i,4)=0,NULL,CONCAT('SONTAY',LPAD(i,4,'0'))),
        IF(v_status='EXPIRED','2025-01-01 00:00:00',TIMESTAMP('2026-01-01 00:00:00')+INTERVAL MOD(i,180) DAY),
        IF(v_status='EXPIRED','2025-03-01 23:59:59',TIMESTAMP('2026-01-01 00:00:00')+INTERVAL (MOD(i,180)+30) DAY),v_status,
        IF(v_status IN ('PENDING','ACTIVE','REJECTED','EXPIRED','ARCHIVED','DELETED'),'2025-12-15 08:00:00',NULL),
        IF(v_status='ACTIVE','2025-12-20 08:00:00',NULL),IF(v_status='DELETED','2026-04-01 08:00:00',NULL),TIMESTAMP('2025-11-01 08:00:00')+INTERVAL i HOUR);
      IF i=1 THEN SET v_promotions=LAST_INSERT_ID(); END IF; SET i=i+1;
    END WHILE;

    -- =========================================================
    -- 06. TOURS AND FAVORITES
    -- =========================================================
    SET i=1; WHILE i<=180 DO
      SET v_status=ELT(MOD(i-1,16)+1,'PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','DRAFT','SUBMITTED','REJECTED','COMPLETED','CANCELLED','ARCHIVED','DELETED','PUBLISHED','PUBLISHED','COMPLETED','SUBMITTED');
      INSERT INTO tours(owner_user_id,title,description,region,difficulty_level,estimated_distance_km,estimated_duration_minutes,status,visibility,submitted_at,published_at,completed_at,deleted_at,created_at)
      VALUES(v_users+MOD(i-1,120),CONCAT(ELT(MOD(i-1,8)+1,'Tour Thành cổ Sơn Tây','Tour Làng cổ Đường Lâm','Tour Văn hóa Xứ Đoài','Sơn Tây một ngày','Sơn Tây cuối tuần','Khám phá Đồng Mô','Di sản Sơn Tây và vùng Ba Vì','Tour xe đạp Đường Lâm'),' ',LPAD(i,3,'0')),
        'Lịch trình gồm các điểm tham quan tại Sơn Tây, cân đối thời gian di chuyển, nghỉ ngơi và trải nghiệm ẩm thực địa phương.','Sơn Tây, Hà Nội',ELT(MOD(i-1,3)+1,'EASY','MODERATE','CHALLENGING'),5+MOD(i,45),240+MOD(i,480),v_status,
        IF(v_status IN ('PUBLISHED','COMPLETED'),'PUBLIC',ELT(MOD(i-1,3)+1,'PRIVATE','UNLISTED','PUBLIC')),
        IF(v_status IN ('SUBMITTED','PUBLISHED','REJECTED','COMPLETED','CANCELLED','ARCHIVED','DELETED'),'2025-10-01 08:00:00',NULL),
        IF(v_status='PUBLISHED','2025-10-05 08:00:00',NULL),IF(v_status='COMPLETED','2026-01-15 18:00:00',NULL),IF(v_status='DELETED','2026-02-15 08:00:00',NULL),TIMESTAMP('2025-09-01 08:00:00')+INTERVAL i HOUR);
      IF i=1 THEN SET v_tours=LAST_INSERT_ID(); END IF; SET i=i+1;
    END WHILE;
    SET i=5; WHILE i<=180 DO
      IF MOD(i,4)=1 THEN UPDATE tours SET source_tour_id=v_tours+i-5 WHERE id=v_tours+i-1; END IF;
      SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=180 DO
      SET j=1; WHILE j<=5 DO
        INSERT INTO tour_items(tour_id,place_id,visit_order,planned_start_at,duration_minutes,transport_method,note)
        VALUES(v_tours+i-1,v_places+MOD((i-1)*7+j-1,200),j,TIMESTAMP('2026-06-01 07:00:00')+INTERVAL (j-1)*90 MINUTE,45+MOD(i+j,4)*15,ELT(MOD(j-1,4)+1,'WALK','BICYCLE','ELECTRIC_CAR','PRIVATE_CAR'),ELT(j,'Don khach va nghe gioi thieu','Tham quan khong gian chinh','Nghi va thuong thuc dac san','Chup anh, mua qua dia phuong','Ket thuc lich trinh tai Son Tay'));
        SET j=j+1;
      END WHILE; SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=1200 DO
      INSERT INTO favorites(user_id,place_id,created_at) VALUES(v_users+MOD(i-1,120),v_places+MOD((i-1)*7+FLOOR((i-1)/120),200),TIMESTAMP('2025-08-01 08:00:00')+INTERVAL i HOUR);
      SET i=i+1;
    END WHILE;

    -- =========================================================
    -- 07. QUIZZES, ATTEMPTS AND BADGES
    -- =========================================================
    SET i=1; WHILE i<=60 DO
      SET v_status=ELT(MOD(i-1,12)+1,'PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','PUBLISHED','DRAFT','PENDING','REJECTED','ARCHIVED','DELETED','PUBLISHED');
      INSERT INTO quizzes(place_id,created_by_user_id,updated_by_user_id,title,description,time_limit_seconds,passing_score_percent,status,submitted_at,published_at,deleted_at,created_at)
      VALUES(v_places+MOD(i-1,30),v_users+100+MOD(i,8),v_users+108+MOD(i,8),CONCAT(ELT(MOD(i-1,10)+1,'Lịch sử Sơn Tây','Bí ẩn Thành cổ Sơn Tây','Khám phá Làng cổ Đường Lâm','Tượng cổ Chùa Mía','Lễ hội Đền Và','Văn hóa Xứ Đoài','Phùng Hưng - Bố Cái Đại Vương','Ngô Quyền và Đường Lâm','Đặc sản Sơn Tây','Lễ hội truyền thống Sơn Tây'),' ',LPAD(i,2,'0')),
        'Sáu câu hỏi về lịch sử, văn hóa, di tích và ẩm thực Sơn Tây.',300+MOD(i,5)*60,60+MOD(i,4)*5,v_status,IF(v_status IN ('PENDING','PUBLISHED','REJECTED','ARCHIVED','DELETED'),'2025-11-01 08:00:00',NULL),IF(v_status='PUBLISHED','2025-11-05 08:00:00',NULL),IF(v_status='DELETED','2026-03-10 08:00:00',NULL),TIMESTAMP('2025-10-01 08:00:00')+INTERVAL i HOUR);
      IF i=1 THEN SET v_quizzes=LAST_INSERT_ID(); END IF; SET i=i+1;
    END WHILE;

    SET i=1; WHILE i<=60 DO
      SET j=1; WHILE j<=6 DO
        INSERT INTO questions(quiz_id,content,explanation,display_order,points)
        VALUES(v_quizzes+i-1,CONCAT(ELT(j,'Cong trinh nao la bieu tuong quan su cua Son Tay?','Vat lieu nao tao nen sac thai kien truc Duong Lam?','Chua nao noi tieng voi he thong tuong co o Son Tay?','Den Va tho vi thanh nao trong tin nguong dan gian?','Hai vi vua nao gan voi vung dat Duong Lam?','Mon qua nao thuong duoc nhac den trong am thuc Xu Doai?'),' [bo ',LPAD(i,2,'0'),']'),ELT(j,'Thanh co Son Tay la dau an kien truc quan su noi bat.','Da ong la vat lieu dac trung cua nhieu cong trinh tai Duong Lam.','Chua Mia luu giu nhieu pho tuong co co gia tri.','Den Va gan voi tin nguong Duc Thanh Tan Vien.','Ngo Quyen va Phung Hung deu gan voi Duong Lam.','Che lam la mon qua quen thuoc cua Xu Doai.'),j,1.00);
        IF i=1 AND j=1 THEN SET v_questions=LAST_INSERT_ID(); END IF;
        SET v_base=LAST_INSERT_ID();
        SET v_correct=MOD(i+j-2,4)+1;
        SET k=1; WHILE k<=4 DO
          INSERT INTO answers(question_id,content,is_correct,display_order)
          VALUES(v_base,CASE j WHEN 1 THEN ELT(k,'Thanh co Son Tay','Ho Dong Mo','Cho Mia','Pho di bo') WHEN 2 THEN ELT(k,'Da ong','Da hoa cuong','Gach men','Thep') WHEN 3 THEN ELT(k,'Chua Mia','Chua Tri','Chua Khai Nguyen','Dinh Mong Phu') WHEN 4 THEN ELT(k,'Duc Thanh Tan Vien','Ngo Quyen','Phung Hung','Giang Van Minh') WHEN 5 THEN ELT(k,'Ngo Quyen va Phung Hung','Ly Thuong Kiet va Tran Hung Dao','Hai Ba Trung','Le Loi va Nguyen Trai') ELSE ELT(k,'Che lam','Banh cuon','Com hen','Keo dua') END,k=v_correct,k);
          IF i=1 AND j=1 AND k=1 THEN SET v_answers=LAST_INSERT_ID(); END IF;
          SET k=k+1;
        END WHILE;
        SET j=j+1;
      END WHILE; SET i=i+1;
    END WHILE;

    SET i=1; WHILE i<=25 DO
      INSERT INTO badges(badge_code,badge_name,description,icon_url) VALUES(CONCAT('SONTAY_',LPAD(i,2,'0')),CONCAT(ELT(MOD(i-1,10)+1,'Nguoi ban Thanh co','Su gia Duong Lam','Am hieu Chua Mia','Ban cua Den Va','Nha van hoa Xu Doai','Dau an Phung Hung','Hau due Ngo Quyen','Tin do am thuc Son Tay','Nha tham hiem Dong Mo','Du khach ben bi'),' ',LPAD(i,2,'0')),'Huy hieu ghi nhan ket qua tim hieu du lich, lich su va van hoa Son Tay.',CONCAT('https://cdn.ltss.local/son-tay/badges/',LPAD(i,2,'0'),'.png'));
      IF i=1 THEN SET v_badges=LAST_INSERT_ID(); END IF; SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=120 DO
      INSERT INTO quiz_badges(quiz_id,badge_id,minimum_score_percent) VALUES(v_quizzes+MOD(i-1,60),v_badges+MOD(FLOOR((i-1)/60)*7+i-1,25),IF(MOD(i,2)=0,80,60));
      SET i=i+1;
    END WHILE;

    SET i=1; WHILE i<=600 DO
      SET v_status=ELT(MOD(i-1,4)+1,'IN_PROGRESS','SUBMITTED','AUTO_SUBMITTED','ABANDONED');
      SET v_started=TIMESTAMP('2025-07-01 08:00:00')+INTERVAL MOD(i*11,380) DAY+INTERVAL MOD(i,12) HOUR;
      INSERT INTO quiz_attempts(quiz_id,user_id,status,randomization_seed,started_at,expires_at,submitted_at,score,total_points,score_percent,is_passed,location_verified_at,distance_to_place_meters,created_at)
      VALUES(v_quizzes+MOD(i-1,60),v_users+MOD(i*7-1,120),v_status,SHA2(CONCAT('quiz-seed-',i),256),v_started,v_started+INTERVAL 10 MINUTE,IF(v_status IN ('SUBMITTED','AUTO_SUBMITTED'),v_started+INTERVAL 7 MINUTE,NULL),CASE WHEN v_status='IN_PROGRESS' THEN 2 ELSE MOD(i,7) END,6,CASE WHEN v_status='IN_PROGRESS' THEN 33.33 ELSE ROUND(MOD(i,7)*100/6,2) END,CASE WHEN v_status='IN_PROGRESS' THEN FALSE ELSE MOD(i,7)>=4 END,IF(MOD(i,3)=0,v_started,NULL),IF(MOD(i,3)=0,25+MOD(i,450),NULL),v_started);
      IF i=1 THEN SET v_attempts=LAST_INSERT_ID(); END IF; SET i=i+1;
    END WHILE;

    SET i=1; WHILE i<=600 DO
      SET j=1; WHILE j<=6 DO
        SET v_correct=MOD((MOD(i-1,60)+1)+j-2,4)+1;
        SET v_selected=MOD(i+j-2,4)+1;
        IF MOD(i-1,4)=0 AND j>3 THEN SET v_selected=0; END IF;
        INSERT INTO quiz_attempt_answers(attempt_id,question_id,selected_answer_id,question_order,question_text_snapshot,selected_answer_text_snapshot,correct_answer_text_snapshot,explanation_snapshot,is_correct,awarded_points,answered_at)
        SELECT v_attempts+i-1,q.id,IF(v_selected=0,NULL,sa.id),j,q.content,IF(v_selected=0,NULL,sa.content),ca.content,q.explanation,(v_selected=v_correct),IF(v_selected=v_correct,q.points,0),IF(v_selected=0,NULL,TIMESTAMP('2025-07-01 08:00:00')+INTERVAL i DAY+INTERVAL j MINUTE)
        FROM questions q JOIN answers ca ON ca.question_id=q.id AND ca.display_order=v_correct
        LEFT JOIN answers sa ON sa.question_id=q.id AND sa.display_order=v_selected
        WHERE q.id=v_questions+(MOD(i-1,60)*6)+(j-1);
        SET j=j+1;
      END WHILE; SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=300 DO
      INSERT INTO user_badges(user_id,badge_id,awarded_by_quiz_id,awarded_attempt_id,awarded_at) VALUES(v_users+MOD(i-1,120),v_badges+MOD(FLOOR((i-1)/120)*7+i-1,25),v_quizzes+MOD(i-1,60),v_attempts+MOD(i-1,600),TIMESTAMP('2025-12-01 08:00:00')+INTERVAL i HOUR);
      SET i=i+1;
    END WHILE;

    -- =========================================================
    -- 08. REVIEWS
    -- =========================================================
    SET i=1; WHILE i<=600 DO
      SET v_status=ELT(MOD(i-1,10)+1,'VISIBLE','VISIBLE','VISIBLE','VISIBLE','VISIBLE','VISIBLE','PENDING','REJECTED','HIDDEN','REMOVED');
      INSERT INTO reviews(user_id,place_id,business_id,article_id,tour_id,rating,comment,status,submitted_at,published_at,deleted_at,created_at)
      VALUES(
        v_users+MOD(i-1,120),
        IF(i<=150,v_places+MOD(i-1,200),NULL),
        IF(i>150 AND i<=300,v_businesses+MOD((i-151)+FLOOR((i-151)/120)*2,60),NULL),
        IF(i>300 AND i<=450,v_articles+MOD((i-301)+FLOOR((i-301)/120)*3,180),NULL),
        IF(i>450,v_tours+MOD((i-451)+FLOOR((i-451)/120)*5,180),NULL),
        CASE MOD(i-1,20) WHEN 0 THEN 1 WHEN 1 THEN 2 WHEN 2 THEN 2 WHEN 3 THEN 3 WHEN 4 THEN 3 WHEN 5 THEN 3 WHEN 6 THEN 3 WHEN 7 THEN 4 WHEN 8 THEN 4 WHEN 9 THEN 4 WHEN 10 THEN 4 WHEN 11 THEN 4 WHEN 12 THEN 4 WHEN 13 THEN 4 ELSE 5 END,
        ELT(MOD(i-1,10)+1,'Không gian rất cổ kính và yên bình, phù hợp cho một ngày tham quan chậm rãi.','Thành cổ được bảo tồn khá tốt, nhiều góc chụp ảnh đẹp và dễ tìm đường.','Chùa Mía rất thanh tịnh, khuôn viên sạch sẽ và người hướng dẫn thân thiện.','Đường Lâm đẹp nhất vào mùa thu, nhưng cuối tuần có lúc khá đông khách.','Đền Và có lễ hội rất đặc sắc, không gian rừng lim tạo cảm giác dễ chịu.','Đồ ăn ngon, giá hợp lý và phục vụ nhanh trong khung giờ buổi trưa.','Homestay sạch sẽ, gần khu di tích và chủ nhà tư vấn lịch trình tận tình.','Dịch vụ ở mức tạm ổn nhưng bãi đỗ xe cần có thêm biển chỉ dẫn rõ ràng.','Phong cảnh đẹp, tuy nhiên thời gian chờ đồ ăn vào ngày lễ còn hơi lâu.','Trải nghiệm chưa như mong đợi, khu vệ sinh và chỉ dẫn cần được cải thiện thêm.'),
        v_status,TIMESTAMP('2025-08-01 08:00:00')+INTERVAL i HOUR,IF(v_status='VISIBLE',TIMESTAMP('2025-08-02 08:00:00')+INTERVAL i HOUR,NULL),IF(v_status='REMOVED',TIMESTAMP('2026-01-01 08:00:00')+INTERVAL i HOUR,NULL),TIMESTAMP('2025-08-01 08:00:00')+INTERVAL i HOUR);
      IF i=1 THEN SET v_reviews=LAST_INSERT_ID(); END IF; SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=150 DO
      INSERT INTO review_replies(review_id,replied_by_user_id,content,created_at) VALUES(v_reviews+i-1,v_users+40+MOD(i-1,60),ELT(MOD(i-1,4)+1,'Cam on ban da chia se trai nghiem. Co so se tiep tuc duy tri chat luong phuc vu.','Chung toi da ghi nhan gop y ve bien chi dan va se bo sung trong thoi gian som nhat.','Rat vui khi ban co mot hanh trinh dang nho tai Son Tay. Hen gap lai ban.','Co so xin loi ve thoi gian cho va da dieu chinh quy trinh phuc vu vao ngay cao diem.'),TIMESTAMP('2025-09-01 08:00:00')+INTERVAL i HOUR);
      SET i=i+1;
    END WHILE;

    -- =========================================================
    -- 09. MEDIA MAPPINGS AND PANORAMA HOTSPOTS
    -- =========================================================
    -- Each of the first 100 places receives two panoramas from the same place.
    SET i=1; WHILE i<=200 DO
      INSERT INTO place_media(place_id,media_asset_id,usage_type,display_order,is_primary) VALUES(v_places+FLOOR((i-1)/2),v_media+i-1,'PANORAMA',MOD(i-1,2),FALSE);
      SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=200 DO
      INSERT INTO place_media(place_id,media_asset_id,usage_type,display_order,is_primary) VALUES(v_places+i-1,v_media+200+MOD(i-1,400),'GALLERY',2,TRUE);
      SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=120 DO
      INSERT INTO event_media(event_id,media_asset_id,usage_type,display_order,is_primary) VALUES(v_events+i-1,v_media+200+MOD((i-1)*2,400),'COVER',0,TRUE);
      INSERT INTO event_media(event_id,media_asset_id,usage_type,display_order,is_primary) VALUES(v_events+i-1,v_media+200+MOD((i-1)*2+1,400),'GALLERY',1,FALSE);
      SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=180 DO
      INSERT INTO article_media(article_id,media_asset_id,usage_type,display_order,is_primary) VALUES(v_articles+i-1,v_media+200+MOD((i-1)*2,400),'COVER',0,TRUE);
      INSERT INTO article_media(article_id,media_asset_id,usage_type,display_order,is_primary) VALUES(v_articles+i-1,v_media+200+MOD((i-1)*2+1,400),'INLINE',1,FALSE);
      SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=180 DO
      INSERT INTO business_post_media(business_post_id,media_asset_id,usage_type,display_order,is_primary) VALUES(v_posts+i-1,v_media+200+MOD((i-1)*2+100,400),'COVER',0,TRUE);
      INSERT INTO business_post_media(business_post_id,media_asset_id,usage_type,display_order,is_primary) VALUES(v_posts+i-1,v_media+200+MOD((i-1)*2+101,400),'INLINE',1,FALSE);
      SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=120 DO
      INSERT INTO promotion_media(promotion_id,media_asset_id,usage_type,display_order,is_primary) VALUES(v_promotions+i-1,v_media+200+MOD((i-1)*2+200,400),'COVER',0,TRUE);
      INSERT INTO promotion_media(promotion_id,media_asset_id,usage_type,display_order,is_primary) VALUES(v_promotions+i-1,v_media+200+MOD((i-1)*2+201,400),'GALLERY',1,FALSE);
      SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=180 DO
      INSERT INTO tour_media(tour_id,media_asset_id,usage_type,display_order,is_primary) VALUES(v_tours+i-1,v_media+200+MOD((i-1)*2+300,400),'COVER',0,TRUE);
      INSERT INTO tour_media(tour_id,media_asset_id,usage_type,display_order,is_primary) VALUES(v_tours+i-1,v_media+200+MOD((i-1)*2+301,400),'GALLERY',1,FALSE);
      SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=150 DO
      INSERT INTO review_media(review_id,media_asset_id,display_order) VALUES(v_reviews+i-1,v_media+200+MOD(i-1,400),1);
      SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=60 DO
      INSERT INTO quiz_media(quiz_id,media_asset_id,usage_type,display_order,is_primary) VALUES(v_quizzes+i-1,v_media+200+MOD((i-1)*2,400),'COVER',0,TRUE);
      INSERT INTO quiz_media(quiz_id,media_asset_id,usage_type,display_order,is_primary) VALUES(v_quizzes+i-1,v_media+200+MOD((i-1)*2+1,400),'EXPLANATION',1,FALSE);
      SET i=i+1;
    END WHILE;
    SET i=1; WHILE i<=60 DO
      INSERT INTO panorama_hotspots(source_media_asset_id,target_media_asset_id,hotspot_type,yaw_degrees,pitch_degrees,label,description,display_order,is_active)
      VALUES(v_media+(i-1)*2,v_media+(i-1)*2+1,'TRANSITION',-170+MOD(i*37,340),-25+MOD(i*11,50),'Di chuyen sang khong gian ke tiep','Hai panorama deu thuoc cung mot diem tham quan tai Son Tay.',1,TRUE);
      INSERT INTO panorama_hotspots(source_media_asset_id,target_media_asset_id,hotspot_type,yaw_degrees,pitch_degrees,label,description,display_order,is_active)
      VALUES(v_media+(i-1)*2,NULL,'INFO',-170+MOD(i*53,340),-30+MOD(i*7,60),ELT(MOD(i-1,5)+1,'Kien truc da ong','Cay co thu','Cong trinh chinh','Goc chup anh','Loi tham quan'),'Thong tin thuyet minh ngan gon ve diem nhin trong khong gian 360.',2,MOD(i,10)<>0);
      SET i=i+1;
    END WHILE;

    -- =========================================================
    -- 10. MODERATION
    -- =========================================================
    SET i=1; WHILE i<=600 DO
      SET v_status=ELT(MOD(i-1,3)+1,'PENDING','RESOLVED','CANCELLED');
      INSERT INTO moderation_records(submitted_by_user_id,moderator_user_id,place_id,business_id,event_id,article_id,business_post_id,promotion_id,tour_id,quiz_id,review_id,status,decision,submission_note,decision_reason,submitted_at,resolved_at,created_at)
      VALUES(v_users+MOD(i-1,120),IF(v_status='PENDING',NULL,v_users+108+MOD(i,8)),
        IF(MOD(i-1,9)=0,v_places+MOD(i-1,200),NULL),IF(MOD(i-1,9)=1,v_businesses+MOD(i-1,60),NULL),IF(MOD(i-1,9)=2,v_events+MOD(i-1,120),NULL),IF(MOD(i-1,9)=3,v_articles+MOD(i-1,180),NULL),IF(MOD(i-1,9)=4,v_posts+MOD(i-1,180),NULL),IF(MOD(i-1,9)=5,v_promotions+MOD(i-1,120),NULL),IF(MOD(i-1,9)=6,v_tours+MOD(i-1,180),NULL),IF(MOD(i-1,9)=7,v_quizzes+MOD(i-1,60),NULL),IF(MOD(i-1,9)=8,v_reviews+MOD(i-1,600),NULL),
        v_status,IF(v_status='RESOLVED',IF(MOD(i,4)=0,'REJECTED','APPROVED'),NULL),'De nghi kiem duyet noi dung du lich Son Tay.',IF(v_status='RESOLVED' AND MOD(i,4)=0,'Noi dung can bo sung nguon thong tin va dieu chinh cach dien dat truoc khi xuat ban.',IF(v_status='RESOLVED','Noi dung phu hop tieu chuan cong dong va boi canh Son Tay.',NULL)),TIMESTAMP('2025-08-01 08:00:00')+INTERVAL i HOUR,IF(v_status='PENDING',NULL,TIMESTAMP('2025-08-02 08:00:00')+INTERVAL i HOUR),TIMESTAMP('2025-08-01 08:00:00')+INTERVAL i HOUR);
      SET i=i+1;
    END WHILE;

    -- =========================================================
    -- 11. ANALYTICS AND ENGAGEMENT EVENTS
    -- =========================================================
    INSERT INTO engagement_event_types(event_type_code,event_type_name,description) VALUES
    ('PLACE_VIEW','Xem dia diem','Xem chi tiet diem den Son Tay'),('PLACE_DIRECTION_REQUEST','Tim duong den dia diem','Mo chi duong'),('PLACE_SHARE','Chia se dia diem','Chia se diem den'),
    ('BUSINESS_VIEW','Xem doanh nghiep','Xem co so dia phuong'),('BUSINESS_CONTACT_CLICK','Lien he doanh nghiep','Nhan goi hoac gui lien he'),
    ('EVENT_VIEW','Xem su kien','Xem su kien Son Tay'),('EVENT_SHARE','Chia se su kien','Chia se le hoi'),
    ('ARTICLE_VIEW','Xem bai viet','Doc bai van hoa Xu Doai'),('ARTICLE_SHARE','Chia se bai viet','Chia se noi dung'),
    ('POST_VIEW','Xem bai doanh nghiep','Xem tin co so'),('POST_SHARE','Chia se bai doanh nghiep','Chia se tin co so'),
    ('PROMOTION_VIEW','Xem khuyen mai','Xem uu dai'),('PROMOTION_CLICK','Nhan khuyen mai','Bam xem uu dai'),
    ('TOUR_VIEW','Xem tour','Xem lich trinh Son Tay'),('TOUR_COPY','Sao chep tour','Sao chep lich trinh'),('TOUR_ROUTE_REQUEST','Tim duong theo tour','Mo tuyen duong'),
    ('QUIZ_START','Bat dau quiz','Danh muc su kien du phong; schema khong co quiz target'),('QUIZ_COMPLETE','Hoan thanh quiz','Danh muc su kien du phong; schema khong co quiz target');

    SET i=1; WHILE i<=8000 DO
      SET j=MOD(i-1,16)+1;
      INSERT INTO engagement_events(event_type_code,user_id,session_key,place_id,business_id,event_id,article_id,business_post_id,promotion_id,tour_id,metadata,occurred_at)
      VALUES(ELT(j,'PLACE_VIEW','PLACE_DIRECTION_REQUEST','PLACE_SHARE','BUSINESS_VIEW','BUSINESS_CONTACT_CLICK','EVENT_VIEW','EVENT_SHARE','ARTICLE_VIEW','ARTICLE_SHARE','POST_VIEW','POST_SHARE','PROMOTION_VIEW','PROMOTION_CLICK','TOUR_VIEW','TOUR_COPY','TOUR_ROUTE_REQUEST'),
        IF(MOD(i,4)=0,NULL,v_users+MOD(i*7-1,120)),CONCAT(IF(MOD(i,4)=0,'guest-','user-'),LPAD(MOD(i*13,1400),5,'0')),
        IF(j<=3,v_places+CASE WHEN MOD(i,10)<7 THEN MOD(i-1,15) ELSE MOD(i*17,200) END,NULL),
        IF(j IN (4,5),v_businesses+MOD(i*11,60),NULL),IF(j IN (6,7),v_events+MOD(i*13,120),NULL),IF(j IN (8,9),v_articles+MOD(i*17,180),NULL),IF(j IN (10,11),v_posts+MOD(i*19,180),NULL),IF(j IN (12,13),v_promotions+MOD(i*23,120),NULL),IF(j>=14,v_tours+MOD(i*29,180),NULL),
        JSON_OBJECT('device',ELT(MOD(i-1,3)+1,'mobile','desktop','tablet'),'source',ELT(MOD(i-1,4)+1,'search','direct','social','recommendation'),'browser',ELT(MOD(i-1,4)+1,'Chrome','Safari','Firefox','Edge'),'province','Ha Noi','context',ELT(j,'Xem Thanh co Son Tay','Tim duong den Duong Lam','Chia se Chua Mia','Xem co so Son Tay','Lien he dat dich vu','Xem Le hoi Den Va','Chia se su kien','Xem bai Van hoa Xu Doai','Chia se bai viet','Xem tin am thuc','Chia se tin co so','Xem uu dai','Nhan ma uu dai','Xem Tour Son Tay','Sao chep Tour Son Tay','Tim duong den Dong Mo')),
        TIMESTAMP('2025-07-01 00:00:00')+INTERVAL MOD(i*17,380) DAY+INTERVAL MOD(i*7,24) HOUR+INTERVAL CASE WHEN MOD(DAYOFWEEK(TIMESTAMP('2025-07-01')+INTERVAL MOD(i*17,380) DAY),6) IN (0,1) THEN MOD(i,45) ELSE MOD(i,20) END MINUTE);
      SET i=i+1;
    END WHILE;
END$$
DELIMITER ;

CALL seed_ltss_son_tay();
DROP PROCEDURE seed_ltss_son_tay;

-- A compact completion signal; detailed checks live in ltss_test_data_validation.sql.
SELECT 'LTSS Son Tay test data seeded' AS result,
       (SELECT COUNT(*) FROM users) AS users,
       (SELECT COUNT(*) FROM places) AS places,
       (SELECT COUNT(*) FROM engagement_events) AS engagement_events;
-- ============================================================================
-- FINAL EXECUTION SUMMARY
-- ============================================================================
SELECT DATABASE() AS database_name,
       (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()) AS table_count,
       (SELECT COUNT(*) FROM users) AS users,
       (SELECT COUNT(*) FROM places) AS places,
       (SELECT COUNT(*) FROM reviews) AS reviews,
       (SELECT COUNT(*) FROM engagement_events) AS engagement_events;
