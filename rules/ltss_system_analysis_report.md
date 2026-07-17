# BÁO CÁO PHÂN TÍCH HỆ THỐNG LTSS TRƯỚC TRIỂN KHAI

**Ngày phân tích:** 2026-07-16  
**Phạm vi:** đọc và đối chiếu toàn bộ tài liệu nghiệp vụ, DDL MySQL 8, quy tắc tổ chức source và cây workspace thực tế.  
**Trạng thái:** phân tích nền tảng; chưa có module cụ thể để triển khai.

> **Cập nhật triển khai 2026-07-16:** Báo cáo bên dưới được giữ như snapshot trước triển khai. Workspace hiện đã có Phase 0–7 và phần policy-safe của Phase 8: engagement ingestion/dedup/rate-limit, system/business analytics, admin dashboard, user status/official-role management, sanitized audit viewer và read-only retention status. Contract cùng các giả định được ghi tại `docs/phase2-identity-access.md` đến `docs/phase8-analytics-administration.md`. Catalog event/KPI, thời hạn retention/xóa dữ liệu, permission management, tour publication/reviewer và các policy community còn mở vẫn không được tự đặt.

## 0. Kết luận điều hành

1. Đã đọc đủ `rules/ltss_database_architecture_report.md` (3.411 dòng), `database/ltss_database_mysql8.sql` (1.291 dòng), `rules/structure.md` (348 dòng) và yêu cầu đính kèm (623 dòng).
2. Phần SQL nhúng tại `rules/ltss_database_architecture_report.md:2041-3331` giống chính xác 1.291 dòng của file SQL độc lập. Khi triển khai, file trong `database/` phải là nguồn vật lý chính; bản nhúng chỉ dùng để đọc báo cáo.
3. `backend/` và `frontend/` tồn tại nhưng đều trống. Không có Maven/Vite project, source, migration, test, cấu hình môi trường, Docker/Nginx, API contract hay Git repository.
4. Yêu cầu module cụ thể vẫn là placeholder `[DÁN MODULE HOẶC CHỨC NĂNG CẦN LÀM VÀO ĐÂY]`. Vì vậy không có chức năng hợp lệ để tự ý triển khai; kết quả của lượt này là báo cáo phân tích, không phải skeleton giả hoặc code suy diễn.
5. DDL hiện chưa thể dùng nguyên trạng làm Flyway V1. Có các blocker MySQL 8 tại `tours`, `role_inheritances`, `reviews`, `engagement_events`, `moderation_records`, `panorama_hotspots` do giới hạn giữa `CHECK`, `AUTO_INCREMENT` và cột có hành vi foreign key; `media_assets.storage_key VARCHAR(1000)` có unique index vượt giới hạn key của InnoDB/utf8mb4. Không sửa âm thầm file SQL gốc; cần migration/baseline đã được review.
6. Bảy nhóm quyết định nghiệp vụ critical vẫn mở: activation, mô hình Business Owner, quan hệ business/place, người duyệt tour, target/eligibility của review, phạm vi moderation và mô hình event.

## 1. Nguồn, thứ tự ưu tiên và quy ước

### 1.1. Thứ tự ưu tiên

| Loại quyết định | Nguồn ưu tiên |
|---|---|
| Actor, use case, business rule, trạng thái, ownership, history | `rules/ltss_database_architecture_report.md` |
| Tên bảng/cột, kiểu, PK/FK/UQ/CHECK/index/default/ON DELETE | `database/ltss_database_mysql8.sql` |
| Tổ chức package/thư mục và coding convention | `rules/structure.md` và yêu cầu đính kèm |

Khi business rule và DDL khác nhau, backend phải giữ đúng nghiệp vụ nhưng không được tự đổi schema. Bất kỳ thay đổi schema nào cũng phải được ghi thành migration riêng kèm lý do và đánh giá dữ liệu.

### 1.2. Phân biệt dữ kiện và đề xuất

- Tên bảng, cột, trạng thái và constraint trong báo cáo này là dữ kiện từ DDL.
- Endpoint, Controller, DTO và file frontend ở mục 8 là **phạm vi/contract dự thảo**, chưa tồn tại trong workspace và chưa phải contract được chốt.
- Những nơi ghi `TBD` hoặc “chưa chốt” không được biến thành business rule khi code.
- Guest là trạng thái chưa xác thực, không phải row trong `roles` (`rules/ltss_database_architecture_report.md:3336`).

## 2. Hiện trạng project

```text
project_group/
├── backend/                              # trống
├── frontend/                             # trống
├── database/
│   └── ltss_database_mysql8.sql
└── rules/
    ├── structure.md
    ├── ltss_database_architecture_report.md
    └── ltss_system_analysis_report.md    # báo cáo này
```

### 2.1. Thành phần chưa tồn tại

- Backend: `pom.xml`, Maven wrapper, `Application.java`, package Java, `application.yml/properties`, Spring Security, JPA entity/repository, controller/service/DTO, Flyway, test.
- Frontend: `package.json`, lockfile, `index.html`, `vite.config.*`, `src`, router, page/component, Axios instance, state store, test.
- Hạ tầng: `.env*`, Dockerfile, Compose, Nginx, CI, README, OpenAPI/API contract.
- Không có source nên chưa thể truy vết Controller → Service → Repository hoặc Route → Page → API service, cũng chưa thể xác định convention Java/React “đang dùng”.
- Không có file môi trường hoặc credential nào được đọc/hiển thị.

### 2.2. Convention có thể xác nhận

Convention hiện hữu chỉ có ở tầng database:

- Table/column: plural `snake_case`.
- Constraint/index: `uq_`, `chk_`, `fk_`, `idx_`, `ftx_`.
- Trạng thái: `VARCHAR + CHECK`, không dùng MySQL `ENUM`.
- Aggregate thường xuyên cập nhật có `version` để map `@Version`.
- Dữ liệu đã publish/có history dùng status + `deleted_at`/archive; hard delete bị giới hạn.
- JSON chỉ dành cho audit snapshot và analytics metadata, không dùng để thay quan hệ.
- Quan hệ association/composition mới dùng cascade; dữ liệu nghiệp vụ/history ưu tiên `RESTRICT` hoặc `SET NULL`.

Convention Java/React mới là kiến trúc đích: feature-first, controller mỏng, constructor injection, request/response DTO, service giữ business rule, frontend gọi API qua lớp tập trung. Chưa có code để chứng minh một coding style cụ thể.

## 3. Tổng quan hệ thống

### 3.1. Mục tiêu

LTSS là cổng thông tin du lịch chính thống cho Sơn Tây: số hóa địa điểm/di tích, bản đồ, panorama 360° và audio guide; hỗ trợ tour cá nhân, doanh nghiệp quảng bá, quy trình kiểm duyệt, analytics và audit trail cho cơ quan quản lý (`rules/ltss_database_architecture_report.md:7-23`).

### 3.2. Actor và phạm vi

| Actor | Phạm vi đã được tài liệu xác nhận | Giới hạn quan trọng |
|---|---|---|
| Guest | Search, map/place/360 public, đăng ký; các flow login/verify/recovery phải truy cập khi chưa có access token | Public access cho article/event/post/promotion/public tour chưa được actor mapping xác nhận đầy đủ |
| Tourist | Profile, favorite, personal tour, review, quiz, badge | Chỉ user `ACTIVE` và không bị khóa mới dùng authenticated function |
| Business Owner | Cùng identity, thêm role theo giả định; business profile, post, promotion, analytics, reply review | Chỉ quản lý business của mình; schema hiện giới hạn một business/user |
| Relic Manager | Place/relic, event, heritage article, quiz | Ownership và phạm vi category cần permission rõ |
| Moderator | Category và moderation article/quiz/business post/review | Phạm vi moderation của business/place/event/promotion/tour chưa chốt |
| Administrator | User/role/status, audit, report; kế thừa Moderator | Không được tự xóa; thay đổi role/status phải audit |
| Email Service | Verification, OTP/reset, email notification | Hệ thống ngoài, không phải RBAC role |
| Map Service | Tile, GPS, route, distance, geofence | Hệ thống ngoài; dữ liệu GPS không được tin cậy tuyệt đối |

