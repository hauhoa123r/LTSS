# Báo cáo đánh giá Cohesion và Coupling kiến trúc LTSS

## 1. Mục đích và phạm vi

Báo cáo này đánh giá cohesion, coupling và hướng phụ thuộc của kiến trúc LTSS sau quá trình tổ chức lại layer, tách service interface/implementation và áp dụng SOLID cùng Design Pattern.

Phạm vi phân tích gồm:

1. Toàn hệ thống.
2. Frontend và Backend.
3. Module và package.
4. Class và method.
5. Dependency giữa các layer.
6. Dependency với dịch vụ bên ngoài.
7. Dependency giữa frontend feature.
8. Dependency giữa backend domain.
9. Ảnh hưởng của Facade, Adapter, Strategy và SOLID.
10. Trade-off của các phương án refactor.

Đây là phân tích tĩnh trên snapshot hiện tại của source code, gồm 316 file Java production và 82 file JavaScript/JSX. Các chỉ số sử dụng gồm số import nội bộ, constructor dependency, kích thước class/method, hướng phụ thuộc package, vòng phụ thuộc domain và đặc điểm của luồng nghiệp vụ. Dependency động do Spring DI, event listener và Axios interceptor được đánh giá từ cấu hình source; báo cáo không coi các chỉ số tĩnh là phép đo LCOM/CBO tuyệt đối.

## 2. Định nghĩa

### 2.1. Cohesion

Cohesion thể hiện mức độ các thành phần bên trong một class, package hoặc module cùng phục vụ một mục tiêu thống nhất.

Cohesion cao thường giúp:

- trách nhiệm rõ ràng;
- dễ hiểu, kiểm thử và bảo trì;
- giảm số lý do khiến class phải thay đổi;
- giảm nguy cơ một thay đổi ảnh hưởng chức năng không liên quan.

Tuy nhiên, tối đa hóa cohesion một cách cực đoan có thể tạo quá nhiều class nhỏ, tăng số abstraction và dependency, làm luồng nghiệp vụ khó theo dõi và debug.

### 2.2. Coupling

Coupling thể hiện mức độ một thành phần phụ thuộc vào cấu trúc, contract hoặc hành vi của thành phần khác.

Coupling thấp thường giúp:

- giới hạn phạm vi lan truyền thay đổi;
- dễ thay implementation và kiểm thử độc lập;
- cô lập framework hoặc dịch vụ bên ngoài;
- tăng khả năng tái sử dụng.

Coupling thấp không phải mục tiêu tuyệt đối. Việc tạo quá nhiều interface, adapter, facade, mapper hoặc event chỉ để giảm dependency trực tiếp có thể tạo coupling gián tiếp, hidden coupling và temporal coupling khó kiểm soát hơn.

## 3. Thang điểm

### 3.1. Cohesion: 1–10, điểm cao hơn là tốt hơn

Điểm cohesion được tổng hợp từ:

| Tiêu chí | Điểm tối đa |
|---|---:|
| Các method cùng phục vụ một trách nhiệm chính | 4 |
| Các thành phần chia sẻ cùng invariant, dữ liệu hoặc workflow | 2 |
| Class/package có ít lý do độc lập để thay đổi | 2 |
| Tên và boundary phản ánh đúng mục tiêu | 2 |

| Khoảng điểm | Diễn giải |
|---:|---|
| 1–2 | Trách nhiệm rời rạc; God Object hoặc junk drawer rõ ràng |
| 3–4 | Nhiều lý do thay đổi; business, persistence, mapping và integration bị trộn |
| 5–6 | Có mục tiêu chung nhưng orchestration, policy, mapping hoặc side effect chưa được tách hợp lý |
| 7–8 | Trách nhiệm rõ; phần lớn class phục vụ cùng một bounded workflow |
| 9–10 | Boundary rất rõ, contract nhỏ, ít lý do thay đổi |

### 3.2. Coupling risk: 1–10, điểm cao hơn là rủi ro hơn

| Tiêu chí | Điểm tối đa |
|---|---:|
| Static fan-out và số collaborator | 3 |
| Sai hướng dependency hoặc circular dependency | 3 |
| Control, content, hidden hoặc temporal coupling | 2 |
| Phạm vi lan truyền thay đổi và độ khó kiểm thử | 2 |

