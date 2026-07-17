# Sự cố trang bài viết và sự kiện bị trắng

## 1. Hiện tượng

Ngày 17/07/2026, hai trang công khai sau chỉ hiển thị phần khung trang hoặc bị trắng phần nội dung:

- `http://localhost:5173/articles`
- `http://localhost:5173/events`

Route React, Vite và backend vẫn hoạt động. API `/api/v1/articles` và `/api/v1/events` đều trả HTTP 200 cùng dữ liệu hợp lệ, vì vậy đây không phải lỗi định tuyến, CORS hay lỗi kết nối cơ sở dữ liệu.

## 2. Nguyên nhân gốc

Lỗi nằm trong `frontend/src/features/content/components/ContentCard.jsx`. Phiên bản cũ tạo một object chứa cấu hình của tất cả loại thẻ rồi mới chọn cấu hình theo `type`:

```jsx
const config = {
  business: {
    title: item.place.name,
  },
  article: {
    title: item.title,
  },
  event: {
    title: item.title,
  },
}[type]
```

JavaScript tính giá trị của toàn bộ object literal trước khi thực hiện `[type]`. Vì vậy, khi đang render một bài viết hoặc sự kiện, biểu thức của nhánh `business` vẫn được chạy và vẫn truy cập `item.place.name`.

Trường `place` không được đảm bảo tồn tại trong dữ liệu công khai:

- `articles.place_id` và `events.place_id` cho phép `NULL` trong schema.
- API không trả DTO địa điểm nếu địa điểm liên kết không ở trạng thái `PUBLISHED`, đúng theo ranh giới dữ liệu được mô tả trong `docs/phase4-business-content.md`.
- Tại thời điểm kiểm tra, trang đầu của API bài viết trả 12 bản ghi, trong đó 8 bản ghi có `place = null`.
- Response danh sách sự kiện tại thời điểm kiểm tra không có thuộc tính `place`.

Do đó trình duyệt phát sinh lỗi runtime tương đương:

```text
TypeError: Cannot read properties of null/undefined (reading 'name')
```

Lỗi xảy ra trong lúc React render danh sách, khiến toàn bộ cây component của danh sách không được tạo và người dùng nhìn thấy trang trắng.

## 3. Cách khắc phục

`ContentCard` được đổi sang `switch (type)` để chỉ tạo cấu hình dành cho đúng loại dữ liệu đang render. Nhánh doanh nghiệp cũng dùng optional chaining và giá trị dự phòng:

```jsx
case 'business':
  return {
    title: item.place?.name || 'Doanh nghiệp địa phương',
    summary: item.place?.summary || item.place?.address,
  }
```

Cách này xử lý cả hai vấn đề:

- Không còn đánh giá các nhánh cấu hình không liên quan.
- Dữ liệu quan hệ tùy chọn không thể làm component crash.

## 4. Kết quả xác minh

Sau khi sửa:

- `npx vite build` hoàn tất thành công với 157 modules.
- Module do Vite ở cổng 5173 phục vụ đã chứa `switch (type)`.
- Không còn biểu thức không an toàn `item.place.name` trong `ContentCard`.
- API bài viết tiếp tục trả 12 bản ghi ở trang đầu.
- API sự kiện tiếp tục trả sự kiện sắp diễn ra.

Nếu trình duyệt vẫn giữ JavaScript cũ sau khi sửa, cần tải lại cưỡng bức bằng `Ctrl + F5` hoặc khởi động lại Vite.

## 5. Lưu ý về ảnh thử nghiệm

Dữ liệu seed sử dụng URL dạng `https://cdn.ltss.local/...`. Đây là domain giả phục vụ kiểm thử metadata, không phải CDN đang hoạt động. Vì vậy ảnh có thể không tải dù nội dung chữ đã hiển thị bình thường. Việc ảnh không tải độc lập với nguyên nhân làm trang trắng nêu trên.

## 6. Quy tắc phòng tránh

- Không truy cập trực tiếp quan hệ tùy chọn như `item.place.name`; sử dụng `item.place?.name` và giá trị dự phòng.
- Không tạo một object cấu hình lớn nếu các nhánh có biểu thức phụ thuộc vào các shape dữ liệu khác nhau.
- Chọn nhánh trước bằng `switch`, map tới hàm, hoặc component riêng rồi mới đọc trường dữ liệu.
- Frontend phải tuân thủ DTO công khai thay vì giả định quan hệ trong database luôn được API công bố.
- Khi một trang React bị trắng nhưng API vẫn trả 200, kiểm tra lỗi runtime trong console trước khi thay đổi backend hoặc database.