### 3.3. Luồng hoạt động chính

1. Guest khám phá nội dung → đăng ký → xác minh email → login.
2. Auth service phát access token 15 phút và refresh token 7 ngày; refresh token chỉ lưu hash; logout revoke token.
3. Tourist lưu favorite, tạo tour 2–10 điểm, review, làm quiz trong geofence và nhận badge.
4. Business Owner đăng ký hồ sơ, sau khi active mới quản lý post/promotion, analytics và reply review của business mình.
5. Relic Manager tạo place/relic, event, article, quiz; nội dung thuộc scope bắt buộc được submit.
6. Moderator approve/reject trong transaction cùng status, moderation record, notification và audit.
7. Administrator quản lý account/RBAC, đọc audit và báo cáo theo khoảng ngày.

### 3.4. Public, authenticated và role-specific

| Mức truy cập | Chức năng |
|---|---|
| Public chắc chắn | Register, verify email, login, refresh/recovery theo token; place search/list/detail, map, panorama/audio public |
| Public có điều kiện | Article/event/business post/promotion/tour chỉ được public khi trạng thái và visibility hợp lệ; actor mapping cần chốt |
| Authenticated chung | Logout, profile, change password, notifications, favorite, own tours, reviews, quiz attempts/badges |
| Business Owner | Own business/profile/post/promotion, own analytics, reply review của đúng business |
| Relic Manager | Own/assigned place/relic/event/article/quiz authoring |
| Moderator | Category và moderation queue/decision theo permission |
| Administrator | User/RBAC/account state, audit, system reports; permission quản trị không được hard-code rải rác |

## 4. Module nghiệp vụ và 49 bảng

| Module | Bảng | Vai trò dữ liệu |
|---|---|---|
| Identity & RBAC | `roles`, `permissions`, `role_permissions`, `role_inheritances`, `users`, `user_roles`, `password_history`, `account_tokens` | Config, identity, association, security transaction/history |
| Notification & Audit | `notifications`, `audit_logs` | Transaction/read-state; audit history |
| Discovery & Place | `search_history`, `place_categories`, `places`, `relic_details`, `favorites` | Search history, config, master data, 1-1 extension, association |
| Content Policy | `prohibited_terms` | Config blacklist |
| Business Portal | `businesses`, `business_posts`, `tags`, `business_post_tags`, `promotions` | Master/content/association/time-bounded transaction |
| Article & Event | `events`, `article_categories`, `articles` | Content and category config |
| Media & Panorama | `media_assets`, `place_media`, `event_media`, `article_media`, `business_post_media`, `promotion_media`, `tour_media`, `review_media`, `quiz_media`, `panorama_hotspots` | Reusable media master, associations, hotspot data |
| Personal Tour | `tours`, `tour_items` | Aggregate and ordered M:N association with attributes |
| Quiz & Gamification | `quizzes`, `questions`, `answers`, `badges`, `quiz_badges`, `quiz_attempts`, `quiz_attempt_answers`, `user_badges` | Config/content, attempt transaction and immutable snapshot/award history |
| Review & Community | `reviews`, `review_replies` | Moderated transaction and official reply |
| Analytics | `engagement_event_types`, `engagement_events` | Event type config and raw append history |
| Moderation | `moderation_records` | Submission/decision history |

## 5. Phân tích database

### 5.1. Bảng chính, phụ, config, transaction và history

| Phân loại | Bảng tiêu biểu |
|---|---|
| Master/chính | `users`, `places`, `businesses`, `events`, `articles`, `media_assets`, `business_posts`, `tours`, `quizzes`, `questions`, `answers`, `reviews` |
| Config | `roles`, `permissions`, `role_permissions`, `role_inheritances`, `prohibited_terms`, `place_categories`, `article_categories`, `tags`, `badges`, `quiz_badges`, `engagement_event_types` |
| Transaction/stateful | `account_tokens`, `notifications`, `promotions`, `tour_items`, `quiz_attempts`, `reviews`, `review_replies`, `moderation_records` |
| History | `password_history`, `search_history`, `quiz_attempts`, `quiz_attempt_answers`, `user_badges`, `engagement_events`, `moderation_records`, `audit_logs` |
| Association/phụ | `role_permissions`, `role_inheritances`, `user_roles`, `business_post_tags`, `favorites`, `quiz_badges`, tám bảng `*_media` |

Semantics append-only cần hiểu chính xác:

- `audit_logs`, `password_history`, `engagement_events`, `user_badges`: chỉ insert qua luồng ứng dụng; không có API update/delete.
- `quiz_attempts` và `quiz_attempt_answers`: được cập nhật trong lúc attempt còn mở, bất biến sau terminal/submission; snapshot không bị sửa theo content hiện tại.
- `moderation_records`: tạo ở `PENDING`, được chuyển đúng một lần sang `RESOLVED` hoặc `CANCELLED`, sau đó bất biến; lần resubmit tạo row mới.
- DDL chỉ ghi comment về append-only, không tự cấm SQL `UPDATE/DELETE`; phải giới hạn DB privilege và API.

### 5.2. Quan hệ chính

- 1–0..1: `places` → `relic_details`; `places` → `businesses`; `users` → `businesses`; `reviews` → `review_replies`.
- 1–N: category → content; user → owned/created content; business → post/promotion; quiz → question → answer; quiz/user → attempts; mọi target → media/moderation/engagement tương ứng.
- M–N bằng bảng composite: role–permission, role inheritance, user–role, post–tag, user–place favorite, quiz–badge và tám quan hệ target–media.
- M–N có thuộc tính/surrogate key: tour–place qua `tour_items`; user–quiz qua `quiz_attempts`.
- Polymorphic có FK thật: `reviews`, `engagement_events`, `moderation_records` dùng nhiều FK nullable và dự kiến ép đúng một target.
- Mermaid ERD dùng `||--||` cho một số quan hệ nhưng schema chỉ bảo đảm parent có **0 hoặc 1** child. DDL là nguồn cardinality vật lý chính xác.

### 5.3. Composite primary key cần `@EmbeddedId`

Có 14 bảng:

1. `role_permissions(role_id, permission_id)`
2. `role_inheritances(role_id, inherited_role_id)`
3. `user_roles(user_id, role_id)`
4. `business_post_tags(business_post_id, tag_id)`
5. `favorites(user_id, place_id)`
6. `quiz_badges(quiz_id, badge_id)`
7. `place_media(place_id, media_asset_id)`
8. `event_media(event_id, media_asset_id)`
9. `article_media(article_id, media_asset_id)`
10. `business_post_media(business_post_id, media_asset_id)`
11. `promotion_media(promotion_id, media_asset_id)`
12. `tour_media(tour_id, media_asset_id)`
13. `review_media(review_id, media_asset_id)`
14. `quiz_media(quiz_id, media_asset_id)`

`relic_details.place_id` là shared single-column PK/FK, phù hợp `@MapsId`; `tour_items` là association entity có surrogate `id`, không phải composite PK.

### 5.4. Unique constraint quan trọng

- Identity: role code/name, permission code, user email, token hash.
- Search/config: `(user_id, normalized_keyword)`, normalized prohibited term, category name/slug, tag name/slug, badge code/name.
- Content: slug của place/event/article/post; media storage key; nullable business registration number và promotion code.
- Business: một business/place và một business/owner.
- Tour/quiz: unique place và visit order trong tour; question/answer display order; quiz–badge; attempt question order; user–badge.
- Community: bốn unique user/target của review; một reply/review; unique review media order.
- Panorama: unique display order/source panorama.