| Khoảng điểm | Diễn giải |
|---:|---|
| 1–2 | Dependency tối thiểu, rõ ràng và ổn định |
| 3–4 | Coupling tự nhiên qua contract hoặc cùng layer |
| 5–6 | Coupling trung bình; có điểm xuyên biên nhưng còn kiểm soát được |
| 7–8 | Fan-out cao, phụ thuộc implementation/internal hoặc có domain cycle |
| 9–10 | Content coupling mạnh, circular dependency phức tạp hoặc thay đổi lan rộng |

Coupling risk không phải là điểm chất lượng đảo ngược. Một class composition root có thể có coupling risk cao nhưng vẫn đúng kiến trúc vì nhiệm vụ của nó là kết nối các module.

## 4. Kết luận tổng quan

| Phạm vi | Cohesion | Coupling risk | Đánh giá |
|---|---:|---:|---|
| Toàn hệ thống | 7 | 6 | Layer rõ, nhưng domain và frontend feature còn một số dependency xuyên biên |
| Frontend với Backend | 8 | 3 | Giao tiếp qua REST; không chia sẻ trực tiếp implementation |
| Backend | 7 | 6 | Service layer đang gánh phần lớn orchestration và dependency |
| Frontend | 6 | 6 | Cấu trúc feature rõ nhưng public boundary chưa được thực thi chặt |

Kiến trúc hiện tại chưa có God Module ở cấp toàn hệ thống. Rủi ro chính tập trung tại một số application service lớn, mapper truy cập persistence, hai vòng dependency backend domain và import nội bộ giữa frontend feature.

## 5. Đánh giá dependency giữa backend layer

Các dependency phổ biến nhất trong static import graph:

| Hướng dependency | Số edge | Nhận định |
|---|---:|---|
| `service -> entity` | 116 | Cao nhưng phần lớn tự nhiên trong application service hiện tại |
| `service -> repository` | 102 | Service chịu trách nhiệm transaction/persistence orchestration |
| `service -> dto` | 96 | Application service đang trực tiếp dựng response model |
| `service -> common` | 75 | Chủ yếu exception, response và logging |
| `controller -> common` | 53 | Response/error contract dùng chung |
| `repository -> entity` | 52 | Dependency đúng hướng |
| `controller -> dto` | 48 | Data coupling mong đợi |
| `dto -> entity` | 27 | DTO mapping tĩnh đang biết persistence model |
| `controller -> service` | 26 | Đúng hướng dependency |
| `service -> security` | 19 | Business workflow cần principal/security context |
| `mapper -> repository` | 2 | Sai trách nhiệm và sai boundary |

Không phát hiện `controller -> repository`, `repository -> service` hoặc `entity -> service`. Vì vậy, không có vòng dependency nghiêm trọng giữa các layer chính.

| Layer/package | Cohesion | Coupling risk | Kết luận |
|---|---:|---:|---|
| `controller` | 8 | 4 | Chủ yếu nhận request, gọi service và trả DTO |
| `service` | 6 | 7 | Nhiều orchestration, mapping và side effect tập trung |
| `repository` | 8 | 3 | Phụ thuộc entity là cần thiết |
| `entity` | 8 | 3 | Domain/persistence model được chia theo domain |
| `dto` | 7 | 5 | Có cấu trúc domain nhưng còn phụ thuộc entity/projection |
| `mapper` | 5 | 8 | Mapper moderation truy vấn repository và dispatch theo kiểu runtime |
| `integration` | 9 | 3 | SDK bên ngoài được cô lập tốt |
| `common` | 7 | 3 | Chưa trở thành junk drawer; các subpackage có mục tiêu rõ |

Package cấp cao `service` và `dto` có nhiều domain, nhưng đây là hệ quả của kiến trúc layer-first đã thống nhất. Không nên kết luận cohesion thấp chỉ từ số file. Cohesion cần được đánh giá tại `service/<domain>` và `dto/<domain>`.

## 6. Đánh giá backend domain

| Domain | Cohesion | Coupling risk | Bằng chứng chính |
|---|---:|---:|---|
| `auth` | 6 | 7 | Registration, session, reset password, token, audit và email orchestration cùng nằm trong service lớn |
| `content` | 7 | 6 | Có dependency hợp lý tới auth/place nhưng nhiều DTO/service |
| `place` | 7 | 5 | Discovery và place data liên quan chặt chẽ |
| `quiz` | 5 | 8 | Quiz attempt trộn grading, badge, notification, audit và persistence |
| `community` | 5 | 8 | Review trộn media, policy, moderation, notification và mapping |
| `moderation` | 7 | 6 | Facade lớn nhưng target-specific behavior đã được tách thành Strategy |
| `analytics` | 6 | 7 | Tổng hợp dữ liệu từ nhiều bounded context là cần thiết nhưng fan-out cao |
| `administration` | 7 | 6 | Cross-domain lookup cần thiết cho quản trị và audit |
| `tour` | 7 | 5 | Workflow và dependency tương đối tập trung |
| `system` | 9 | 2 | Boundary nhỏ, trách nhiệm rõ |

