# Báo cáo hiện trạng trước khi refactor kiến trúc LTSS

## Phạm vi scan

- Frontend React/Vite: 83 file trong `frontend/src`, gồm bootstrap, router, provider, 10 feature, service HTTP, layout, page và asset.
- Backend Spring Boot: 277 file Java production, 26 file Java test, 9 resource/config/migration.
- Đã kiểm tra package declaration, import, route, API call, controller, service, repository, entity, DTO, security, email integration, test, Maven, Docker Compose và environment template.
- Baseline trước refactor: frontend production build PASS; backend `mvn test` PASS (74 test, 0 failure, 0 error, 1 integration test MySQL bị skip vì Docker daemon chưa khả dụng).

## Cấu trúc hiện tại

### Frontend

Frontend đã có `app`, `features`, `services`, `assets`, nhưng còn dùng các thư mục gốc `components`, `pages`, `styles`. Layout nằm tại `components/layout`; `App.jsx` nằm trực tiếp ở `src`; `HomePage` và `NotFoundPage` nằm trong `src/pages`. HTTP client Axios đã được tập trung tại `services/httpClient.js` và các API nghiệp vụ nằm trong từng feature.

### Backend

Backend đang tổ chức chủ yếu theo feature trước, layer sau: `com.ltss.features.<domain>.<layer>`. Cấu trúc này có phân lớp nội bộ hợp lý nhưng không khớp cấu trúc package layer-first bắt buộc (`controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `security`, `config`, `integration`, `common`). `common.config` và `common.security` cũng đang gộp hai trách nhiệm riêng vào `common`.

## Dependency và trách nhiệm sai phát hiện được

| Nhóm | Hiện trạng | Rủi ro / cách sửa |
|---|---|---|
| Frontend layout | `MainLayout` ở `components/layout` | Di chuyển sang `layouts`; layout tiếp tục chỉ dùng auth context/service ở mức phiên và UI chung. |
| Frontend shared | `FormMessage` ở feature `auth` nhưng được 8 feature khác import trực tiếp | Đây là component trung lập, di chuyển sang `shared/components` để loại bỏ dependency feature-to-feature. |
| Frontend feature boundary | `places`, `content`, `tours` import internal `community/components/ReviewSection` | Tạo public API `features/community/index.js` và chỉ import qua public export. |
| Frontend app | `App.jsx`, global stylesheet, Home/404 chưa ở package đích | Di chuyển bootstrap component vào `app`, style vào `shared/styles`, Home vào feature `home`, 404 vào `app/pages`. |
| Frontend API | Axios client đã tập trung, nhưng tên chưa theo mẫu `apiClient` | Giữ compatibility bằng public service tập trung; không tạo instance mới và không đổi contract. |
| Backend package | 9 domain nằm dưới `features/<domain>/<layer>` | Di chuyển sang `<layer>/<domain>`, sửa đồng bộ package/import và test. |
| Backend config/security | `common.config`, `common.security`, `features.auth.config`, `features.auth.security` | Tách thành top-level `config` và `security`. |
| Backend integration | Email adapter nằm tại `features.auth.email` | Di chuyển sang `integration.email`; business service chỉ phụ thuộc `AccountEmailSender` abstraction. |
| Controller/entity coupling | `ReviewController`, `ModerationController`, `AdministrationController` import persistence enum | Giữ contract enum hiện tại để không đổi request contract trong lần di chuyển; đánh dấu là coupling cần tách sang DTO enum ở bước cải tiến sau. |
| Controller/repository | Không phát hiện controller gọi repository trực tiếp | Duy trì invariant sau refactor. |
| Entity REST response | Không phát hiện controller trả JPA entity trực tiếp | Duy trì DTO response sau refactor. |
| Integration/controller | Không phát hiện controller gọi email provider trực tiếp | Di chuyển adapter nhưng giữ flow Controller → Service → Integration. |
| Mapper | Project chủ yếu dùng mapping trong service, chưa có package mapper độc lập | Chỉ tách mapper thực sự có trách nhiệm mapping (`ModerationTargetContentMapper`); không tạo wrapper rỗng. |
| Circular dependency | Không phát hiện vòng import trực tiếp bằng static scan | Build/test sau di chuyển sẽ là kiểm chứng chính. |

## Mapping di chuyển

| File/vị trí hiện tại | Vị trí mới | Lý do | Dependency cần sửa |
|---|---|---|---|
| `frontend/src/App.jsx` | `frontend/src/app/App.jsx` | Bootstrap cấp ứng dụng | `main.jsx` |
| `frontend/src/components/layout/*` | `frontend/src/layouts/*` | Layout top-level bắt buộc | Router và relative imports |
| `frontend/src/components/common/*` | `frontend/src/shared/components/*` | UI dùng chung | Consumer imports |
| `frontend/src/features/auth/components/FormMessage.jsx` | `frontend/src/shared/components/FormMessage.jsx` | Component không phụ thuộc auth, đang dùng xuyên feature | Toàn bộ feature consumer |
| `frontend/src/pages/HomePage.jsx` | `frontend/src/features/home/pages/HomePage.jsx` | UI nghiệp vụ khám phá trang chủ | Router |
| `frontend/src/pages/NotFoundPage.jsx` | `frontend/src/app/pages/NotFoundPage.jsx` | Page hạ tầng routing | Router |
| `frontend/src/styles/*` | `frontend/src/shared/styles/*` | Style dùng chung | `main.jsx` |
| `com.ltss.features.<domain>.controller.*` | `com.ltss.controller.<domain>.*` | Layer controller top-level | Package/import production và test |
| `com.ltss.features.<domain>.service.*` | `com.ltss.service.<domain>.*` | Layer service top-level | Package/import production và test |
| `com.ltss.features.<domain>.repository.*` | `com.ltss.repository.<domain>.*` | Layer repository top-level | Package/import production và test |
| `com.ltss.features.<domain>.entity.*` | `com.ltss.entity.<domain>.*` | Layer entity top-level | Package/import/JPA references |
| `com.ltss.features.<domain>.dto.*` | `com.ltss.dto.<domain>.*` | Layer DTO top-level | Package/import production và test |
| `com.ltss.features.auth.security.*`, `com.ltss.common.security.*` | `com.ltss.security.auth.*`, `com.ltss.security.*` | Tách security khỏi feature/common | Security configuration và tests |
| `com.ltss.common.config.*`, `com.ltss.features.auth.config.*` | `com.ltss.config.*`, `com.ltss.config.auth.*` | Config top-level | Configuration property imports |
| `com.ltss.features.auth.email.*` | `com.ltss.integration.email.*` | Adapter dịch vụ ngoài | Auth service/event imports |
| `ModerationTargetContentMapper` | `com.ltss.mapper.moderation.*` | Mapping entity/projection sang DTO | Moderation service |
| `com.ltss.features.auth.exception.*` | `com.ltss.common.exception.auth.*` | Exception dùng trong global handling | Handler và auth services |

## Code trùng lặp và rủi ro

- Nhiều page lặp import và render `FormMessage`; việc chuyển một file shared giảm coupling nhưng giữ nguyên UI.
- `ContentCard` đang export utility `formatDate`; đây là coupling UI/utility nội bộ feature, có thể tách trong feature mà không cần đưa global trong refactor này.
- Worktree có nhiều file modified/untracked trước khi refactor. Mọi phép di chuyển phải giữ nguyên nội dung hiện hữu; không reset hoặc xóa thay đổi của người dùng.
- Di chuyển đồng loạt package Java tác động toàn bộ import và test. Bắt buộc chạy clean compile/test để tránh kết quả cache từ `target`.
- Docker daemon hiện không khả dụng, vì vậy kiểm chứng Docker/MySQL có thể vẫn bị giới hạn dù unit và Spring context test chạy được.

## Nguyên tắc bảo toàn

Không đổi endpoint, payload, response envelope, schema/migration, auth flow, role/permission, environment variable hoặc hành vi nghiệp vụ. Không xóa class nếu chưa có vị trí thay thế; không tạo package rỗng chỉ để khớp sơ đồ.