Với review, MySQL cho nhiều `NULL` trong unique index, nhưng CHECK “exactly one target” dự kiến bảo đảm row luôn rơi vào đúng một trong bốn unique constraint. Do blocker CHECK/FK ở mục 10, backend không được giả định constraint này đã chạy trước khi migration được sửa.

### 5.5. ON DELETE

- `CASCADE` chỉ cho composition/association: mapping RBAC, password/token/notification/search của user, relic detail, child question/answer, tour item, media mapping, reply/review media.
- `RESTRICT` bảo vệ category, business/content, tour item place, quiz/attempt, review target, engagement/moderation target và badge.
- `SET NULL` giữ history khi actor/uploader/source/selected content bị xóa hoặc ẩn danh.
- Content đã publish, quiz có attempt, business có analytics, user có nghiệp vụ, target có moderation/audit không được hard delete thông thường dù một số FK association có cascade.

### 5.6. Mapping JPA bắt buộc

- `BIGINT UNSIGNED` → `Long` với giả định ID không vượt `Long.MAX_VALUE`; phải ghi rõ giới hạn ứng dụng.
- `DECIMAL` → `BigDecimal`, không dùng floating point cho score/money/coordinate.
- `DATETIME(6)` → kiểu thời gian thống nhất UTC; không phụ thuộc timezone máy chạy.
- `version INT UNSIGNED` → `@Version Integer` trên user/content/business/tour/quiz/review/reply aggregates có cột này.
- JSON (`old_values`, `new_values`, `metadata`) map bằng converter/type được kiểm soát; không biến quan hệ thành JSON.
- Cascade JPA phải bám `ON DELETE`; không dùng `CascadeType.ALL` mặc định.
- Entity không được trả trực tiếp qua REST; tránh serialize vòng lặp/bí mật.

## 6. Business rule theo miền

### 6.1. Identity, authentication và authorization — BR-01..22

- RBAC lấy từ `roles`, `permissions`, `role_permissions`, `role_inheritances`, `user_roles`; chặn cycle inheritance ở service.
- Access token hết hạn sau 15 phút; refresh token sau 7 ngày và chỉ lưu hash; logout revoke refresh token/session.
- Password dài 8–32, có uppercase/lowercase/digit/special, không chứa username/email, không thuộc weak list, khác hiện tại và ba password gần nhất.
- OTP/reset link hết hạn 10 phút; resend cách tối thiểu 60 giây; token chỉ dùng một lần.
- Login trả lỗi chung, không tiết lộ email; hơn 5 lỗi liên tiếp thì khóa tạm 15 phút hoặc CAPTCHA.
- Chỉ account `ACTIVE`, email/account state hợp lệ và không nằm trong `locked_until` mới dùng authenticated function.
- Chỉ Administrator đổi role và account state theo administrative flow; thay đổi phải audit. BR-65 cần được hiểu là quản trị account, không được vô hiệu hóa self-service register/profile/password.

### 6.2. Discovery, map và media — BR-23..32, BR-51..56

- Nearby radius tối đa 5 km; exact name xếp trước distance; fuzzy/case/accent-insensitive phải được acceptance-test bằng dữ liệu tiếng Việt.
- Tối đa 10 keyword gần nhất/user, upsert và prune trong transaction.
- View chỉ tính một lần/session/target/24 giờ; cần idempotency/lock, không chỉ `COUNT` rồi insert không khóa.
- Publish place/relic cần name, description, category, coordinate và polygon Sơn Tây hợp lệ.
- Audio ≤20 MiB; panorama ≤50 MiB, tỷ lệ 2:1; backend phải inspect file/mime/dimension thực, không tin metadata client.
- Transition hotspot chỉ tới panorama cùng place/relic; hotspot không overlap; một primary media/usage/target do backend bảo đảm.

### 6.3. Tour, review và community — BR-33..38

- Tour khi submit/save-success theo rule phải có 2–10 place duy nhất và visit order duy nhất 1–10.
- Review có đúng một target, rating 1–5, comment trim ≥20, blacklist hợp lệ, tối đa ba image và một review/user/target.
- Business Owner chỉ reply review target `business_id` thuộc business có `owner_user_id` chính mình; một official reply/review.
- Eligibility “đã trải nghiệm”, target review chính thức và policy edit/re-review vẫn chưa chốt; không tự đặt check-in/payment rule.

### 6.4. Quiz và gamification — BR-39..45

- Chỉ bắt đầu quiz khi backend xác minh khoảng cách tới relic ≤200 m; lưu distance/time xác minh.
- Thời lượng 1–600 giây, không pause; timeout phải auto-submit idempotent.
- Randomize question/answer cho từng attempt, lưu seed/order và text snapshot để tái lập/audit.
- Mỗi question có 2–4 active answer, đúng một correct, text question ≤250 và answer ≤100; kiểm tra toàn aggregate trong transaction trước submit/publish.
- Badge chỉ active mới được trao và unique user–badge; chấm điểm, ghi attempt/answer snapshots và award badge trong một transaction.

### 6.5. Business, content và moderation — BR-46..64

- Business chỉ `ACTIVE` khi associated place/profile đủ name/address/phone/opening hours/coordinates; chỉ business active mới đăng nội dung và được tính analytics.
- Article/business post/quiz thuộc scope rõ phải submit trước publish; approve chuyển thẳng trạng thái public; reject bắt buộc reason.
- Published article chỉ author/owner hoặc Moderator được sửa; edit phải tạo revision flow/quay lại pending theo state machine, không sửa âm thầm nội dung đang public.
- Article/place category name/slug không rỗng, unique theo collation; FK `RESTRICT` chặn xóa khi đang dùng.
- Promotion phải có kỳ hợp lệ; percentage trong 0–100. Scope moderation và loại promotion không định lượng chưa chốt.
- Mỗi submit tạo moderation case mới; chỉ một pending case/target; status target + moderation + notification + audit cùng transaction.

### 6.6. Administration, analytics và audit — BR-65..74

- Administrator không được tự delete/deactivate bằng administrative delete flow.
- Report nhận date range hợp lệ; business chỉ tính `ACTIVE`, user activity chỉ `user_id IS NOT NULL`, event giao với kỳ báo cáo, view được dedup 24 giờ.
- Audit administrative action phải có actor, timestamp, action và detail; `actor_user_id = NULL` chỉ cho action hệ thống theo policy rõ.
- API thông thường không có update/delete audit. Không ghi password, hash, access/refresh token, OTP/reset token hoặc secret vào log/snapshot.

## 7. State machine nghiệp vụ

Actor ở bảng dưới được suy ra từ actor/BR. Dòng có `TBD` là quyết định chưa được SRS/report xác nhận.

### 7.1. User

| Actor | Nguồn → đích | Điều kiện và dữ liệu history | Cấm |
|---|---|---|---|
| System/Email flow | `PENDING_VERIFICATION → ACTIVE` | Verification token hợp lệ, unused, unrevoked; set `email_verified_at`; audit | Kích hoạt bằng token hết hạn/đã dùng |
| Auth service | active state + `locked_until` tạm thời | >5 login failure; counter/lock/audit security event | Trả lỗi phân biệt email/password |
| Administrator | `ACTIVE ↔ DEACTIVATED`; `ACTIVE → SUSPENDED`; `* → DELETED` | Permission, optimistic lock, reason, actor/time audit; revoke token | Non-admin đổi state; Admin tự xóa; `DELETED → ACTIVE` |

Mâu thuẫn: BR-10 nói auto-active ngay khi đăng ký, UC-01/schema dùng `PENDING_VERIFICATION`. Baseline hiện ưu tiên verification nhưng phải chốt trước auth implementation.

### 7.2. Business