### 6.1. Circular dependency backend domain

Không có circular layer dependency, nhưng có hai vòng dependency domain.

#### `quiz <-> moderation`

- `QuizAttemptServiceImpl` phụ thuộc `NotificationEntity` và `NotificationRepository` thuộc moderation.
- `QuizModerationStrategy` phụ thuộc quiz entity, repository và validator.

#### `community <-> moderation`

- `ReviewServiceImpl` phụ thuộc notification repository/entity và `ModerationService`.
- `ReviewModerationStrategy` phụ thuộc review entity và repository.

Hai vòng này hình thành vì notification đang được đặt trong moderation package trong khi đây là capability được nhiều domain sử dụng. Đây là coupling xấu vì thay đổi notification hoặc cấu trúc moderation có thể lan sang quiz/community.

Hướng xử lý phù hợp là tạo một `NotificationPort` hoặc application service trung lập. Quiz và community phụ thuộc abstraction này; implementation có thể nằm trong notification/moderation adapter. Không nên đưa toàn bộ nghiệp vụ notification vào `common`, vì `common` sẽ trở thành một domain không có boundary rõ.

## 7. Đánh giá class và method

### 7.1. Các class có rủi ro cao

| Class | LOC | Constructor dependency | Cohesion | Coupling risk | Đánh giá |
|---|---:|---:|---:|---:|---|
| `QuizAttemptServiceImpl` | 328 | 15 | 5 | 8 | God Service rõ nhất |
| `ReviewServiceImpl` | 222 | 14 | 5 | 8 | Đang tiến gần God Service |
| `AuthServiceImpl` | 322 | 13 | 6 | 7 | Nhiều use case của identity lifecycle |
| `ModerationServiceImpl` | 377 | 9 | 7 | 6 | Facade lớn nhưng bounded workflow rõ |
| `ModerationTargetContentMapper` | 136 | 3 | 5 | 8 | Mapper kiêm loader và runtime dispatcher |
| `AuditEntityDisplayNameResolver` | 136 | 11 | 8 | 6 | Fan-out cao nhưng phục vụ một trách nhiệm thống nhất |

Số dependency không đủ để kết luận một class là God Service. `AuditEntityDisplayNameResolver` có fan-out cao vì phải resolve tên nhiều entity, nhưng mọi dependency phục vụ cùng một mục tiêu. Tách class này chỉ để giảm constructor parameter có thể làm luồng audit khó theo dõi hơn.

### 7.2. `QuizAttemptServiceImpl`

Class đang đảm nhiệm:

- khởi tạo attempt;
- load và trả chi tiết attempt;
- validate submission;
- chấm điểm;
- tính khoảng cách địa lý;
- trao badge;
- tạo notification;
- audit;
- mapping response.

Method `grade` vừa validate, load question/answer, tính điểm, mutate attempt, trao badge, audit và mapping response. Các bước cùng thuộc use case chấm bài nên có functional cohesion, nhưng calculation, persistence và side effect có lý do thay đổi độc lập.

Nên tách:

- `QuizGradingCalculator`: tính điểm thuần, không truy cập repository;
- `BadgeAwardService`: chính sách và persistence badge;
- `QuizAttemptServiceImpl`: transaction và orchestration.

Không nên tách mỗi bước thành một command class riêng nếu project chưa cần pipeline thay đổi động.

### 7.3. `ReviewServiceImpl`

Class đang trộn review/reply, media, prohibited terms, target validation, moderation, notification và response mapping. Đây là nhiều lý do thay đổi trong cùng class.

Nên xem xét:

- `ReviewContentPolicy` cho prohibited terms và validation;
- `ReviewTargetResolver` cho các target type;
- notification port trung lập;
- giữ CRUD và transaction orchestration tại `ReviewServiceImpl`.

### 7.4. `AuthServiceImpl`

Registration, verify/resend, login, refresh, logout, forgot/reset password, token, audit và email đều thuộc identity lifecycle, vì vậy class không hoàn toàn thiếu cohesion. Tuy nhiên, mỗi nhóm use case có security policy và lý do thay đổi khác nhau.

