# Eyewear Management Backend

Backend API cho hệ thống quản lý và kinh doanh kính mắt, được xây dựng bằng Spring Boot để phục vụ các nghiệp vụ bán hàng, quản lý sản phẩm, giỏ hàng, checkout, đơn hàng, thanh toán, vận chuyển, kho, đổi trả, dashboard và các tính năng AI hỗ trợ như chatbot gợi ý sản phẩm và OCR đơn kính thuốc.

Project này không chỉ là một REST API cơ bản, mà còn tích hợp nhiều dịch vụ bên thứ ba để xử lý các bài toán thực tế:

- Tính phí vận chuyển và thời gian giao dự kiến với GHN
- Thanh toán online với VNPAY Sandbox và PayOS VietQR
- Chatbot tư vấn sản phẩm bằng Gemini
- OCR ảnh đơn kính thuốc bằng OCR.Space
- Upload ảnh và tài nguyên try-on bằng Cloudinary
- Gửi email HTML thông báo đơn hàng bằng Spring Mail + Thymeleaf

## Tính năng chính

- Xác thực và phân quyền bằng JWT, Spring Security và role-based authorization
- Quản lý người dùng với các vai trò `CUSTOMER`, `ADMIN`, `MANAGER`, `SALES STAFF`, `OPERATIONS STAFF`
- Quản lý sản phẩm, thương hiệu, supplier, hình ảnh sản phẩm
- Quản lý giỏ hàng, xem trước checkout, áp dụng khuyến mãi, tính đặt cọc và số tiền còn lại
- Tạo đơn hàng với các loại đơn `DIRECT_ORDER`, `PRE_ORDER`, `PRESCRIPTION_ORDER`, `MIX_ORDER`
- Thanh toán online qua VNPAY và PayOS, bao gồm callback/webhook để đồng bộ trạng thái thanh toán
- Tính `shippingFee` và `expectedDeliveryAt` từ GHN
- Hỗ trợ khách hàng hủy đơn, đổi trả, hoàn tiền và upload bằng chứng/QR hoàn tiền
- Quản lý kho, phiếu nhập kho và tồn kho
- Dashboard thống kê và top sản phẩm bán chạy
- OCR ảnh đơn kính thuốc để trích xuất thông số mắt
- Chatbot gợi ý sản phẩm với chiến lược hybrid: Gemini + heuristic fallback
- Cấu hình file GLB cho tính năng virtual try-on của sản phẩm

## Công nghệ sử dụng

| Nhóm | Công nghệ |
| --- | --- |
| Ngôn ngữ & build tool | Java 25, Maven 3.9+ |
| Framework backend | Spring Boot 4.0.1 |
| REST API | Spring Web, Spring Validation |
| Data REST & Hypermedia | Spring Data REST, Spring HATEOAS |
| Bảo mật | Spring Security, OAuth2 Resource Server, JWT, Nimbus JOSE JWT, BCrypt |
| Persistence | Spring Data JPA, Hibernate, Microsoft SQL Server |
| Mapping | MapStruct, Lombok |
| API docs | springdoc-openapi, Swagger UI |
| Tối ưu hiệu năng | Spring Cache |
| Email | Spring Mail, Thymeleaf |
| Upload file/media | Cloudinary |
| Thanh toán | VNPAY Sandbox, PayOS VietQR |
| Vận chuyển | GHN API |
| AI / OCR | Gemini API, OCR.Space API |
| Containerization | Docker multi-stage build |
| Testing | JUnit 5, Mockito, Spring Test |

## Kiến trúc và tổ chức mã nguồn

Project đang đi theo cấu trúc phân lớp khá rõ ràng:

- `controller`: khai báo REST endpoints
- `service`: business logic
- `integration`: client/gateway cho GHN, VNPAY, Gemini
- `repository`: truy cập dữ liệu qua JPA
- `entity`: ánh xạ database
- `dto`: request/response/projection
- `mapper`: chuyển đổi entity <-> DTO bằng MapStruct
- `config`: security, OpenAPI, payment, GHN, OCR, Gemini, CORS, Cloudinary

```text
backend/eyewear-management-backend
├── docs
├── markdown_files
├── notes
├── sql_files
├── src
│   ├── main
│   │   ├── java/com/swp391/eyewear_management_backend
│   │   │   ├── config
│   │   │   ├── controller
│   │   │   ├── dto
│   │   │   ├── entity
│   │   │   ├── exception
│   │   │   ├── integration
│   │   │   ├── mapper
│   │   │   ├── repository
│   │   │   └── service
│   │   └── resources
│   └── test
├── Dockerfile
└── pom.xml
```

## Các module API chính