| Actor | Nguồn → đích | Điều kiện/history | Cấm |
|---|---|---|---|
| Business Owner | tạo `PENDING`; `REJECTED → PENDING` khi resubmit | Một business/user/place; ownership; profile completeness | Quản lý business người khác; đăng content khi chưa active |
| Reviewer `TBD` | `PENDING → ACTIVE/REJECTED` | Decision/reason policy, moderation/audit, approved actor/time | `REJECTED → ACTIVE` không có submission mới |
| Admin/reviewer `TBD` | `ACTIVE ↔ SUSPENDED/INACTIVE` | Reason + audit; đồng bộ visibility của associated place theo policy | Tính business non-active trong KPI |

### 7.3. Place

| Actor | Nguồn → đích | Điều kiện/history | Cấm |
|---|---|---|---|
| Relic Manager/owner | tạo `DRAFT`; `DRAFT/REJECTED/PUBLISHED → PENDING` sau create/edit | Ownership, complete publish fields, Sơn Tây geofence, media validation | Sửa `PENDING` nếu workflow khóa nội dung |
| Moderator nếu thuộc scope | `PENDING → PUBLISHED/REJECTED`; `PUBLISHED → ARCHIVED` | Reject reason; status + moderation + notification + audit transaction | `DRAFT → PUBLISHED` bypass |
| Admin/owner theo policy | `* → DELETED` soft state | Chỉ draft/no history cho hard delete service | Hard delete target có history |

### 7.4. Article

| Actor | Nguồn → đích | Điều kiện/history | Cấm |
|---|---|---|---|
| Business Owner/Relic Manager author | `DRAFT → PENDING`; `REJECTED/PUBLISHED → PENDING` sau edit | Ownership; category/content/media valid; new moderation row | Author khác sửa; sửa pending |
| Moderator | `PENDING → PUBLISHED/REJECTED`; `PUBLISHED → ARCHIVED` | Reject reason; set `published_at`; audit/notification | Resolve moderation hai lần; publish trực tiếp |
| Owner hoặc Moderator | edit `PUBLISHED` rồi resubmit | Optimistic lock; giữ audit old/new values | Làm mất bản public/history không có audit |

### 7.5. Business Post

Giống Article, nhưng owner phải join `business_posts.business_id → businesses.owner_user_id`. Chỉ `DRAFT/REJECTED` được chỉnh theo rule hiện tại; `DRAFT → PUBLISHED` và sửa post của business khác bị cấm. `DRAFT` được giữ vì UC-18 có Save Draft dù state diagram thiếu trạng thái này.

### 7.6. Promotion

| Actor | Nguồn → đích | Điều kiện/history | Cấm |
|---|---|---|---|
| Business Owner | `DRAFT → PENDING`, resubmit sau reject | Own active business, period/discount valid | Tạo/sửa cho business khác |
| Reviewer `TBD` | `PENDING → ACTIVE/REJECTED` | Chỉ áp dụng nếu promotion thuộc moderation scope | Active ngoài validity period |
| Scheduler/system | `ACTIVE → EXPIRED` | `end_at` qua; idempotent job/audit theo policy | `EXPIRED → ACTIVE` không revision |
| Owner/reviewer `TBD` | `ACTIVE → ARCHIVED` | Permission + audit | Hard delete có engagement/moderation |

### 7.7. Event

Relic Manager tạo `DRAFT`, submit `PENDING`; reviewer `TBD` chuyển `PUBLISHED/REJECTED`; actor được chốt sau chuyển `PUBLISHED → CANCELLED/ARCHIVED`. `end_at > start_at` luôn đúng; `CANCELLED → PUBLISHED` không được phép nếu không có revision/resubmission. History dùng moderation/audit.

### 7.8. Tour

| Actor | Nguồn → đích | Điều kiện/history | Cấm |
|---|---|---|---|
| Tourist owner | tạo/sửa `DRAFT`; `DRAFT → SUBMITTED`; copy tạo tour mới có `source_tour_id` | 2–10 stop unique, ownership, source visibility hợp lệ | Sửa/copy tour không có quyền; submit count sai |
| Reviewer `TBD` | `SUBMITTED → PUBLISHED/REJECTED` | UC-09 nói review nhưng không có actor/use case duyệt | Không được tự gán reviewer |
| Owner/system | `PUBLISHED → COMPLETED/CANCELLED`; archive | Policy completion chưa chốt; audit nếu quan trọng | Terminal quay lại published không revision |

Toàn bộ moderation/publication của tour là giả định cần chốt; không triển khai trước quyết định.

### 7.9. Quiz và Quiz Attempt

| Đối tượng/actor | Nguồn → đích | Điều kiện/history | Cấm |
|---|---|---|---|
| Quiz/Relic Manager | `DRAFT → PENDING`; `REJECTED/PUBLISHED → PENDING` sau edit | Quiz gắn relic/place; question/answer aggregate valid | Publish quiz invalid/sửa làm sai history |
| Quiz/Moderator | `PENDING → PUBLISHED/REJECTED`; archive | Reject reason; moderation/audit/notification | Approve khi không đủ 2–4 answer/one correct |
| Attempt/Tourist | `IN_PROGRESS → SUBMITTED/ABANDONED` | Geofence verified; within expiry; snapshot answers | Pause/gia hạn trái rule; terminal → in-progress |
| Attempt/System | `IN_PROGRESS → AUTO_SUBMITTED` | Timeout job/request idempotent | Chấm/trao badge hai lần |

### 7.10. Review

Tourist tạo `PENDING`; nếu review thuộc moderation scope, Moderator chuyển `PENDING → VISIBLE/REJECTED`; Moderator/Admin `TBD` chuyển `VISIBLE → HIDDEN/REMOVED`. Mỗi quyết định có moderation history; reject cần reason. Không được bypass `PENDING`, vượt ba ảnh, tạo target thứ hai, reply không phải own business hoặc tạo reply thứ hai. Actor hide/remove và edit policy chưa chốt.

### 7.11. Moderation Record

| Actor | Nguồn → đích | Điều kiện/history | Cấm |
|---|---|---|---|
| Content owner/service | tạo `PENDING` | Đúng một target; không có case pending khác; target chuyển pending cùng transaction | Tạo duplicate pending do race |
| Moderator | `PENDING → RESOLVED(APPROVED/REJECTED)` | Permission, optimistic/pessimistic lock; reject reason; target state + notification + audit | Sửa terminal decision; resolve hai lần |
| Actor `TBD` | `PENDING → CANCELLED` | Chỉ trước resolution; policy actor/reason cần chốt | Cancel terminal record |

### 7.12. Token

Lifecycle suy ra: `USABLE → USED | REVOKED | EXPIRED`; không có chiều ngược. Reset/change token chỉ được đánh dấu used sau khi password và history ghi thành công. Refresh rotation/reuse-detection policy chưa được tài liệu xác định nhưng logout bắt buộc revoke.

## 8. Phạm vi backend và frontend

### 8.1. Kiến trúc đích

Backend theo feature:

```text
backend/src/main/java/<base-package>/
├── common/                 # config, security, response, exception, auditing
└── features/
    ├── auth/               # chỉ package cần dùng: controller/service/repository/dto/...
    ├── user/
    ├── role/
    ├── place/
    ├── media/
    ├── business/
    ├── article/
    ├── event/
    ├── tour/
    ├── review/
    ├── quiz/
    ├── moderation/
    ├── notification/
    ├── analytics/
    └── audit/
```

Frontend theo feature:

```text
frontend/src/
├── app/                    # router, providers, store
├── components/             # common, layout
├── features/               # auth, discovery, place, business, article, ...
├── services/               # Axios instance/cross-cutting transport only
└── pages/                  # route composition where useful
```

Không tạo package rỗng hàng loạt. Base package, Java version, Spring Boot version và JavaScript/TypeScript phải được khóa ở Phase 1; báo cáo không tự xem ví dụ folder là source đã tồn tại.