Cấu trúc phù hợp là giữ `AuthService` làm facade ổn định cho controller, sau đó ủy quyền cho:

- registration service;
- session/token service;
- password recovery service.

Facade giảm coupling của controller nhưng không được phép che giấu một God Service mới phía sau.

### 7.5. `ModerationTargetContentMapper`

Mapper hiện inject `QuestionRepository`, `AnswerRepository`, nhận `Object`, dispatch bằng `instanceof` và truy vấn database khi map quiz.

Đây là:

- persistence coupling vì mapper gọi repository;
- content coupling vì mapper phải biết concrete target class;
- control coupling vì kiểu runtime quyết định nhánh xử lý;
- vi phạm SRP vì mapping và loading bị gộp.

Nên đưa việc load aggregate vào moderation strategy hoặc một `ModerationContentLoader<T>`. Mapper chỉ nhận typed snapshot và chuyển sang DTO. Đây là refactor có ưu tiên cao nhất vì nó sửa hướng dependency, không chỉ giảm số collaborator.

## 8. Frontend cohesion và coupling

### 8.1. Dependency tổng quan

`layouts` và `shared` hiện không import project module khác. Đây là hướng dependency tốt:

```text
app -> features -> shared/services
app -> layouts
```

`AppRouter` có fan-out cao tới nhiều page nhưng đây là coupling cần thiết tại composition root. Chia router chỉ để giảm số import có thể làm cấu hình điều hướng phân tán và khó kiểm tra hơn.

### 8.2. Đánh giá theo feature

| Feature | Cohesion | Coupling risk | Đánh giá |
|---|---:|---:|---|
| `community` | 8 | 5 | Component/review workflow tương đối tập trung |
| `auth` | 7 | 6 | Context được nhiều feature import trực tiếp |
| `moderation` | 7 | 6 | API được feature khác dùng trực tiếp |
| `content` | 6 | 7 | Phụ thuộc community, analytics, places và moderation |
| `places` | 6 | 7 | Phụ thuộc auth, community và analytics |
| `quiz` | 6 | 7 | Phụ thuộc auth, places và moderation |
| `tours` | 7 | 7 | Phụ thuộc auth, community, analytics và places |
| `administration` | 6 | 7 | Nhiều UI label và API quản trị |
| `analytics` | 5 | 8 | Dashboard tổng hợp administration, moderation và analytics |
| `system` | 8 | 3 | Boundary nhỏ |
| `home` | 8 | 2 | Ít dependency business |
| `shared` | 9 | 1 | Không phụ thuộc feature, nhưng hiện còn nhỏ |

Frontend chưa có circular feature import rõ ràng, nhưng có nhiều import xuyên biên như:

- analytics import administration API/label và moderation API;
- content import places API, analytics hook và moderation API;
- places, quiz và tours import auth context;
- quiz import moderation API.

Các feature nên cung cấp public contract thông qua `features/<feature>/index.js`, chẳng hạn `useAuth`, `useTrackView` hoặc moderation command. Tuy nhiên, chỉ tạo barrel file không làm coupling biến mất; public API phải ổn định và che giấu implementation nội bộ.

### 8.3. Layout Facade

`useMainNavigation` ở app layer đọc auth/role và tạo navigation view model. `AppMainLayout` truyền model xuống `MainLayout` qua props.

Kết quả:

- `MainLayout` không còn biết auth context hoặc feature configuration;
- coupling business được chuyển về composition layer;
- layout có cohesion tốt hơn và dễ test bằng props.

Coupling tổng thể vẫn tồn tại trong app hook, nhưng đây là coupling có chủ đích tại đúng boundary.

## 9. Dependency với dịch vụ bên ngoài

Email integration có boundary tốt:

```text
Auth/Account service
-> AccountEmailEvent
-> AccountEmailEventListener AFTER_COMMIT
-> AccountEmailSender
-> SmtpAccountEmailSender
-> JavaMailSender
```

Chỉ `SmtpAccountEmailSender` biết Spring Mail SDK. `AccountEmailSender` là port nhỏ và listener phụ thuộc abstraction. `AFTER_COMMIT` tránh gửi email khi transaction database rollback.

| Thành phần | Cohesion | Coupling risk |
|---|---:|---:|
| `integration.email` | 9 | 3 |

Điểm chưa tối ưu là business service publish `AccountEmailEvent` nằm trong package `integration.email`. Dependency direction sẽ sạch hơn nếu command/event contract nằm trong application port, còn integration chỉ chứa listener và adapter.