| Module | Base path | Mô tả |
| --- | --- | --- |
| Authentication | `/auth/**` | Login, introspect, refresh token, logout |
| Users | `/users/**` | Đăng ký, hồ sơ người dùng, quản trị user |
| Products | `/api/products/**` | Tìm kiếm, chi tiết, CRUD sản phẩm |
| Cart | `/api/cart/**` | Thêm, sửa, xóa giỏ hàng |
| Checkout | `/checkout/preview` | Tính preview đơn hàng, khuyến mãi, GHN fee, deposit |
| Orders | `/orders/**` | Tạo đơn, lịch sử đơn, hủy đơn |
| Staff Orders | `/api/staff/**`, `/api/operation-staff/orders/**` | Xử lý đơn cho sales staff và operations staff |
| Payments | `/payments/vnpay/**`, `/api/payment/**` | VNPAY callback/IPN, PayOS tạo link, webhook, status |
| Shipping | `/ghn/**` | Danh sách tỉnh/huyện/xã và hỗ trợ shipping |
| OCR | `/api/prescriptions/parse-image` | Parse ảnh đơn kính thuốc |
| Chatbot | `/api/chatbot/recommend` | Gợi ý sản phẩm bằng chatbot |
| Returns | `/api/return-exchanges/**`, `/api/returns/**` | Đổi trả, hoàn tiền, upload ảnh |
| Inventory | `/api/inventory/**`, `/api/inventory-receipts/**` | Tồn kho và phiếu nhập |
| Dashboard | `/api/v1/dashboard/**` | Thống kê và top sản phẩm |
| Try-on Config | `/api/products/{productId}/try-on-config` | Upload/cấu hình model GLB |
| Suppliers | `/api/suppliers/**` | Quản lý supplier và brand liên quan |

## Bảo mật và phân quyền

- Dùng JWT tự ký với thuật toán `HS512`
- Password được mã hóa bằng `BCrypt`
- Token có hỗ trợ introspect, refresh và invalidation khi logout
- Phân quyền theo authority dạng `ROLE_*`
- CORS được cấu hình qua `app.cors.allowed-origin-patterns`
- Swagger/OpenAPI được mở public để phục vụ tích hợp và test API

Các nhóm quyền hiện có trong hệ thống:

- `CUSTOMER`
- `ADMIN`
- `MANAGER`
- `SALES STAFF`
- `OPERATIONS STAFF`

## Tích hợp bên thứ ba

### 1. GHN API

Được dùng để:

- Lấy danh sách tỉnh, huyện, xã
- Tính `shippingFee`
- Tính `expectedDeliveryAt`
- Hỗ trợ các bước xử lý giao hàng trong nghiệp vụ đơn hàng

Project có dùng `Spring Cache` để cache dữ liệu location từ GHN:

- `ghnProvinces`
- `ghnDistricts`
- `ghnWards`

### 2. VNPAY Sandbox

Được dùng cho thanh toán online qua redirect flow.

- Tạo URL thanh toán VNPAY
- Xác minh chữ ký callback
- Xử lý `return` và `ipn`
- Đồng bộ trạng thái `Payment`, `Order`, `Invoice`

### 3. PayOS VietQR

Được dùng cho luồng thanh toán VietQR:

- Tạo payment link
- Nhận webhook xác thực từ PayOS
- Đồng bộ trạng thái thanh toán và trạng thái đơn hàng

### 4. Gemini API

Được dùng để phân tích ý định người dùng trong chatbot tư vấn sản phẩm.

Luồng chatbot hiện tại là hybrid:

- Ưu tiên phân tích intent bằng Gemini
- Nếu Gemini lỗi hoặc không khả dụng, fallback sang heuristic nội bộ để vẫn trả kết quả hữu ích

### 5. OCR.Space API

Được dùng để đọc ảnh đơn kính thuốc và chuyển thành dữ liệu cấu trúc:

- `SPH`
- `CYL`
- `AXIS`
- `ADD`
- `PD`

Response OCR có thêm:

- `confidence`
- `requiresReview`
- `warnings`
- `rawText`

### 6. Cloudinary

Được dùng để upload và lưu trữ:

- Hình ảnh sản phẩm
- Ảnh bằng chứng đổi trả
- Ảnh QR tài khoản hoàn tiền
- File model `.glb` cho virtual try-on

### 7. SMTP Email

Được dùng để gửi email HTML thông báo liên quan đến đơn hàng bằng template Thymeleaf.

## Yêu cầu hệ thống

- JDK 25
- Maven 3.9 trở lên
- Microsoft SQL Server
- Kết nối Internet để gọi các dịch vụ bên ngoài: GHN, VNPAY, PayOS, Gemini, OCR.Space, Cloudinary

## Cấu hình môi trường

Template cấu hình mẫu nằm tại:

```text
src/main/resources/application.example.yml
```

Khuyến nghị:

1. Copy file mẫu thành `src/main/resources/application.yml`
2. Điền giá trị thật cho môi trường local
3. Không commit file chứa secret thật lên Git

Các nhóm cấu hình quan trọng:

| Nhóm cấu hình | Key tiêu biểu | Mục đích |
| --- | --- | --- |
| Database | `spring.datasource.*` | Kết nối SQL Server |
| JWT | `jwt.*` | Ký và kiểm tra access token |
| GHN | `ghn.*` | Cấu hình phí ship, shop ID, token, package defaults |
| VNPAY | `vnpay.*` | Tích hợp thanh toán VNPAY Sandbox |
| PayOS | `payos.*` | Tích hợp PayOS VietQR |
| Gemini | `app.gemini.*` | Chatbot recommendation |
| OCR | `app.ocr.*` | Parse ảnh đơn kính thuốc |
| Cloudinary | `cloudinary.*` | Upload ảnh và model try-on |
| Mail | `spring.mail.*` | Gửi email HTML |
| Frontend/Backend URL | `app.frontend.*`, `app.backend.*` | Redirect callback thanh toán và liên kết frontend |
| CORS | `app.cors.allowed-origin-patterns` | Domain được phép gọi API |

Lưu ý vận hành:

- `spring.jpa.hibernate.ddl-auto` đang để `none`, nên database schema phải được tạo sẵn trước khi chạy app
- Time zone mặc định của ứng dụng là `Asia/Ho_Chi_Minh`
- Multipart upload hiện cấu hình tối đa `50MB/file` và `100MB/request`

## Chuẩn bị database

Thư mục script:

```text
sql_files/
```

Bạn có thể chọn một trong hai cách:

### Cách 1: Dùng script tổng

Chạy file:

```text
sql_files/swp391_script.sql
```

Script này đã bao gồm phần tạo database và schema tổng thể.

### Cách 2: Tách schema và demo data

Chạy lần lượt:

```text
sql_files/SWP391_Eyewear_DB_Structure.sql
sql_files/SWP391_Eyewear_DB_DemoData.sql
```

Database mục tiêu là:

```text
EyewearManagement
```

## Chạy project local

Di chuyển vào thư mục backend:

```bash
cd backend/eyewear-management-backend
```

Build project:

```bash
mvn clean install
```

Chạy ứng dụng:

```bash
mvn spring-boot:run
```

Ứng dụng mặc định chạy tại:

```text
http://localhost:8080
```

Lưu ý:

- Hiện repository có file `mvnw` và `mvnw.cmd`, nhưng thiếu thư mục `.mvn/wrapper`, nên cần dùng `mvn` cài trên máy thay vì Maven Wrapper

## Docker

Project có sẵn `Dockerfile` multi-stage để build và chạy bằng container.

Build image:

```bash
docker build -t eyewear-management-backend .
```

Run container:

```bash
docker run --rm -p 8080:8080 eyewear-management-backend
```

Lưu ý:

- Docker build hiện giả định file cấu hình runtime đã sẵn có trong project tại thời điểm build
- Với CI/CD, có thể inject `application.yml` từ secret theo tài liệu `docs/secret-management.md`

## API Documentation

Sau khi chạy ứng dụng, có thể truy cập:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Swagger đang scan các controller chính trong project và phù hợp để frontend hoặc QA kiểm thử nhanh các endpoint.

## Testing

Chạy test:

```bash
mvn test
```

Project hiện đã có test cho một số luồng quan trọng, ví dụ:

- Authentication
- Checkout preview
- Inventory
- Staff order
- VNPAY callback

## Tài liệu liên quan

- `docs/secret-management.md`: hướng dẫn quản lý secret và deploy
- `docs/prescription-ocr-flow.md`: mô tả chi tiết luồng OCR đơn kính thuốc
- `sql_files/`: schema và dữ liệu mẫu
- `markdown_files/`: tài liệu nghiệp vụ mở rộng
- `notes/`: ghi chú API, flow nghiệp vụ và rule xử lý

## Ghi chú bảo mật

- Không nên commit `application.yml` chứa thông tin thật của database, mail, payment gateway hoặc AI keys
- Nếu bất kỳ secret nào bị lộ, cần rotate ngay và cập nhật lại môi trường deploy
- Nên dùng biến môi trường hoặc secret manager cho production thay vì hard-code cấu hình

## Tóm tắt stack thực tế của backend này

Ngoài các công nghệ bạn đã liệt kê, backend hiện còn đang sử dụng thêm:

- SQL Server
- Spring Data JPA / Hibernate
- Spring Validation
- Spring Cache
- Spring Data REST / HATEOAS
- springdoc-openapi / Swagger UI
- MapStruct
- Lombok
- Cloudinary
- Spring Mail
- Thymeleaf
- Docker
- JUnit 5 + Mockito cho test