### 8.2. API contract dự thảo

Đây là contract để ước lượng phạm vi, **không phải API đã tạo**. URI/versioning, token transport, permission code, pagination và error envelope phải được khóa trước khi frontend implementation. Tên field request/response phải được định nghĩa trong OpenAPI và không lấy nguyên Entity.

| Module | Method | Endpoint/family dự thảo | Role/permission | Request DTO | Response DTO | Controller đích | Frontend API file đích |
|---|---|---|---|---|---|---|---|
| Auth | POST | `/api/v1/auth/register` | Guest | `RegisterRequest` | `RegistrationResponse` | `auth/AuthController` | `features/auth/api/authApi.js` |
| Auth | POST | `/api/v1/auth/email-verification/confirm` | Guest/token | `VerifyEmailRequest` | `MessageResponse` | `auth/AuthController` | `features/auth/api/authApi.js` |
| Auth | POST | `/api/v1/auth/login` | Guest | `LoginRequest` | `TokenResponse` + safe user summary | `auth/AuthController` | `features/auth/api/authApi.js` |
| Auth | POST | `/api/v1/auth/refresh` | Valid refresh token | `RefreshTokenRequest` hoặc secure cookie, TBD | `TokenResponse` | `auth/AuthController` | `features/auth/api/authApi.js` |
| Auth | POST | `/api/v1/auth/logout` | Authenticated/session | current refresh/session | `MessageResponse` | `auth/AuthController` | `features/auth/api/authApi.js` |
| Password | POST | `/api/v1/auth/password/{operation}` với forgot, reset, OTP hoặc change | Guest/token hoặc authenticated tùy flow | Flow-specific request DTO | `MessageResponse` | `auth/PasswordController` | `features/auth/api/passwordApi.js` |
| Profile | GET/PATCH | `/api/v1/me` | Authenticated | `UpdateProfileRequest` cho PATCH | `UserProfileResponse` | `user/ProfileController` | `features/user/api/profileApi.js` |
| User admin | GET/PATCH | `/api/v1/admin/users`, `/{id}/status`, `/{id}/roles` | Administrator + atomic permission TBD | Filter / `ChangeStatusRequest` / `AssignRolesRequest` | page/detail user DTO | `user/UserAdminController` | `features/administration/api/usersApi.js` |
| RBAC | GET/POST/PATCH | `/api/v1/admin/roles`, permissions, inheritance/mapping | Administrator + RBAC-manage | role/permission mapping DTO | role/permission DTO | `role/RoleAdminController` | `features/administration/api/rolesApi.js` |
| Place public | GET | `/api/v1/places`, `/places/{slug}` | Public for published content | query: q/category/lat/lng/radius/sort/page | `PlaceSummaryResponse` / `PlaceDetailResponse` | `place/PlaceQueryController` | `features/place/api/placeApi.js` |
| Place manage | POST/PATCH/DELETE | `/api/v1/manager/places`, `/{id}`, `/{id}/submit`, `/{id}/archive` | Relic Manager/owner; Moderator where permitted | `PlaceUpsertRequest`, `SubmitRequest` | `PlaceDetailResponse` | `place/PlaceCommandController` | `features/place/api/placeManagementApi.js` |
| Category | GET/POST/PATCH/DELETE | `/api/v1/place-categories`, `/article-categories` | Public read; Moderator manage | category request DTO | category response DTO | feature category controller | corresponding feature API file |
| Search history | GET/DELETE | `/api/v1/me/search-history` | Authenticated owner | none / item id or clear command | search history list | `place/SearchHistoryController` | `features/discovery/api/searchHistoryApi.js` |
| Media | POST/DELETE/PATCH | `/api/v1/media`, attach/detach/reorder endpoints under owning target | Content owner/manager | multipart + metadata / association request | `MediaAssetResponse` / association response | `media/MediaController` | `features/media/api/mediaApi.js` |
| Panorama | GET/POST/PATCH/DELETE | `/api/v1/media/{panoramaId}/hotspots` | Public read; content owner manage | `HotspotRequest` | `HotspotResponse` | `media/PanoramaHotspotController` | `features/place/api/panoramaApi.js` |
| Favorite | GET/PUT/DELETE | `/api/v1/me/favorites`, `/{placeId}` | Tourist/owner | none | favorite/place summary | `favorite/FavoriteController` | `features/place/api/favoriteApi.js` |
| Business | GET/POST/PATCH | `/api/v1/businesses/{id-or-slug}`, `/api/v1/me/business` | Public active read; Business Owner own write | `BusinessUpsertRequest` | `BusinessResponse` | `business/BusinessController` | `features/business/api/businessApi.js` |
| Business post | GET/POST/PATCH | public posts; `/api/v1/me/business/posts`, `/{id}/submit` | Public published; own Business Owner write | `BusinessPostUpsertRequest` | post summary/detail | `business/BusinessPostController` | `features/business/api/businessPostApi.js` |
| Promotion | GET/POST/PATCH | public active; `/api/v1/me/business/promotions`, lifecycle commands | Public active; own Business Owner write | `PromotionUpsertRequest` | promotion DTO | `business/PromotionController` | `features/business/api/promotionApi.js` |
| Article | GET/POST/PATCH | `/api/v1/articles`; manager own CRUD + submit/archive | Public published; author/Moderator rules | `ArticleUpsertRequest` | article summary/detail | `article/ArticleController` | `features/article/api/articleApi.js` |
| Event | GET/POST/PATCH | `/api/v1/events`; manager lifecycle commands | Public published; Relic Manager/permission TBD | `EventUpsertRequest` | event summary/detail | `event/EventController` | `features/event/api/eventApi.js` |
| Moderation | GET/POST | `/api/v1/moderation/queue`, `/{id}/{decision}`, target history | Moderator; cancel actor TBD | `ModerationDecisionRequest` | `ModerationRecordResponse` | `moderation/ModerationController` | `features/moderation/api/moderationApi.js` |
| Tour | GET/POST/PATCH/DELETE | `/api/v1/me/tours`, items/reorder/submit/cancel/complete; public/shared detail/copy | Tourist owner; reviewer TBD | `TourUpsertRequest`, `TourItemsRequest`, command DTO | `TourSummary/DetailResponse` | `tour/TourController` | `features/tour/api/tourApi.js` |
| Review | GET/POST/PATCH | target review list; `/api/v1/reviews`; moderation command separate | Public visible; Tourist owner; edit policy TBD | `CreateReviewRequest` / edit TBD | `ReviewResponse` | `review/ReviewController` | `features/review/api/reviewApi.js` |
| Review reply | PUT/DELETE | `/api/v1/reviews/{id}/reply` | Business Owner of target business; edit/delete policy TBD | `ReviewReplyRequest` | `ReviewReplyResponse` | `review/ReviewReplyController` | `features/review/api/reviewApi.js` |
| Quiz authoring | GET/POST/PATCH | manager quiz/question/answer CRUD + submit | Relic Manager owner; Moderator decision separate | quiz aggregate request DTOs | quiz authoring DTO | `quiz/QuizManagementController` | `features/quiz/api/quizManagementApi.js` |
| Quiz play | POST/GET | `/api/v1/quizzes/{id}/attempts`, `/attempts/{id}/submit`, history/result | Tourist + geofence | `StartAttemptRequest`, `SubmitAttemptRequest` | attempt/player/result DTO | `quiz/QuizAttemptController` | `features/quiz/api/quizAttemptApi.js` |
| Badge | GET | `/api/v1/me/badges` | Authenticated owner | pagination | `UserBadgeResponse` | `badge/BadgeController` | `features/quiz/api/badgeApi.js` |
| Notification | GET/PATCH | `/api/v1/me/notifications`, `/{id}/read` | Authenticated owner | filter/page | notification page/item | `notification/NotificationController` | `features/notification/api/notificationApi.js` |
| Engagement | POST | `/api/v1/engagement-events` | Public session or authenticated, rate-limited | `EngagementEventRequest` | 202/no sensitive body | `analytics/EngagementController` | cross-feature analytics client |
| Analytics | GET | `/api/v1/analytics/...` với `from`, `to` | Business Owner own scope / Administrator | `DateRangeFilter` | metric/series DTO | `analytics/AnalyticsController` | `features/analytics/api/analyticsApi.js` |
| Audit | GET | `/api/v1/admin/audit-logs` | Administrator/audit-read | actor/action/entity/date/page filters | read-only audit DTO page | `audit/AuditQueryController` | `features/administration/api/auditApi.js` |