Project chưa có map/storage SDK implementation đủ lớn để đánh giá Adapter tương đương. Không nên tạo package hoặc abstraction rỗng trước khi có dependency thật.

## 10. Phân loại coupling hiện tại

### 10.1. Data coupling

Controller chủ yếu truyền ID, request DTO, response DTO và enum. Đây là coupling cần thiết. Tuy nhiên, DTO có nhiều static factory nhận entity, khiến web contract biết persistence model. Với mapping đơn giản, trade-off này vẫn chấp nhận được; không cần tạo mapper riêng cho mọi DTO.

### 10.2. Control coupling

- Enum target type được dùng để chọn moderation Strategy.
- Mapper dùng `instanceof` để chọn luồng mapping.
- Auth flow kiểm tra chuỗi error code như `TOKEN_REQUEST_RATE_LIMITED`.

Registry/Strategy đã giảm switch lớn trong moderation. String error code nên được thay bằng typed exception hoặc error-code enum.

### 10.3. Content coupling

- Mapper trực tiếp truy cập repository.
- Controller sử dụng persistence enum làm API parameter.
- Frontend feature import `api`, `context` hoặc `hooks` nội bộ của feature khác.
- Moderation repository/query biết trạng thái của content domain.

Đây là loại coupling nên xử lý trước việc đơn thuần giảm số constructor dependency.

### 10.4. Hidden coupling

- Spring tự động inject danh sách Strategy vào registry.
- Email side effect được kích hoạt bởi event listener mà caller không thấy trực tiếp.
- Axios interceptor tự refresh token và retry request.
- Shared `refreshRequest` điều phối nhiều request 401.

Hidden coupling có thể chấp nhận khi abstraction có giá trị, nhưng cần test registry completeness, event delivery và concurrent token refresh.

### 10.5. Temporal coupling

Các chuỗi có thứ tự bắt buộc gồm:

- register: lưu user -> role -> password history -> token -> email event;
- moderation: validate -> đổi trạng thái target -> lưu case -> notification -> audit;
- quiz grading: tính điểm -> lưu attempt -> award badge -> notification -> audit;
- HTTP 401: refresh -> cập nhật token -> retry -> giải phóng shared refresh promise.

Không nên loại bỏ toàn bộ temporal coupling bằng event bất đồng bộ. Một số bước cần nằm trong cùng transaction hoặc cần thứ tự rõ. Cách phù hợp là tách calculation/policy thuần khỏi orchestration, sau đó giữ transaction boundary tại facade/application service.

## 11. Ảnh hưởng của SOLID và Design Pattern

### 11.1. Facade

`ModerationService` và `AppMainLayout` làm giảm fan-out của consumer. Tuy nhiên, Facade không làm tổng dependency biến mất; nó tập trung dependency tại boundary. Facade có giá trị khi consumer chỉ cần biết một use-case contract, nhưng có thể che giấu God Service nếu implementation tiếp tục tích lũy policy, mapping và integration.

### 11.2. Adapter

Email Adapter cô lập framework bên ngoài tốt và cho phép thay SMTP implementation mà không sửa auth business logic. Adapter chỉ có giá trị khi boundary bên ngoài thực sự có khả năng thay đổi hoặc cần fake trong test. Không nên tạo adapter cho class nội bộ ổn định chỉ để tăng số abstraction.

### 11.3. Strategy và Registry

Moderation và email Strategy cải thiện OCP: thêm target/email type không cần sửa chuỗi `switch`. Đổi lại, Spring list injection và registry lookup tạo hidden coupling. Registry cần phát hiện duplicate và nên kiểm tra thiếu enum implementation sớm tại startup.

### 11.4. Dependency Injection

Constructor injection làm dependency hiển thị rõ và hỗ trợ test. Tuy nhiên, 13–15 constructor dependency là tín hiệu class đang orchestration quá nhiều trách nhiệm; DI không tự động bảo đảm SRP hoặc coupling thấp.

### 11.5. Interface Segregation và over-abstraction

Một số service interface còn wildcard import entity/repository dù signature chỉ cần DTO. Điều này làm contract vẫn phụ thuộc implementation detail.

Nên:

1. Xóa import không dùng khỏi interface.
2. Giữ interface cho external port, Strategy, policy, facade và boundary có khả năng thay implementation.
3. Không bắt buộc mọi helper/service nội bộ đơn giản phải có interface nếu chỉ có một implementation và không tạo boundary có ý nghĩa.

