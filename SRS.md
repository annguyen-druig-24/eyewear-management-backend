# SRS - Eyewear Management System (EMS)
## Software Requirements Specification

---

**Phiên bản:** 1.0  
**Ngày:** 25/03/2026  
**Dự án:** Eyewear Management - Return & Exchange Backend V2  
**Loại:** Backend API (RESTful)  
**Công nghệ:** Spring Boot 4.0.1, Java 25, SQL Server

---

## Mục lục

1. [Giới thiệu](#1-giới-thiệu)
2. [Mô tả Tổng quan](#2-mô-tả-tổng-quan)
3. [Yêu cầu Chức năng](#3-yêu-cầu-chức-năng)
4. [Yêu cầu Phi chức năng](#4-yêu-cầu-phi-chức-năng)
5. [Kiến trúc Hệ thống](#5-kiến-trúc-hệ-thống)
6. [Mô hình Dữ liệu](#6-mô-hình-dữ-liệu)
7. [API Endpoints](#7-api-endpoints)
8. [Bảo mật](#8-bảo-mật)
9. [Tích hợp Bên thứ ba](#9-tích-hợp-bên-thứ-ba)
10. [Ràng buộc](#10-ràng-buộc)
11. [Phụ lục](#11-phụ-lục)

---

## 1. Giới thiệu

### 1.1 Mục đích

Tài liệu này mô tả chi tiết các yêu cầu phần mềm cho hệ thống **Eyewear Management System (EMS)** - hệ thống quản lý cửa hàng kính mắt với đầy đủ chức năng thương mại điện tử, bao gồm quản lý sản phẩm, đơn hàng, thanh toán, vận chuyển và xử lý đổi trả.

### 1.2 Phạm vi

- **Backend API** cung cấp RESTful services
- **Quản lý sản phẩm** (kính gọng, kính thuốc, kính áp tròng)
- **Quản lý đơn hàng** và thanh toán trực tuyến
- **Xử lý đổi trả** và hoàn tiền
- **Quản lý tồn kho** và nhập hàng
- **Tích hợp OCR** đọc đơn thuốc
- **Tích hợp logistics** và thanh toán

### 1.3 Định nghĩa, từ viết tắt

| Thuật ngữ | Mô tả |
|-----------|-------|
| EMS | Eyewear Management System |
| SRS | Software Requirements Specification |
| REST | Representational State Transfer |
| API | Application Programming Interface |
| JWT | JSON Web Token |
| ORM | Object-Relational Mapping |
| CRUD | Create, Read, Update, Delete |
| VNPay | Cổng thanh toán Việt Nam |
| GHN | Giao Hàng Nhanh (đơn vị vận chuyển) |

---

## 2. Mô tả Tổng quan

### 2.1 Bối cảnh nghiệp vụ

Hệ thống EMS được xây dựng để hỗ trợ các cửa hàng kính mắt vận hành kinh doanh trực tuyến, bao gồm:

- Bán các sản phẩm kính mắt (gọng kính, tròng kính, kính áp tròng)
- Tiếp nhận đơn đặt hàng có đơn thuốc từ khách hàng
- Xử lý thanh toán trực tuyến qua nhiều cổng thanh toán
- Quản lý vận chuyển và theo dõi đơn hàng
- Hỗ trợ đổi trả và hoàn tiền

### 2.2 Người dùng hệ thống

| Vai trò | Mô tả | Quyền hạn |
|---------|-------|-----------|
| **ADMIN** | Quản trị viên | Toàn quyền hệ thống |
| **MANAGER** | Quản lý | Xem thống kê, quản lý nhân viên |
| **SALES_STAFF** | Nhân viên bán hàng | Xử lý đơn hàng, quản lý sản phẩm |
| **OPERATIONS_STAFF** | Nhân viên vận hành | Quản lý kho, xử lý vận chuyển |
| **CUSTOMER** | Khách hàng | Mua hàng, xem đơn hàng, đổi trả |

### 2.3 Luồng nghiệp vụ chính

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Khách hàng │────▶│   Đặt hàng   │────▶│   Thanh toán │────▶│   Giao hàng  │
│   Mua sắm    │     │   (Cart)     │     │   (VNPay/PayOS) │  │   (GHN)      │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
                           │                                         │
                           │                                         ▼
                     ┌──────────────┐                         ┌──────────────┐
                     │  Đơn thuốc   │                         │  Xác nhận    │
                     │  OCR (tùy chọn) │                      │  hoàn tất    │
                     └──────────────┘                         └──────────────┘
                                                                     │
                                                                     ▼
┌──────────────┐     ┌──────────────┐                         ┌──────────────┐
│  Hoàn tiền   │◀────│  Đổi trả      │                         │   Đánh giá   │
│  (nếu có)    │     │  Yêu cầu      │                         │   (tùy chọn) │
└──────────────┘     └──────────────┘                         └──────────────┘
```

---

## 3. Yêu cầu Chức năng

### 3.1 Quản lý Xác thực & Ủy quyền (Authentication & Authorization)

#### 3.1.1 Đăng nhập
- **Mô tả:** Xác thực người dùng và cấp JWT token
- **Đầu vào:** Email, mật khẩu
- **Đầu ra:** Access token, refresh token, thông tin người dùng
- **Quy tắc:**
  - Token có thời hạn 1 giờ (access token)
  - Refresh token có thời hạn 10 giờ
  - Mật khẩu được mã hóa BCrypt

#### 3.1.2 Token Management
- **Mô tả:** Quản lý vòng đời của JWT tokens
- **Chức năng:**
  - Introspect: Xác thực token còn hợp lệ
  - Refresh: Cấp lại access token từ refresh token
  - Logout: Vô hiệu hóa token

#### 3.1.3 Phân quyền
- **Mô tả:** Kiểm soát truy cập dựa trên vai trò
- **Các endpoint được bảo vệ:** Tất cả trừ `/auth/**`, `/swagger-ui/**`, `/api/products/**`

---

### 3.2 Quản lý Người dùng (User Management)

#### 3.2.1 CRUD Người dùng
- **Mô tả:** Tạo, đọc, cập nhật, xóa người dùng
- **Thuộc tính:**
  - ID, email, password, full name, phone, address
  - Ngày sinh, giới tính, avatar URL
  - Trạng thái (ACTIVE, INACTIVE, DELETED)
  - Vai trò (ROLE_ADMIN, ROLE_MANAGER, ROLE_SALES_STAFF, ROLE_OPERATIONS_STAFF, CUSTOMER)

#### 3.2.2 Quản lý Vai trò
- **Mô tả:** Gán và thu hồi vai trò cho người dùng
- **Hỗ trợ:** Nhiều vai trò trên mỗi người dùng

---

### 3.3 Quản lý Sản phẩm (Product Management)

#### 3.3.1 Sản phẩm (Product)
- **Mô tả:** Thực thể chính của sản phẩm
- **Thuộc tính:**
  - ID, tên sản phẩm, mã SKU, mô tả
  - Giá bán, giá gốc, phần trăm giảm giá
  - Số lượng tồn kho, số lượng đã bán
  - Thương hiệu (Brand), nhà cung cấp (Supplier)
  - Hình ảnh chính, album ảnh
  - Danh mục, trạng thái hoạt động
  - Thông số kỹ thuật (chiều dài cầu mũi, chiều dng temple)

#### 3.3.2 Gọng kính (Frame)
- **Mô tả:** Thuộc tính riêng của gọng kính
- **Thuộc tính:**
  - Frame ID (liên kết với Product)
  - Loại gọng (full-rim, semi-rim, rimless)
  - Chất liệu (metal, plastic, titanium, mixed)
  - Hình dạng (round, square, rectangle, aviator, cat-eye)
  - Giới tính mục tiêu (male, female, unisex)
  - Màu sắc, kích thước
  - Chiều dài cầu mũi, chiều dài temple

#### 3.3.3 Tròng kính (Lens)
- **Mô tả:** Thuộc tính riêng của tròng kính
- **Thuộc tính:**
  - Lens ID (liên kết với Product)
  - Loại tròng (single-vision, bifocal, progressive)
  - Chỉ số khúc xạ (1.5, 1.56, 1.59, 1.6, 1.67, 1.74)
  - Chất liệu (CR-39, Polycarbonate, Trivex, High-index)
  - Tính năng (anti-reflective, blue-light blocking, photochromic)
  - Đường kính

#### 3.3.4 Kính áp tròng (Contact Lens)
- **Mô tả:** Thuộc tính riêng của kính áp tròng
- **Thuộc tính:**
  - ContactLens ID
  - Loại (daily, weekly, monthly, yearly)
  - Đường cong đáy (BC)
  - Đường kính (DIA)
  - Công suất (sphere từ -20.00 đến +20.00)
  - Màu sắc (nếu có)

#### 3.3.5 Tìm kiếm & Lọc sản phẩm
- **Mô tả:** Tìm kiếm sản phẩm theo nhiều tiêu chí
- **Tiêu chí:**
  - Tên, mô tả (full-text search)
  - Khoảng giá (min-max price)
  - Thương hiệu, danh mục
  - Loại sản phẩm (frame, lens, contact lens)
  - Tình trạng (còn hàng, hết hàng)
  - Sắp xếp (giá, tên, bán chạy, mới nhất)

---

### 3.4 Quản lý Giỏ hàng (Shopping Cart)

#### 3.4.1 Thao tác Giỏ hàng
- **Chức năng:**
  - Thêm sản phẩm vào giỏ (với số lượng, thông số kính)
  - Cập nhật số lượng sản phẩm
  - Xóa sản phẩm khỏi giỏ
  - Xem danh sách sản phẩm trong giỏ
  - Xóa toàn bộ giỏ hàng

#### 3.4.2 Quy tắc nghiệp vụ
- Số lượng tối thiểu: 1
- Số lượng tối đa: 10 mỗi sản phẩm
- Kiểm tra tồn kho trước khi thêm
- Giỏ hàng được gắn với user đã đăng nhập

---

### 3.5 Quản lý Đơn hàng (Order Management)

#### 3.5.1 Tạo Đơn hàng
- **Mô tả:** Tạo đơn hàng từ giỏ hàng hoặc trực tiếp
- **Đầu vào:**
  - Danh sách sản phẩm (cart items hoặc product IDs)
  - Thông tin giao hàng (địa chỉ, số điện thoại, ghi chú)
  - Phương thức thanh toán
  - Mã khuyến mãi (nếu có)
  - Đơn thuốc (nếu có - cho đơn hàng kính thuốc)

#### 3.5.2 Trạng thái Đơn hàng
```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
    │          │           │           │
    └──────────┴───────────┴───────────┴──▶ CANCELLED
    │          │           │
    └──────────┴───────────┴───────────▶ RETURN_REQUESTED → RETURN_APPROVED/REJECTED
```

| Trạng thái | Mô tả |
|------------|-------|
| PENDING | Chờ xác nhận thanh toán |
| CONFIRMED | Đã xác nhận, đang chờ xử lý |
| PROCESSING | Đang chuẩn bị hàng |
| SHIPPED | Đã giao cho đơn vị vận chuyển |
| DELIVERED | Đã giao thành công |
| CANCELLED | Đã hủy |
| RETURN_REQUESTED | Yêu cầu đổi trả |
| RETURN_APPROVED | Đổi trả đã được chấp nhận |
| RETURN_REJECTED | Yêu cầu đổi trả bị từ chối |

#### 3.5.3 Chi tiết Đơn hàng (Order Detail)
- Liên kết với Product
- Số lượng, đơn giá tại thời điểm đặt
- Thông số kính (nếu là kính thuốc)
- Trạng thái riêng cho từng item

#### 3.5.4 Lịch sử Đơn hàng
- Xem danh sách đơn hàng của khách hàng
- Xem chi tiết đơn hàng
- Hủy đơn hàng (chỉ khi PENDING hoặc CONFIRMED)

---

### 3.6 Thanh toán (Payment)

#### 3.6.1 VNPay Integration
- **Mô tả:** Tích hợp cổng thanh toán VNPay
- **Chức năng:**
  - Tạo URL thanh toán VNPay
  - Xử lý IPN (Instant Payment Notification)
  - Xử lý Return URL (sau khi thanh toán)
  - Hoàn tiền qua VNPay

#### 3.6.2 PayOS Integration
- **Mô tả:** Tích hợp cổng thanh toán PayOS
- **Chức năng:**
  - Tạo link thanh toán PayOS
  - Xử lý webhook callback
  - Kiểm tra trạng thái thanh toán

#### 3.6.3 Quản lý Thanh toán
- **Bảng Payment:**
  - ID, Order ID, số tiền, phương thức
  - Trạng thái (PENDING, SUCCESS, FAILED, REFUNDED)
  - Mã giao dịch bên thứ ba
  - Thời gian thanh toán

---

### 3.7 Quản lý Đổi trả & Hoàn tiền (Return & Exchange Management)

#### 3.7.1 Yêu cầu Đổi trả
- **Mô tả:** Khách hàng yêu cầu đổi/trả sản phẩm
- **Đầu vào:**
  - Order ID
  - Danh sách sản phẩm muốn đổi/trả
  - Lý do đổi/trả
  - Hình ảnh minh chứng (tùy chọn)
  - Loại yêu cầu (RETURN hoặc EXCHANGE)

#### 3.7.2 Xử lý Yêu cầu
- **Quy trình:**
  1. Khách hàng tạo yêu cầu
  2. Nhân viên xem xét yêu cầu
  3. Phê duyệt hoặc từ chối
  4. Nếu phê duyệt: Hướng dẫn khách hàng gửi hàng
  5. Xác nhận đã nhận hàng
  6. Xử lý hoàn tiền (nếu RETURN)

#### 3.7.3 Hoàn tiền
- **Phương thức hoàn tiền:**
  - Tự động qua VNPay (nếu thanh toán ban đầu là VNPay)
  - Thủ công (chuyển khoản ngân hàng)
- **Quy tắc:**
  - Hoàn tiền trong vòng 7-14 ngày làm việc
  - Số tiền hoàn = Số tiền đã thanh toán cho sản phẩm

#### 3.7.4 ReturnExchange Entity
- ID, Order ID, Customer ID
- Loại (RETURN, EXCHANGE)
- Lý do, mô tả chi tiết
- Hình ảnh minh chứng
- Trạng thái (PENDING, APPROVED, REJECTED, RECEIVED, COMPLETED)
- Số tiền hoàn (nếu có)
- Ghi chú từ nhân viên

---

### 3.8 Quản lý Đơn thuốc (Prescription Management)

#### 3.8.1 OCR Đơn thuốc
- **Mô tả:** Nhận diện thông tin từ hình ảnh đơn thuốc
- **Tích hợp:** OCR.space API
- **Thông tin nhận diện:**
  - Thông số mắt phải (OD): Sphere, Cylinder, Axis, ADD, PD
  - Thông số mắt trái (OS): Sphere, Cylinder, Axis, ADD, PD
  - Ngày kê đơn
  - Bác sĩ kê đơn

#### 3.8.2 PrescriptionOrder
- **Mô tả:** Lưu trữ đơn thuốc kèm theo đơn hàng
- **Thuộc tính:**
  - ID, Order ID
  - Thông số kính (OD/OS cho mỗi thông số)
  - Loại đơn thuốc (single-vision, progressive, bifocal)
  - Ngày hết hạn
  - Hình ảnh đơn thuốc gốc
  - Trạng thái xác minh

#### 3.8.3 Xác minh Đơn thuốc
- Kiểm tra định dạng thông số
- Cảnh báo nếu đơn thuốc hết hạn (> 6 tháng)
- Yêu cầu xác minh lại nếu cần

---

### 3.9 Quản lý Tồn kho (Inventory Management)

#### 3.9.1 Theo dõi Tồn kho
- **Bảng Inventory:**
  - Product ID, số lượng tồn
  - Vị trí trong kho
  - Ngày cập nhật cuối

#### 3.9.2 Nhập hàng (Inventory Receipt)
- **Mô tả:** Ghi nhận hàng nhập về
- **Thuộc tính:**
  - ID, nhà cung cấp (Supplier)
  - Danh sách sản phẩm nhập
  - Số lượng, đơn giá
  - Ngày nhập, người tạo
  - Ghi chú

#### 3.9.3 Giao dịch Tồn kho (Inventory Transaction)
- **Mô tả:** Lịch sử thay đổi tồn kho
- **Các loại giao dịch:**
  - IMPORT: Nhập hàng mới
  - EXPORT: Xuất bán
  - ADJUSTMENT: Điều chỉnh (thêm/giảm thủ công)
  - RETURN: Trả lại kho (từ đổi trả)

#### 3.9.4 Cảnh báo Tồn kho
- Cảnh báo khi số lượng dưới ngưỡng tối thiểu
- Đề xuất nhập hàng khi cần

---

### 3.10 Vận chuyển (Shipping/Logistics)

#### 3.10.1 GHN Integration
- **Mô tả:** Tích hợp Giao Hàng Nhanh
- **Chức năng:**
  - Tra cứu tỉnh/thành phố, quận/huyện, phường/xã
  - Tính phí vận chuyển
  - Tạo đơn vận chuyển
  - Lấy token GHN

#### 3.10.2 Quản lý Vận chuyển
- Cập nhật trạng thái vận chuyển
- Theo dõi mã vận đơn
- Cập nhật thời gian giao dự kiến

#### 3.10.3 Thông tin Giao hàng
- **Bảng ShippingInfo:**
  - Order ID, người nhận
  - Số điện thoại, địa chỉ chi tiết
  - Tỉnh/thành, quận/huyện, phường/xã
  - Mã vận đơn, phí ship
  - Ghi chú giao hàng

---

### 3.11 Khuyến mãi (Promotion/Coupon)

#### 3.11.1 Mã Khuyến mãi
- **Thuộc tính:**
  - Mã code, tên chương trình
  - Loại giảm giá (PERCENTAGE, FIXED_AMOUNT)
  - Giá trị giảm
  - Điều kiện áp dụng (đơn hàng tối thiểu, sản phẩm cụ thể)
  - Số lượng giới hạn, số lần sử dụng tối đa
  - Ngày bắt đầu, ngày kết thúc
  - Trạng thái hoạt động

#### 3.11.2 Áp dụng Khuyến mãi
- Tính toán giảm giá khi checkout
- Kiểm tra điều kiện trước khi áp dụng
- Ghi nhận mã đã sử dụng

---

### 3.12 Thương hiệu & Nhà cung cấp (Brand & Supplier)

#### 3.12.1 Thương hiệu (Brand)
- ID, tên thương hiệu, mô tả
- Logo URL, quốc gia xuất xứ
- Trạng thái hoạt động

#### 3.12.2 Nhà cung cấp (Supplier)
- ID, tên, địa chỉ, số điện thoại
- Email, người liên hệ
- Trạng thái hợp tác

---

### 3.13 Hóa đơn (Invoice)

#### 3.13.1 Hóa đơn Bán hàng
- **Thuộc tính:**
  - ID, Order ID
  - Thông tin người bán
  - Thông tin người mua
  - Danh sách sản phẩm, số lượng, đơn giá
  - Thuế, tổng cộng
  - Ngày xuất

---

### 3.14 Dashboard & Thống kê

#### 3.14.1 Thống kê Tổng quan
- Tổng doanh thu (theo ngày, tuần, tháng)
- Số đơn hàng
- Số khách hàng mới
- Tỷ lệ đổi trả

#### 3.14.2 Báo cáo Sản phẩm
- Sản phẩm bán chạy
- Sản phẩm tồn kho thấp
- Sản phẩm theo danh mục

---

## 4. Yêu cầu Phi chức năng

### 4.1 Hiệu năng (Performance)

| Chỉ số | Yêu cầu |
|--------|---------|
| Thời gian phản hồi trung bình | < 500ms |
| Thời gian phản hồi tối đa | < 2 giây |
| Số request đồng thời | > 100 concurrent users |
| Uptime | 99.5% |

### 4.2 Bảo mật (Security)

| Yêu cầu | Mô tả |
|---------|-------|
| Mã hóa mật khẩu | BCrypt (strength 10) |
| JWT Algorithm | HS512 |
| HTTPS | Bắt buộc cho production |
| CORS | Cấu hình whitelist domains |
| Input Validation | Tất cả API inputs |
| SQL Injection | Phòng ngừa qua JPA |
| Rate Limiting | Giới hạn request/giờ |

### 4.3 Khả năng mở rộng (Scalability)

- Hỗ trợ horizontal scaling
- Stateless application (JWT)
- Caching layer cho dữ liệu tĩnh
- Database connection pooling

### 4.4 Khả năng bảo trì (Maintainability)

- Clean Architecture (Controller → Service → Repository)
- MapStruct cho object mapping
- Exception handling tập trung
- API versioning (/api/v1, /api/v2)
- OpenAPI/Swagger documentation

### 4.5 Tính khả dụng (Availability)

- Health check endpoint
- Graceful error handling
- Retry mechanism cho external services

---

## 5. Kiến trúc Hệ thống

### 5.1 Kiến trúc Layer

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
│  │Controller│  │   DTO    │  │   Exception Handler  │  │
│  └──────────┘  └──────────┘  └──────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                      SERVICE LAYER                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
│  │ Service  │  │   MapStruct│ │    Validation        │  │
│  └──────────┘  └──────────┘  └──────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                   INTEGRATION LAYER                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
│  │  VNPay   │  │  PayOS   │  │   GHN / Cloudinary   │  │
│  └──────────┘  └──────────┘  └──────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                   PERSISTENCE LAYER                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
│  │  Entity  │  │  Repo    │  │   Spring Data JPA   │  │
│  └──────────┘  └──────────┘  └──────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 5.2 Package Structure

```
com.swp391.eyewear_management_backend/
├── config/                     # Configuration classes
│   ├── ApplicationConfig.java
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   └── OpenApiConfig.java
├── controller/                 # REST Controllers
│   ├── AuthController.java
│   ├── UserController.java
│   ├── ProductController.java
│   ├── CartController.java
│   ├── OrderController.java
│   ├── PaymentController.java
│   ├── ReturnExchangeController.java
│   ├── PrescriptionController.java
│   ├── InventoryController.java
│   ├── DashboardController.java
│   └── GHNController.java
├── dto/
│   ├── request/                # Request DTOs
│   └── response/               # Response DTOs
├── entity/                     # JPA Entities
├── exception/                  # Custom exceptions
│   ├── GlobalExceptionHandler.java
│   └── custom exceptions...
├── integration/                 # External integrations
│   ├── vnpay/
│   ├── payos/
│   ├── ghn/
│   ├── cloudinary/
│   └── ocr/
├── mapper/                     # MapStruct mappers
├── repository/                  # Spring Data repositories
├── security/                    # Security components
├── service/                     # Service interfaces
│   └── impl/                   # Service implementations
└── util/                       # Utility classes
```

### 5.3 Database Schema Overview

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    User     │────▶│    Role     │     │   Product   │
└─────────────┘     └─────────────┘     └─────────────┘
      │                                        │
      │                                        │
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    Cart     │────▶│  CartItem   │────▶│OrderDetail  │
└─────────────┘     └─────────────┘     └─────────────┘
                                            │
┌─────────────┐     ┌─────────────┐          │
│ReturnExchange│◀───│ReturnExchangeItem│      │
└─────────────┘     └─────────────┘          ▼
                                           ┌─────────────┐
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Inventory  │────▶│InventoryReceipt│   │    Order    │
└─────────────┘     └─────────────┘     └─────────────┘
                           │                   │
                           ▼                   ▼
                    ┌─────────────┐     ┌─────────────┐
                    │InventoryTransaction│  │   Payment   │
                    └─────────────┘     └─────────────┘
```

---

## 6. Mô hình Dữ liệu

### 6.1 Entity Relationship Diagram

```
User (1) ────────── (N) Cart
                        │
                        │ (1:N)
                        ▼
                   CartItem (N) ── (1) Product
                                          │
          ┌───────────────┬────────────────┼───────────────┐
          │               │                │               │
          ▼               ▼                ▼               ▼
       Frame           Lens         ContactLens        Brand
                                                               │
                                                               ▼
┌──────────┐   ┌──────────┐   ┌──────────┐              ┌──────────┐
│PrescriptionOrder│ │Inventory│   │Supplier  │              │Promotion│
└──────────┘   └──────────┘   └──────────┘              └──────────┘
      │              │              │
      │              │              │
      ▼              ▼              ▼
┌──────────┐   ┌──────────┐
│InventoryReceipt│ │InventoryTransaction│
└──────────┘   └──────────┘
```

### 6.2 Bảng chính và mối quan hệ

| Bảng | Khóa chính | Khóa ngoại | Mối quan hệ |
|------|-----------|-----------|-------------|
| `users` | id | role_id | 1:N với roles |
| `roles` | id | - | - |
| `products` | id | brand_id, supplier_id, category_id | 1:N với nhiều bảng |
| `frames` | product_id | - | 1:1 với products |
| `lenses` | product_id | - | 1:1 với products |
| `contact_lenses` | product_id | - | 1:1 với products |
| `carts` | id | user_id | 1:1 với users |
| `cart_items` | id | cart_id, product_id | N:1 với carts, products |
| `orders` | id | user_id, shipping_info_id | 1:N với order_details, payments |
| `order_details` | id | order_id, product_id | N:1 với orders, products |
| `payments` | id | order_id | N:1 với orders |
| `return_exchanges` | id | order_id, user_id | 1:N với return_exchange_items |
| `return_exchange_items` | id | return_exchange_id, order_detail_id | N:1 với return_exchanges, order_details |
| `prescription_orders` | id | order_id | 1:1 với orders |
| `inventory` | product_id | - | 1:1 với products |
| `inventory_receipts` | id | supplier_id, user_id | 1:N với inventory_transactions |
| `inventory_transactions` | id | product_id, receipt_id | N:1 với products, receipts |
| `shipping_info` | id | - | 1:N với orders |
| `promotions` | id | - | - |
| `invoices` | id | order_id | 1:1 với orders |
| `brands` | id | - | 1:N với products |
| `suppliers` | id | - | 1:N với products |

---

## 7. API Endpoints

### 7.1 Authentication APIs (`/auth`)

| Method | Endpoint | Mô tả | Auth |
|-------|----------|-------|------|
| POST | `/auth/token` | Đăng nhập | Không |
| POST | `/auth/introspect` | Xác thực token | Bearer |
| POST | `/auth/refresh` | Làm mới token | Không |
| POST | `/auth/logout` | Đăng xuất | Bearer |

### 7.2 User APIs (`/users`)

| Method | Endpoint | Mô tả | Auth |
|-------|----------|-------|------|
| GET | `/users` | Danh sách người dùng | ADMIN |
| GET | `/users/{id}` | Chi tiết người dùng | ADMIN |
| PUT | `/users/{id}` | Cập nhật người dùng | ADMIN/Self |
| DELETE | `/users/{id}` | Xóa người dùng | ADMIN |
| PUT | `/users/{id}/roles` | Cập nhật vai trò | ADMIN |

### 7.3 Product APIs (`/api/products`)

| Method | Endpoint | Mô tả | Auth |
|-------|----------|-------|------|
| GET | `/api/products` | Danh sách sản phẩm (public) | Không |
| GET | `/api/products/{id}` | Chi tiết sản phẩm | Không |
| GET | `/api/products/search` | Tìm kiếm sản phẩm | Không |
| POST | `/api/products` | Tạo sản phẩm | SALES_STAFF |
| PUT | `/api/products/{id}` | Cập nhật sản phẩm | SALES_STAFF |
| DELETE | `/api/products/{id}` | Xóa sản phẩm | SALES_STAFF |

### 7.4 Cart APIs (`/cart`)

| Method | Endpoint | Mô tả | Auth |
|-------|----------|-------|------|
| GET | `/cart` | Xem giỏ hàng | Bearer |
| POST | `/cart/items` | Thêm vào giỏ | Bearer |
| PUT | `/cart/items/{id}` | Cập nhật số lượng | Bearer |
| DELETE | `/cart/items/{id}` | Xóa khỏi giỏ | Bearer |
| DELETE | `/cart` | Xóa giỏ hàng | Bearer |

### 7.5 Order APIs (`/orders`)

| Method | Endpoint | Mô tả | Auth |
|-------|----------|-------|------|
| POST | `/orders` | Tạo đơn hàng | Bearer |
| GET | `/orders` | Danh sách đơn hàng | Bearer/Staff |
| GET | `/orders/{id}` | Chi tiết đơn hàng | Bearer |
| PUT | `/orders/{id}/status` | Cập nhật trạng thái | STAFF |
| POST | `/orders/{id}/cancel` | Hủy đơn hàng | Bearer |

### 7.6 Payment APIs

| Method | Endpoint | Mô tả | Auth |
|-------|----------|-------|------|
| POST | `/api/payment/vnpay/create` | Tạo thanh toán VNPay | Bearer |
| GET | `/api/payment/vnpay/return` | VNPay Return URL | Không |
| POST | `/api/payment/vnpay/ipn` | VNPay IPN | Không |
| POST | `/api/payment/payos/create` | Tạo thanh toán PayOS | Bearer |
| POST | `/api/payment/payos/webhook` | PayOS Webhook | Không |

### 7.7 Return & Exchange APIs (`/api/return-exchanges`)

| Method | Endpoint | Mô tả | Auth |
|-------|----------|-------|------|
| POST | `/api/return-exchanges` | Tạo yêu cầu đổi trả | Bearer |
| GET | `/api/return-exchanges` | Danh sách yêu cầu | STAFF |
| GET | `/api/return-exchanges/{id}` | Chi tiết yêu cầu | Bearer |
| PUT | `/api/return-exchanges/{id}/approve` | Phê duyệt | STAFF |
| PUT | `/api/return-exchanges/{id}/reject` | Từ chối | STAFF |
| PUT | `/api/return-exchanges/{id}/receive` | Xác nhận đã nhận hàng | STAFF |
| POST | `/api/return-exchanges/{id}/refund` | Hoàn tiền thủ công | STAFF |

### 7.8 Prescription APIs (`/api/prescriptions`)

| Method | Endpoint | Mô tả | Auth |
|-------|----------|-------|------|
| POST | `/api/prescriptions/parse-image` | OCR đơn thuốc | Bearer |
| POST | `/api/prescriptions` | Tạo đơn thuốc | Bearer |
| GET | `/api/prescriptions/{id}` | Chi tiết đơn thuốc | Bearer |

### 7.9 Inventory APIs (`/api/inventory`)

| Method | Endpoint | Mô tả | Auth |
|-------|----------|-------|------|
| GET | `/api/inventory` | Danh sách tồn kho | STAFF |
| GET | `/api/inventory/{productId}` | Tồn kho sản phẩm | STAFF |
| POST | `/api/inventory-receipts` | Tạo phiếu nhập | STAFF |
| GET | `/api/inventory-receipts` | Danh sách phiếu nhập | STAFF |
| GET | `/api/inventory/transactions` | Lịch sử giao dịch | STAFF |

### 7.10 Checkout APIs (`/checkout`)

| Method | Endpoint | Mô tả | Auth |
|-------|----------|-------|------|
| GET | `/checkout/preview` | Xem trước thanh toán | Bearer |
| POST | `/checkout` | Thanh toán | Bearer |

### 7.11 Logistics APIs (`/ghn`)

| Method | Endpoint | Mô tả | Auth |
|-------|----------|-------|------|
| GET | `/ghn/provinces` | Danh sách tỉnh/thành | Không |
| GET | `/ghn/districts/{provinceId}` | Danh sách quận/huyện | Không |
| GET | `/ghn/wards/{districtId}` | Danh sách phường/xã | Không |
| POST | `/ghn/calculate-fee` | Tính phí vận chuyển | Không |
| POST | `/ghn/create-order` | Tạo đơn vận chuyển | STAFF |

### 7.12 Dashboard APIs (`/api/v1/dashboard`)

| Method | Endpoint | Mô tả | Auth |
|-------|----------|-------|------|
| GET | `/api/v1/dashboard/stats` | Thống kê tổng quan | STAFF |
| GET | `/api/v1/dashboard/top-products` | Sản phẩm bán chạy | STAFF |

### 7.13 Image Upload API

| Method | Endpoint | Mô tả | Auth |
|-------|----------|-------|------|
| POST | `/api/upload` | Upload hình ảnh | Bearer |

---

## 8. Bảo mật

### 8.1 Authentication Flow

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  Client  │────▶│  Server  │────▶│ Validate │────▶│ Response │
│  Request │     │   API    │     │  Credentials│   │ with JWT │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
     │                                        │
     │                                        ▼
     │                                 ┌──────────┐
     │                                 │  Check   │
     │                                 │  Roles   │
     │                                 └──────────┘
```

### 8.2 JWT Token Structure

**Access Token Claims:**
```json
{
  "sub": "user_id",
  "email": "user@example.com",
  "roles": ["ROLE_CUSTOMER"],
  "iat": 1679500000,
  "exp": 1679503600
}
```

### 8.3 Security Rules

1. **Rate Limiting:** 100 requests/giờ cho mỗi IP
2. **Input Validation:** All inputs must be validated
3. **CORS:** Whitelist allowed origins
4. **HTTPS:** Required for production
5. **JWT Expiration:** 1 hour (access token)

### 8.4 Role-Based Access Control

| Endpoint Pattern | Allowed Roles |
|-----------------|---------------|
| `GET /api/products/**` | All (public) |
| `POST /auth/**` | All (public) |
| `/admin/**` | ADMIN only |
| `/orders/**` (POST) | Authenticated users |
| `/orders/**` (PUT/DELETE) | STAFF, ADMIN |
| `/return-exchanges/**` | Authenticated users (own), STAFF (all) |
| `/inventory/**` | STAFF, ADMIN |
| `/dashboard/**` | STAFF, ADMIN |

---

## 9. Tích hợp Bên thứ ba

### 9.1 VNPay

| Thành phần | Mô tả |
|-----------|-------|
| Base URL | `https://sandbox.vnpayment.vn` (test) / `https://vnpayment.vn` (prod) |
| API | Tạo thanh toán, IPN, Hoàn tiền |
| Mã hóa | HMAC SHA512 |

### 9.2 PayOS

| Thành phần | Mô tả |
|-----------|-------|
| Base URL | `https://api.payos.vn` |
| API | Tạo payment link, Webhook |
| Xác thực | API Key + Checksum |

### 9.3 GHN (Giao Hàng Nhanh)

| Thành phần | Mô tả |
|-----------|-------|
| Base URL | `https://dev-online-gateway.ghn.vn` |
| API | Tra cứu địa chỉ, Tính phí, Tạo đơn |
| Xác thực | Token (ShopID) |

### 9.4 Cloudinary

| Thành phần | Mô tả |
|-----------|-------|
| Purpose | Lưu trữ hình ảnh sản phẩm, đơn thuốc |
| Features | Upload, Resize, CDN |

### 9.5 OCR.space

| Thành phần | Mô tả |
|-----------|-------|
| Purpose | Nhận diện text từ hình ảnh đơn thuốc |
| API | Parse image endpoint |
| Response | JSON với text được trích xuất |

---

## 10. Ràng buộc

### 10.1 Ràng buộc Kỹ thuật

- **Ngôn ngữ:** Java 25
- **Framework:** Spring Boot 4.0.1
- **Database:** SQL Server
- **Build Tool:** Maven
- **Java EE Version:** Java 21+

### 10.2 Ràng buộc Nghiệp vụ

- Thời gian xử lý đơn hàng: 1-3 ngày làm việc
- Thời gian hoàn tiền: 7-14 ngày làm việc
- Đơn thuốc có hiệu lực: 6 tháng
- Số lượng đặt tối thiểu: 1 sản phẩm
- Số lượng đặt tối đa mỗi sản phẩm: 10

### 10.3 Ràng buộc Triển khai

- Môi trường Production: HTTPS bắt buộc
- CORS: Chỉ whitelist domains được phép
- Logging: Ghi log tất cả transactions

---

## 11. Phụ lục

### 11.1 HTTP Status Codes

| Code | Mô tả | Usage |
|------|-------|-------|
| 200 | OK | Thành công, có data |
| 201 | Created | Tạo mới thành công |
| 204 | No Content | Xóa thành công |
| 400 | Bad Request | Dữ liệu không hợp lệ |
| 401 | Unauthorized | Chưa đăng nhập |
| 403 | Forbidden | Không có quyền |
| 404 | Not Found | Không tìm thấy |
| 409 | Conflict | Xung đột dữ liệu |
| 500 | Internal Server Error | Lỗi server |

### 11.2 Error Response Format

```json
{
  "success": false,
  "message": "Error description",
  "error": {
    "code": "ERROR_CODE",
    "details": "Additional details"
  }
}
```

### 11.3 Success Response Format

```json
{
  "success": true,
  "message": "Success message",
  "data": { ... }
}
```

### 11.4 Công nghệ sử dụng

| Công nghệ | Phiên bản | Mục đích |
|-----------|----------|----------|
| Java | 25 | Ngôn ngữ lập trình |
| Spring Boot | 4.0.1 | Framework |
| Spring Security | 6.x | Bảo mật |
| Spring Data JPA | 3.x | ORM |
| Hibernate | 6.x | JPA Implementation |
| MapStruct | 1.6.3 | Object Mapping |
| Lombok | - | Giảm boilerplate |
| JWT (jjwt) | 0.12.x | JSON Web Token |
| SpringDoc OpenAPI | 3.0.1 | API Documentation |
| SQL Server | - | Database |
| Maven | - | Build Tool |

### 11.5 Glossary

| Thuật ngữ | Định nghĩa |
|-----------|-----------|
| SPH (Sphere) | Độ cận/viễn của mắt |
| CYL (Cylinder) | Độ loạn thị |
| Axis | Trục loạn thị (0-180) |
| ADD | Độ bổ sung (cho đa tròng) |
| PD (Pupillary Distance) | Khoảng cách đồng tử |
| OD | Oculus Dexter (Mắt phải) |
| OS | Oculus Sinister (Mắt trái) |
| BC (Base Curve) | Độ cong đáy kính áp tròng |
| DIA (Diameter) | Đường kính kính áp tròng |

---

**Document Information:**
- Author: SWP391 Development Team
- Version: 1.0
- Last Updated: 25/03/2026
- Status: Draft

---

*Document generated based on codebase analysis of Eyewear Management Backend V2*