Không có endpoint public để tự gán status, role, owner, `approved_by`, `published_at`, score, badge hoặc audit fields. Các field đó do service xác lập từ principal và state transition.

### 8.3. Ma trận kỹ thuật theo module

| Module | Entity/repository | Validation & authorization | Transaction/audit/notification | Test quan trọng |
|---|---|---|---|---|
| Foundation | Tất cả mapping; 14 embedded IDs; JSON/time/version | Schema validation, global DTO validation, CORS/security/error envelope | Request ID/audit writer; secret redaction | context load, Flyway on real MySQL 8, schema validate |
| Auth/account | User, role graph, password history, token | Password/email/phone; account status/lock; generic errors; token hash/expiry/revoke | Register/change/reset/login counter/logout atomic; audit security/admin changes; email dispatch | duplicate email, weak/reused password, token replay/expiry, >5 failures, disabled login |
| RBAC/admin | Role/permission/inheritance/user role | Admin permission, no cycle, no self-delete, optimistic lock | Assignment/revocation + audit same transaction | inherited permission, cycle, unauthorized role change, concurrent status edit |
| Place/discovery | Place/category/relic/search/favorite | publish completeness, Sơn Tây polygon, radius≤5, ranking, ownership | Search upsert+trim; important lifecycle audit; moderation if confirmed | accent search, exact-before-distance, geofence boundary, category restrict, 10-history race |
| Media/panorama | Media + associations + hotspot | real MIME/size/dimension/checksum; same relic; no overlap; one primary | Storage/DB consistency via outbox/cleanup; lifecycle audit | fake MIME, size/ratio edge, orphan cleanup, concurrent primary/hotspot overlap |
| Business | Business/place/post/tag/promotion | own business, active gate, complete profile, period/discount, prohibited terms | Aggregate updates; submit/decision/audit/notification | second business, cross-owner write, inactive post, invalid promotion, moderation transition |
| Article/event | Category/article/event/media | author/Moderator; content/period; category restrict; state machine | Submit/decision/status/audit/notification atomic | published edit, reject no reason, delete used category, stale version |
| Tour | Tour/items/place/media | owner, source visibility, unique/order, count 2–10 | Save/reorder/estimate/status atomic; audit important commands | 1/11 stops, duplicate/reorder race, cross-owner access, source self/cycle policy |
| Review | Review/media/reply/blacklist | exactly one target, one review, rating/comment/media, eligibility TBD, business ownership | Create+media+moderation; reply atomic; notification | multi/no target, duplicate, 4 images, blacklist, wrong owner reply, concurrent duplicate |
| Quiz | Quiz/question/answer/attempt/snapshot/badge | owner/moderator; relic; 2–4/one correct; timer/geofence; no answer leak | Start snapshot; submit+grade+award atomic; timeout idempotent | 201m/200m, timeout race, randomized snapshot, duplicate award, content edit after attempt |
| Moderation | Moderation + target repository | permission, exactly one target, one pending, reject reason, terminal immutable | Target + record + notification + audit single transaction | duplicate pending race, double resolve, missing reason, rollback consistency |
| Analytics | Event types/events | allowlisted type/target/metadata; session; date range; owner scope | Idempotent/dedup strategy; append only | 24h boundary/concurrency, anonymous vs auth KPI, range timezone |
| Notification/audit | Notification/audit | recipient ownership; admin read; no audit mutation | read-state atomic; audit append only | cross-user read, read_at coherence, audit update/delete denied/redaction |

### 8.4. Luồng tích hợp bắt buộc

```text
Frontend Route
→ Page
→ Feature component
→ Hook/event handler
→ Feature API service
→ Shared Axios instance/interceptor
→ Backend Controller
→ Service/validator/authorization
→ Repository
→ MySQL
→ Response DTO/error envelope
→ Frontend query/state
→ loading | empty | success | error UI
```

Mỗi contract phải kiểm tra hai chiều: method/URI, request/response field và type, pagination/sort/filter, token header/cookie, permission/ownership, status code, validation error, refresh retry không lặp vô hạn và UI state. Ẩn nút frontend không thay authorization backend.

### 8.5. Trang frontend cần có

| Feature | Page/route scope | Actor |
|---|---|---|
| Auth | Register, email verification result, Login, Forgot Password, Reset Password, Change Password | Guest/token; authenticated cho change |
| Account | Profile view/edit, security/session state, notification center | Authenticated owner |
| Discovery/place | Home/discovery, search results, map, place/relic detail, panorama/audio viewer, favorite list | Guest; Tourist cho history/favorite |
| Place/media management | Place list/editor, relic detail editor, media library/association, panorama hotspot editor | Relic Manager/authorized owner |
| Business public | Business directory/detail, post detail/list, active promotion list/detail | Public theo state đã chốt |
| Business owner | Business registration/profile, owner dashboard, post editor/list, promotion editor/list, review reply workspace, own analytics | Business Owner of own business |
| Article/event | Public article/event list/detail; author article/event editor/list | Public; Relic Manager/authorized author |
| Tour | My Tours, tour builder/reorder/map estimate, shared/public tour detail, copy confirmation | Tourist owner; public/shared reader theo visibility |
| Review | Target review list, create/edit page nếu policy cho phép, media picker; reply UI | Tourist; Business Owner reply |
| Quiz | Quiz availability, player/timer, result, attempt history, badges; quiz/question/answer editor | Tourist; Relic Manager author |
| Moderation | Queue/filter, submission detail/diff, approve/reject dialog, target history | Moderator/Administrator theo permission |
| Administration | User list/detail/status, role/permission management, audit viewer | Administrator |
| Analytics | Business dashboard, administrative date-range reports | Business Owner own scope; Administrator |

Mọi page dữ liệu phải có loading, empty, validation, forbidden/not-found, retryable error và success state dùng chung; player/tour/editor còn phải xử lý stale version/conflict rõ ràng.

## 9. Authorization, ownership, validation và transaction

### 9.1. Authorization model

- Authentication, account status, token lifecycle, password policy và authorization là các concern riêng.
- Khi authenticate, permission hiệu lực là union của active direct roles và toàn bộ inheritance graph đã kiểm tra cycle; Guest endpoint dùng allowlist public, không giả role Guest.
- Controller không nhận role/owner từ client làm nguồn tin. Principal lấy từ SecurityContext; service kiểm tra permission và ownership ở cùng query/transaction.
- Frontend có thể dùng permission để điều khiển UX nhưng backend luôn là enforcement point.

### 9.2. Ownership query bắt buộc

| Tác vụ | Điều kiện ownership |
|---|---|
| Business/profile/post/promotion | `businesses.owner_user_id = currentUserId`; target con phải join qua đúng business |
| Review reply | review có `business_id`, business đó thuộc current user; không được reply review place/article/tour như official business reply |
| Article/place/event/quiz | creator/author/assigned owner theo policy và permission; Moderator override chỉ ở tác vụ được cho phép |
| Tour/item/media | `tours.owner_user_id = currentUserId`; update child qua aggregate owner, không chỉ tin child id |
| Review/media | `reviews.user_id = currentUserId` và edit policy/state cho phép |
| Notification/profile/history | recipient/user id phải là current user, trừ API admin có permission rõ |
| Analytics business | mọi target/report bị scope theo business owner, không nhận arbitrary business id mà không kiểm tra |