## 12. Refactor đề xuất và trade-off

| Ưu tiên | Refactor | Lợi ích | Chi phí/trade-off |
|---|---|---|---|
| P0 | Loại repository khỏi moderation mapper | Khôi phục layer boundary, mapping dễ test | Thêm loader hoặc snapshot model |
| P0 | Tạo notification port trung lập | Phá `quiz/community <-> moderation` cycle | Thêm abstraction và phải xác định transaction semantics |
| P1 | Tách `QuizGradingCalculator` | Thuật toán thuần, giảm God Service | Thêm collaborator và mapping dữ liệu đầu vào |
| P1 | Tách badge awarding | Cô lập policy/persistence badge | Không nên tách nếu badge chỉ là vài dòng ổn định |
| P1 | Tách review policy/target resolver | Giảm dependency và branch của review service | Tăng số class; cần giữ workflow dễ truy vết |
| P1 | Auth facade với use-case services | Giảm lý do thay đổi, giữ controller ổn định | Facade vẫn có fan-out tới các use case |
| P1 | Public API cho frontend feature | Ngăn import internals | Barrel file phải được quản trị, tránh export mọi thứ |
| P2 | Dùng chung geolocation hook/service | Cô lập browser API và bỏ duplication | Không đáng nếu chỉ còn một consumer |
| P2 | Dọn import service interface | Contract sạch, rủi ro thấp | Không thay đổi coupling runtime |
| P2 | ArchUnit/ESLint boundary rule | Ngăn cycle và import sai tái xuất hiện | Tăng maintenance cho architecture test |

## 13. Những trường hợp không nên tối ưu cực đoan

Không nên:

- tách `AppRouter` chỉ vì có nhiều import; đây là composition root;
- tách `AuditEntityDisplayNameResolver` chỉ vì có nhiều repository dependency;
- biến mọi notification và audit thành event bất đồng bộ;
- tạo mapper class cho từng DTO đơn giản;
- tạo interface cho mọi class chỉ để tuân theo hình thức DIP;
- đưa mọi code dùng chung vào `common` hoặc `shared`;
- chuyển component business vào `shared` chỉ để giảm import chéo;
- chia class theo số dòng nếu các method vẫn cùng thay đổi theo một invariant;
- áp dụng Strategy khi chỉ có một implementation ổn định;
- tạo package `integration.map` hoặc `integration.storage` trước khi có integration thật.

Mục tiêu đúng không phải là cohesion cao nhất và coupling thấp nhất về mặt số học. Mục tiêu là mỗi dependency phải có lý do rõ ràng, đi đúng hướng, nằm tại boundary ổn định và có chi phí thay đổi phù hợp với quy mô hệ thống.

## 14. Kế hoạch thực hiện đề xuất

### Giai đoạn 1: sửa dependency direction

1. Tách repository loading khỏi `ModerationTargetContentMapper`.
2. Tạo notification application port và loại dependency vòng.
3. Dọn import thừa trong service interface.
4. Thêm architecture tests cho layer/package boundary.

### Giai đoạn 2: giảm God Service

1. Tách pure grading calculator khỏi quiz attempt orchestration.
2. Tách review content policy và target resolver.
3. Tách auth implementation theo registration, session và password recovery, giữ facade hiện tại.

### Giai đoạn 3: củng cố frontend feature boundary

1. Xác định public API cho auth, analytics, moderation, places và community.
2. Thay direct internal import bằng public contract hoặc app-level composition.
3. Thêm ESLint import-boundary rule.
4. Chỉ trích xuất browser adapter/hook khi có từ hai consumer thực tế trở lên.

## 15. Kết luận

Kiến trúc LTSS hiện có layer separation tốt và các pattern gần đây đã cải thiện rõ OCP, SRP và dependency direction. Cohesion toàn hệ thống đạt khoảng 7/10; coupling risk khoảng 6/10.

Ba thay đổi có giá trị kiến trúc cao nhất là:

1. Khôi phục mapper boundary bằng cách loại repository khỏi mapper.
2. Phá vòng dependency notification giữa quiz/community và moderation.
3. Tách calculation/policy khỏi `QuizAttemptServiceImpl` và `ReviewServiceImpl` nhưng giữ transaction orchestration tập trung.

Các refactor tiếp theo nên được đánh giá theo hướng dependency, lý do thay đổi và khả năng kiểm thử, thay vì chỉ dựa trên số class, số interface hoặc số dependency.
