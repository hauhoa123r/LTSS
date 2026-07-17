# SOLID và Design Pattern Refactor

## Phạm vi và nguyên tắc

Lần refactor này chỉ áp dụng pattern tại các hotspot có chi phí thay đổi hoặc coupling rõ ràng. Endpoint, DTO contract, database schema, role/permission và nghiệp vụ không thay đổi.

## Hotspot đã xử lý

### Moderation workflow

Trước refactor, `ModerationServiceImpl`:

- dài 497 dòng;
- constructor phụ thuộc 16 collaborator;
- biết trực tiếp repository và lifecycle của sáu loại target;
- dùng nhiều `switch`, `instanceof` và chuỗi `if/else` cho load, owner, status, version và transition;
- phải sửa service trung tâm mỗi khi thêm loại nội dung kiểm duyệt.

Sau refactor:

- `ModerationTargetStrategy` mô tả capability chung của target kiểm duyệt;
- `SubmittableModerationTargetStrategy` tách riêng capability submit thủ công theo ISP;
- sáu strategy domain quản lý repository, ownership và lifecycle của target tương ứng;
- `ModerationTargetStrategyRegistry` là registry/factory chọn implementation theo `ModerationTargetType` và chặn đăng ký trùng;
- `AbstractModerationTargetStrategy` và `AbstractSubmittableModerationTargetStrategy` dùng Template Method để chuẩn hóa cast/delegation mà không lặp orchestration;
- `ModerationServiceImpl` còn 9 dependency và tập trung vào transaction/use-case workflow;
- các switch/type-chain trong moderation service được loại bỏ;
- review không còn bị ép implement thao tác submit không hỗ trợ, bảo đảm LSP/ISP.

Pattern áp dụng: Strategy, Factory/Registry, Template Method, Dependency Injection, Application Facade.

### Email integration

Trước refactor, SMTP adapter vừa gửi mail vừa switch để tạo subject/body cho từng loại email.

Sau refactor:

- `AccountEmailTemplate` là strategy contract;
- mỗi email type có template riêng;
- `AccountEmailTemplateRegistry` chọn template và chặn duplicate;
- `AbstractLinkAccountEmailTemplate` gom bước dựng URL chung;
- `SmtpAccountEmailSender` chỉ còn trách nhiệm adapter SMTP/delivery;
- thêm email type mới bằng template mới, không sửa sender.

Pattern áp dụng: Adapter, Strategy, Factory/Registry, Template Method, Dependency Injection.

### Account event flow

Flow gửi email sau commit đã có kiến trúc phù hợp và được giữ nguyên:

```text
Auth/Account Service
→ ApplicationEventPublisher
→ AccountEmailEvent
→ @TransactionalEventListener(AFTER_COMMIT)
→ AccountEmailSender adapter
```

Pattern áp dụng: Observer. Việc giữ listener ở `AFTER_COMMIT` tránh gửi email cho transaction đã rollback.

### Frontend main layout

Trước refactor, `MainLayout` vừa render layout vừa đọc auth context, hiểu role, feature config, redirect và xây navigation model. Điều này tạo dependency `layouts → feature internal`.

Sau refactor:

- `useMainNavigation` ở app layer gom role/navigation policy và tạo view model;
- `AppMainLayout` làm composition/facade giữa app state và layout;
- `MainLayout` chỉ nhận props, quản lý interaction UI và render;
- `layouts` không còn import feature internal.

Pattern áp dụng: Facade/composition, custom hook, Dependency Injection qua props; đồng thời cải thiện SRP và dependency direction.

## Đánh giá SOLID

| Nguyên tắc | Thay đổi |
|---|---|
| SRP | Tách moderation target lifecycle, email template và navigation policy khỏi orchestrator/render adapter. |
| OCP | Thêm moderation target hoặc email type bằng strategy mới và đăng ký qua DI, không sửa switch trung tâm. |
| LSP | Tách `SubmittableModerationTargetStrategy`; review không còn implementation giả hoặc unsupported operation. |
| ISP | Contract moderation nền không ép mọi target hỗ trợ submit thủ công. |
| DIP | Moderation workflow phụ thuộc strategy abstraction/registry; SMTP sender phụ thuộc template abstraction; Spring inject danh sách implementation. |

## Pattern đã xem xét nhưng không áp dụng

- Builder: DTO/entity hiện có constructor/factory rõ ràng; thêm builder lúc này chỉ tăng API surface và có thể che validation bắt buộc.
- Specification: chưa có tổ hợp filter domain tái sử dụng đủ phức tạp; query hiện nằm đúng repository. Có thể xem xét khi search/filter động tăng lên.
- Chain of Responsibility: validation hiện ngắn và có thứ tự rõ trong từng use case; tách thành chain sẽ tăng số class mà chưa tạo khả năng mở rộng thực tế.
- Factory Method riêng cho entity: entity creation hiện có named factory phù hợp (`pending`, DTO factory); không cần thêm factory wrapper.

## Kiểm thử bổ sung

- Registry moderation chọn đúng strategy, báo lỗi target không hỗ trợ và chặn duplicate registration.
- Registry email chọn đúng template, dựng đúng verification link và chặn duplicate registration.
- Các test workflow moderation hiện hữu tiếp tục xác nhận submit, approve, review, quiz, optimistic version, audit và rollback propagation.

## Contract

Không có API contract hoặc database contract thay đổi trong lần refactor này.