### 9.3. Transaction boundary quan trọng

- Register: user + initial role + password history + verification token/outbox.
- Password reset/change: validate token/OTP + password/history + revoke relevant tokens + audit.
- Login failure/success: counter/`locked_until`/last-login và security audit nhất quán.
- Role/status change: current state + assignment/revocation + revoke sessions nếu cần + audit.
- Search history: normalize/upsert + prune còn 10.
- Tour: aggregate + 2–10 unique ordered items + estimates/status.
- Review: review + media associations + moderation submission; lock để chống duplicate/race.
- Quiz submit: validate full question/answer aggregate. Attempt submit: answer snapshots + score + terminal status + badge award.
- Moderation: lock pending case/target; write decision + target status/timestamp + notification + audit atomically.
- Engagement dedup: check/insert bằng idempotency strategy hoặc suitable lock; tránh race “read rồi insert”.

Email/object storage là external side effect; dùng outbox/retry/cleanup để không giả định transaction MySQL có thể rollback email/file đã gửi.

## 10. Blocker và lỗ hổng của DDL MySQL 8

### 10.1. Blocker: `CHECK` không tương thích `AUTO_INCREMENT`/foreign-key action

MySQL 8 không cho `CHECK` tham chiếu cột `AUTO_INCREMENT`; đồng thời không cho cột dùng trong `CHECK` tham gia foreign key có referential action. DDL vi phạm tại:

| Bảng | Dòng SQL | Vi phạm |
|---|---|---|
| `role_inheritances` | 54–66, đặc biệt 59–65 | `role_id`, `inherited_role_id` nằm trong CHECK self-inheritance và FK có `ON DELETE/UPDATE` |
| `tours` | 622–658, đặc biệt 623, 648–654 | CHECK tham chiếu `id AUTO_INCREMENT`; `source_tour_id` đồng thời là FK có action |
| `reviews` | 898–948, đặc biệt 919–942 | bốn target FK nằm trong CHECK exactly-one-target và có actions |
| `engagement_events` | 980–1038, đặc biệt 995–1027 | bảy target FK nằm trong CHECK và có actions |
| `moderation_records` | 1040–1122, đặc biệt 1061–1110 | chín target FK nằm trong CHECK và có actions |
| `panorama_hotspots` | 1259–1291, đặc biệt 1275–1289 | source/target FK nằm trong target/not-self CHECK và có actions |

Hệ quả: baseline có thể dừng ngay ở `role_inheritances`; không được quảng cáo “migration chạy thành công” trước khi sửa. Phương án phải được thiết kế thành migration/baseline review riêng, ví dụ chuyển invariant phù hợp sang trigger/service hoặc đổi cách biểu diễn; không bỏ FK hay business validation âm thầm.

Tài liệu tham chiếu: [MySQL 8.0 CHECK Constraints](https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html).

Ngoài ra phải pin minor version từ MySQL 8.0.16 trở lên nếu muốn `CHECK` được enforce; các bản trước 8.0.16 chỉ parse rồi bỏ qua CHECK. Việc chỉ ghi “MySQL 8.x” chưa đủ làm acceptance criterion.

### 10.2. Blocker: unique index quá dài

`media_assets.storage_key VARCHAR(1000)` dùng database charset `utf8mb4` và `UNIQUE(storage_key)` (`database/ltss_database_mysql8.sql:478`, `:490`). Max lý thuyết 4.000 byte vượt giới hạn 3.072 byte của full-column InnoDB index ở page 16 KiB; unique index vượt giới hạn luôn lỗi. Page nhỏ còn có giới hạn thấp hơn.

Không được tự cắt prefix unique vì sẽ đổi semantics. Cần quyết định storage key có thực chất là ASCII hay cần thêm hash/canonical key/đổi length-collation bằng migration. Tham chiếu: [MySQL 8.0 InnoDB Limits](https://dev.mysql.com/doc/refman/8.0/en/innodb-limits.html).

### 10.3. `CHECK` trả `UNKNOWN` khi gặp `NULL`

MySQL chấp nhận `CHECK` nếu biểu thức là `TRUE` **hoặc `UNKNOWN`**. Do đó:

- `chk_moderation_rejection_reason` cho phép `decision='REJECTED'` với `decision_reason=NULL`, vì biểu thức thành `UNKNOWN` (`database/ltss_database_mysql8.sql:1075-1077`). Backend vẫn phải bắt buộc reason; constraint cần migration sửa với `IS NOT NULL` nếu muốn DB enforce.
- `chk_promotions_percentage` cho phép `discount_type='PERCENTAGE'` với `discount_value=NULL` (`:603-605`). Cần xác nhận nullable semantics rồi validate/migrate.

### 10.4. Constraint logic chưa đầy đủ hoặc chỉ có comment

| Vấn đề | Ảnh hưởng/backend rule |
|---|---|
| `notifications`: `is_read=TRUE` vẫn cho `read_at=NULL` | Service phải set hai field cùng lúc; constraint hiện không bảo đảm đối xứng |
| `user_roles`: inactive vẫn có thể `revoked_at=NULL` | Revocation service phải ghi actor/time; audit là history chính |
| `moderation_records`: `CANCELLED` có thể có `resolved_at` tùy ý, resolved không bắt `moderator_user_id` | State validator bắt terminal coherence và actor policy |
| Hotspot `INFO` có thể mang target | Chốt semantics; hiện CHECK chỉ bắt TRANSITION có target |
| Content status không ép `submitted_at/published_at/deleted_at` tương ứng | Service state transition sở hữu timestamp, không expose setters tùy ý |
| Business `ACTIVE` không ép profile/place completeness hoặc `approved_by/at` | Validate aggregate và actor trước transition |
| Quiz attempt status không ép `submitted_at`/score coherence | Terminal grading service cập nhật atomically |
| Audit append-only chỉ là comment | DB application user không có UPDATE/DELETE; không tạo API mutation |
| Một primary media, hotspot overlap/cùng relic | Backend aggregate lock/validator |
| Một pending moderation case/target, view dedup 24h, search max 10 | Backend transaction/idempotency/lock |
| Role inheritance cycle, exactly one correct answer, minimum 2 tour items | Backend aggregate validation |

`review_media` thực tế đã kết hợp order 1–3 và unique `(review_id, display_order)`, nên DB có thể giới hạn tối đa ba row/review; backend vẫn cần thông báo validation thân thiện và transaction.

### 10.5. Rủi ro collation cho dữ liệu bảo mật

Database mặc định `utf8mb4_0900_ai_ci`. Điều này hữu ích cho search/name uniqueness nhưng cũng áp vào `account_tokens.token_hash`, `media_assets.storage_key`, `checksum_sha256` nếu không override. Hash/key phải được canonical hóa tuyệt đối; nếu dùng encoding case-sensitive như Base64, collation case-insensitive có thể coi hai chuỗi khác nhau là bằng nhau. Kiểu/collation bảo mật cần được review trong migration, không chỉ ở Java.

### 10.6. Chuyển baseline thành Flyway

- `CREATE DATABASE` và `USE ltss` không nên được bê nguyên vào migration chạy trên datasource schema; tách provisioning database khỏi V1.
- V1 phải giữ table/column/type/constraint đã được duyệt sau khi xử lý blocker; V2 seed role/permission/inheritance/event type/badge/config chính thức.
- `SET time_zone` chỉ ảnh hưởng session migration; application datasource vẫn phải chuẩn hóa UTC.
- Hibernate dùng `ddl-auto=validate`; không dùng create/update để che migration lỗi.
- Integration test phải chạy trên MySQL 8 thật/Testcontainers tương đương; H2 không đủ để xác minh CHECK/FULLTEXT/collation/UNSIGNED/JSON.

## 11. Mâu thuẫn, giả định và quyết định cần chốt

### 11.1. Critical trước khi code domain

1. **Activation:** account `ACTIVE` ngay (BR-10) hay `PENDING_VERIFICATION` tới khi click link (UC-01/schema)?
2. **Business identity/cardinality:** Business Owner là role bổ sung trên cùng user? Một user có đúng một business hay nhiều?
3. **Business/place:** mọi business bắt buộc là một `place` và hai lifecycle đồng bộ thế nào?
4. **Tour moderation:** ai duyệt tour, có bắt buộc duyệt trước public không?
5. **Review:** target chính thức nào; business-place kép xử lý ra sao; “đã trải nghiệm” xác minh bằng gì?
6. **Moderation scope:** chính xác article, business post, quiz; còn business profile/place/event/promotion/review/tour có bắt buộc không?
7. **Event model:** event là entity độc lập hay article category; article–event cardinality là gì?

### 11.2. Important

- BR-03 Admin-only lock khác system temporary lock BR-18; cần tách administrative suspension/lock và security `locked_until`.
- Moderator hay chỉ Administrator được quản lý user; BR-65 phải giới hạn administrative operations để không chặn self-service.
- Business approval actor/reason/lifecycle; đồng bộ business `ACTIVE` và place `PUBLISHED`.
- Business Post có `DRAFT` vì UC Save Draft; approved có lưu trung gian hay approve chuyển thẳng public.
- Promotion hỗ trợ percentage/fixed/other nullable thế nào; phone 10 chữ số hay E.164.
- Review/reply có được edit/delete; soft delete review có cho review lại hay unique là “một lần suốt đời”.
- Badge theo quiz hay global achievement; có point system hay không (không tự thêm loyalty points).
- Quiz đã có attempt: soft delete, clone version hay version aggregate.
- Polygon/version ranh giới Sơn Tây; GPS tolerance.
- Retention của token/notification/media/audit/analytics và danh mục event type/KPI.
- Token transport/rotation, pagination, base package, TypeScript hay JavaScript, storage provider là quyết định kỹ thuật Phase 1.

### 11.3. Optional nhưng ảnh hưởng contract/schema

Opening hours text hay structured; category tree/multi-category; multilingual; MySQL FULLTEXT hay search engine; spatial index; media versioning; tour difficulty/transport config; offset hay cursor pagination.

## 12. Rủi ro kiến trúc và biện pháp

| Rủi ro | Mức | Biện pháp bắt buộc |
|---|---|---|
| DDL không tạo được trên MySQL 8 | Blocker | Chốt migration correction, chạy real-MySQL integration trước entity work |
| Business/state/moderation còn mâu thuẫn | Cao | Decision record trước module tương ứng; không hard-code giả định |
| Partial failure moderation | Cao | One transaction cho target/record/notification/audit; outbox cho external side effect |
| Quiz content thay đổi sau attempt | Cao | Snapshot hiện có + soft delete/version policy + immutable terminal attempt |
| Hard delete làm mất history | Cao | RESTRICT/soft delete; DB privilege; service-only delete |
| Analytics tăng nhanh | Cao dài hạn | Retention, partition/aggregate sau khi có volume; không tự thêm trước số liệu |
| Race view/search/tour/answer/moderation | Trung bình | Lock/idempotency/unique/retry + concurrency test |
| Search tiếng Việt chưa đạt fuzzy | Trung bình | Acceptance dataset; MySQL v1 rồi chỉ đổi engine nếu test không đạt |
| GPS/geofence sai số | Trung bình | Polygon version + tolerance/accuracy policy + audit distance |
| Media DB khác object storage | Trung bình | Server inspect, checksum, outbox/orphan cleanup |
| Role inheritance cycle | Trung bình | Validate DAG + max traversal/caching invalidation test |
| JPA polymorphic target mapping | Trung bình | Explicit target type/factory/specification; DTO; constraint/integration tests |

## 13. Kế hoạch triển khai theo gate

### Phase 0 — Decision và schema gate

- Chốt bảy critical questions và các DDL blocker.
- Chọn versions/base package/JS-TS/token transport/pagination/error envelope.
- Tạo OpenAPI baseline, permission catalog và ADR cho ownership/state/moderation.
- Chạy V1/V2 trên MySQL 8 sạch và `ddl-auto=validate`.

### Phase 1 — Foundation

Scaffold Spring Boot/Maven và React/Vite; config env không chứa secret; Flyway; global response/exception/validation/logging; security foundation; Axios/router/layout; health check; CI/test baseline.

### Phase 2 — Identity & Access

Register/verify/login/refresh/logout/recovery/change password/profile/account status/RBAC/role management; đầy đủ audit, token revocation và security tests.

### Phase 3 — Discovery & Place

Category/place/relic/search/history/map/media/panorama/audio/favorite; geofence/file/search acceptance tests.

### Phase 4 — Business & Content

Business profile/post/promotion/article/event/media association theo scope đã chốt.

### Phase 5 — Moderation

Submit/queue/approve/reject/cancel/history/notification/audit; concurrency và rollback tests.

### Phase 6 — Community & Tour

Review/media/reply; personal tour/items/share/copy theo reviewer/eligibility policy đã chốt.

### Phase 7 — Quiz & Gamification

Quiz/question/answer/moderation/attempt/snapshot/badge/geofence; timeout/idempotency/concurrency tests.

### Phase 8 — Analytics & Administration

Engagement ingestion/dedup/report/admin dashboard/audit viewer/retention operations.

Gate sau mỗi phase: backend build, frontend build, migration clean install + upgrade, unit/integration/security/UI tests, API contract diff và danh sách lỗi/rủi ro còn lại.

## 14. Test strategy

| Tầng | Test bắt buộc |
|---|---|
| Domain/service | Business rule, state transition, ownership, permission inheritance, DTO validation |
| Repository/schema | FK/UQ/CHECK/ON DELETE, composite ID, JSON, optimistic lock, full-text/collation |
| Integration/security | JWT expiry/refresh/revoke, disabled/locked account, generic login error, admin/self/ownership, transaction rollback |
| Concurrency | duplicate review/badge/pending case, search prune, tour reorder, timeout submit, view dedup |
| Contract | OpenAPI request/response/error/pagination, frontend API client compatibility |
| Frontend | public/protected route, loading/empty/error/success, permission UX, refresh retry, forms/state |
| E2E | Guest discovery; register/verify/login; tour; business content→moderation; review/reply; quiz→badge; admin audit/report |
| Non-functional | secret redaction, upload limits, rate limit, query/index plan, accessibility/responsive map/media |

## 15. Kết quả xác minh của lượt phân tích

- Đã đối chiếu line-by-line ba tài liệu và yêu cầu đính kèm.
- Đã xác nhận 49 `CREATE TABLE` và 14 composite primary key.
- Đã xác nhận SQL nhúng trong report giống file SQL riêng.
- Đã khảo sát toàn bộ workspace: backend/frontend trống, không có source/config/test/build.
- Đã static-review DDL và đối chiếu restriction với tài liệu MySQL chính thức.
- Chưa chạy migration vì DDL có blocker đã nêu và không có môi trường project; chưa chạy backend/frontend build hoặc test vì chưa có project.
- Không sửa SQL gốc, report kiến trúc gốc hoặc `structure.md`.

## 16. Phạm vi file thay đổi

| File | Lý do |
|---|---|
| `rules/ltss_system_analysis_report.md` | Lưu báo cáo phân tích nền tảng, contract dự thảo, state machine, blocker và gate triển khai |

Không có API, page/component, entity, repository, migration hoặc test nào được tạo trong lượt này vì không có module cụ thể và schema baseline còn blocker.
