# Flow hủy đơn trước khi SALES STAFF xác nhận và hoàn tiền thủ công

## 1. Mục đích tài liệu

Tài liệu này mô tả chi tiết flow cho trường hợp:

- CUSTOMER tự bấm **Hủy đơn hàng**.
- Đơn hàng vẫn đang ở giai đoạn **trước khi SALES STAFF xác nhận**.
- Đơn có thể đã thanh toán online một phần hoặc toàn phần.
- Hệ thống hiện tại **chỉ áp dụng hoàn tiền thủ công**: SALES STAFF chuyển khoản/hoàn thủ công dựa trên `refund_method` và `refund_account_number` do CUSTOMER cung cấp, sau đó upload ảnh biên nhận để lưu evidence.

---

## 2. Ghi chú chỉnh sửa so với file gốc

### 2.1. Phần được giữ lại

- Giữ nguyên tinh thần nghiệp vụ: **hủy đơn và hoàn tiền là hai bước khác nhau**.
- Giữ nguyên nguyên tắc: chỉ cho CUSTOMER tự hủy ở nhóm trạng thái an toàn như `PENDING`, `PARTIALLY_PAID`, `PAID`.
- Giữ nguyên nguyên tắc: tiền hoàn phải tính theo **số tiền thực thu thành công**, không tính theo tổng đơn lý thuyết.
- Giữ nguyên yêu cầu: phải có **audit** và **chống bấm lặp / chống tạo refund trùng**.

### 2.2. Phần được chỉnh sửa / bổ sung

> [CHỈNH SỬA] Bỏ hướng ưu tiên auto-refund qua gateway trong phạm vi triển khai hiện tại.  
> [BỔ SUNG MỚI] Chốt rõ phạm vi hiện tại: **manual refund only**.  
> [CHỈNH SỬA] Không khuyến nghị cập nhật `Invoice.Status = UNPAID` một cách máy móc nếu tiền đã thu thành công; cần giữ sự thật dữ liệu về payment/refund. Nếu CUSTOMER chọn hủy đơn hàng thì sẽ cập nhật Invoice.Status = CANCELED.
> [BỔ SUNG MỚI] Bổ sung chi tiết bước CUSTOMER nhập thông tin nhận hoàn tiền.  
> [BỔ SUNG MỚI] Bổ sung mapping dữ liệu với `Return_Exchange`.  
> [BỔ SUNG MỚI] Bổ sung rule về hoàn kho/tạo inventory reversal nếu hệ thống đã trừ kho khi tạo đơn.  
> [BỔ SUNG MỚI] Bổ sung ví dụ thực tế, validation, API gợi ý và edge cases.

---

## 3. Phạm vi áp dụng

Flow này chỉ áp dụng khi thỏa mãn đồng thời các điều kiện sau:

1. CUSTOMER là đúng chủ đơn hàng.
2. `Order_Status` thuộc danh sách cho phép CUSTOMER tự hủy:
   - `PENDING`
   - `PARTIALLY_PAID`
   - `PAID`
3. Đơn **chưa** được SALES STAFF xác nhận (`CONFIRMED`) và chưa đi vào các bước vận hành sâu hơn.
4. Đơn chưa có yêu cầu hủy / hoàn tiền đang mở cho cùng order.
5. Nếu đơn có phát sinh payment thành công thì việc hoàn tiền sẽ được xử lý theo **luồng refund thủ công**.

### 3.1. Không áp dụng cho các trạng thái sau

- `CONFIRMED`
- `PROCESSING`
- `READY`
- `COMPLETED`
- `CANCELED`

> [BỔ SUNG MỚI] Thay vì chỉ check `Order_Status != CONFIRMED`, tài liệu này chốt lại bằng **whitelist trạng thái được hủy** để tránh lỗi nghiệp vụ.

---

## 4. Mục tiêu nghiệp vụ

1. CUSTOMER có thể hủy đơn nhanh khi đơn chưa được staff xử lý.
2. Đơn được khóa ngay để không tiếp tục đi vào pipeline xử lý.
3. Nếu hệ thống đã thu tiền thì phải sinh yêu cầu hoàn tiền có thể theo dõi được.
4. SALES STAFF hoàn tiền thủ công, upload bằng chứng chuyển khoản và lưu audit đầy đủ.
5. Hệ thống tránh được:
   - hoàn tiền thiếu / thừa,
   - tạo trùng request,
   - mất dấu vết người thao tác,
   - sai lệch giữa trạng thái order và trạng thái hoàn tiền.

---

## 5. Nguyên tắc nghiệp vụ cốt lõi

### 5.1. Hủy đơn và hoàn tiền là hai nghiệp vụ khác nhau

- **Hủy đơn** dùng để dừng vòng đời đơn hàng.
- **Hoàn tiền** dùng để xử lý phần tiền mà hệ thống đã thu của CUSTOMER.

Điều này có nghĩa là:

- Có thể có đơn `CANCELED` nhưng chưa refund xong.
- Có thể có đơn `CANCELED` và không cần refund vì chưa thu đồng nào.
- Không được xem `Order_Status = CANCELED` là bằng với `đã hoàn tiền`.

### 5.2. Chỉ hoàn theo số tiền thực thu thành công

Công thức đề xuất:

```text
refundAmount = tổng Payment.Amount có Status = SUCCESS
             - tổng số tiền đã refund thành công trước đó
```

Không tính vào refund:

- payment đang `PENDING`
- payment `FAILED`
- payment bị hủy
- phần tiền chưa từng thu thành công

### 5.3. Mọi thao tác phải idempotent

- Một order chỉ có tối đa **một request REFUND đang mở** cho flow hủy trước xác nhận.
- CUSTOMER bấm nút nhiều lần không được tạo nhiều request.
- Callback / retry phía backend không được sinh thêm refund trùng.

### 5.4. Giữ sự thật dữ liệu kế toán

> [CHỈNH SỬA] Tài liệu này không khuyến nghị đổi trạng thái invoice/payment một cách đơn giản thành `UNPAID` nếu hệ thống đã thực tế thu tiền.
--> Sẽ cập nhật `Invoice.Status = CANCELED`

Nguyên tắc an toàn hơn:

- `Order` có thể chuyển sang `CANCELED`.
- `Payment` vẫn giữ đúng bản chất đã thu (`SUCCESS`) cho đến khi xử lý refund.
- Dấu vết refund được lưu riêng ở `Return_Exchange` và/hoặc lịch sử payment/refund.

---

## 6. Thiết kế trạng thái đề xuất

### 6.1. Trạng thái đơn hàng

- `PENDING`: mới tạo, chưa staff xác nhận.
- `PARTIALLY_PAID`: đã thu cọc / đã thu một phần.
- `PAID`: đã thu đủ.
- `CANCELED`: đơn đã bị hủy, không đi tiếp trong pipeline bán hàng.

### 6.2. Trạng thái yêu cầu refund trong `Return_Exchange`

Trong phạm vi triển khai hiện tại, dùng bộ trạng thái đơn giản:

- `PENDING`: request refund mới được tạo, chờ staff xử lý.
- `APPROVED`: staff đã kiểm tra và đồng ý refund.
- `REJECTED`: staff từ chối refund và có lý do.
- `COMPLETED`: staff đã hoàn tiền thủ công xong và đã upload evidence.

> [GIỮ LẠI + ĐIỀU CHỈNH] Giữ bộ trạng thái đơn giản để phù hợp giai đoạn hiện tại, nhưng diễn giải lại rõ vai trò từng trạng thái.

---

## 7. Flow chi tiết end-to-end

## Bước 1: CUSTOMER bấm “Hủy đơn”

### 7.1.1. Hệ thống kiểm tra điều kiện

Hệ thống phải validate:

1. `order.user_id == currentCustomerId`
2. `order.status IN (PENDING, PARTIALLY_PAID, PAID)`
3. Không có request refund mở cho cùng order:
   - `Return_Type = REFUND`
   - `Request_Scope = ORDER`
   - `Status IN (PENDING, APPROVED)`
4. Đơn chưa bị hủy trước đó.
5. Nếu hệ thống có logic trừ kho ngay khi tạo đơn thì chuẩn bị flow hoàn kho. --> Sau khi Return_Exchange.Status = COMPLETED thì sẽ cộng lại số lượng sản phẩm CUSTOMER hủy vào Product.Available_Quantity.

### 7.1.2. Dữ liệu CUSTOMER cần nhập

Nếu order có khả năng cần refund, giao diện nên yêu cầu CUSTOMER nhập trước:

- `refund_method`
  - Ví dụ: `BANK_TRANSFER`, `EWALLET`, `OTHER_MANUAL`
- `refund_account_number`
- `refund_account_name` (khuyến nghị thêm để staff chuyển đúng người)
- Ghi chú thêm của khách (nếu có)

> [BỔ SUNG MỚI] Phần này chưa được mô tả rõ trong file gốc, nhưng cần có để staff hoàn tiền thủ công đúng và đủ thông tin.

### 7.1.3. Hệ thống cập nhật đơn

Nếu hợp lệ:

- Cập nhật `Order.Order_Status = CANCELED`
- Ghi nhận lý do hủy / note hủy
- Ghi log vào `Order_Processing` hoặc bảng audit:
  - action: `CUSTOMER_CANCEL_BEFORE_CONFIRM`
  - actor: CUSTOMER
  - timestamp
  - note

### 7.1.4. Hoàn kho (nếu có)

> [BỔ SUNG MỚI - QUAN TRỌNG] Nếu hệ thống của bạn đã trừ kho ngay khi tạo order, thì khi order chuyển sang `CANCELED`, cần tạo nghiệp vụ **hoàn kho / inventory reversal**.

Khuyến nghị:

- Không update cứng số lượng tồn kho bằng tay.
- Tạo `InventoryTransaction` loại `CANCEL_RESTOCK` hoặc tương đương.
- Số lượng hoàn kho bằng đúng số lượng từng product đã bị trừ cho order đó.

---

## Bước 2: Xác định có cần tạo refund request hay không

Sau khi hủy đơn, hệ thống tính:

```text
totalPaidSuccess = SUM(Payment.Amount WHERE Order_ID = ? AND Status = SUCCESS)
totalRefunded    = tổng số tiền đã refund thành công trước đó cho order
refundAmount     = totalPaidSuccess - totalRefunded
```

### 7.2.1. Nếu `refundAmount <= 0`

- Không tạo `Return_Exchange`
- Kết thúc flow hủy đơn
- Thông báo cho CUSTOMER: đơn đã hủy thành công, không phát sinh hoàn tiền

### 7.2.2. Nếu `refundAmount > 0`

Tạo `Return_Exchange`:

- `Order_ID = orderId`
- `User_ID = customerId`
- `Return_Type = REFUND`
- `Request_Scope = ORDER`
- `Status = PENDING`
- `Refund_Amount = refundAmount`
- `Refund_Method = CUSTOMER input`
- `Refund_Account_Number = CUSTOMER input`
- `Refund_Account_Name = CUSTOMER input`
- `Customer_Account_QR = CUSTOMER input`
- `Return_Reason = CUSTOMER cancel reason`
- `Request_Note = Hủy đơn trước khi staff xác nhận`

> [CHỈNH SỬA] Ở flow này, refund được lưu ở mức **ORDER**, không phụ thuộc `Order_Detail`.

---

## Bước 3: SALES STAFF tiếp nhận request refund

SALES STAFF mở danh sách “Đơn đã hủy chờ hoàn tiền” và kiểm tra:

1. `Order_Status = CANCELED`
2. `Return_Exchange.Status = PENDING`
3. Số tiền refund đề xuất có khớp dữ liệu payment hay không
4. Thông tin hoàn tiền của CUSTOMER có đủ hay không:
   - refund method
   - account number
   - account name (có thể có hoặc không, không bắt buộc)
   - account_QR (có thể có hoặc không, không bắt buộc)

### 7.3.1. Các lý do có thể từ chối

Chỉ được reject khi có lý do nghiệp vụ rõ ràng, ví dụ:

- Request tạo trùng
- Dữ liệu tài khoản nhận tiền không đủ / không hợp lệ
- Không có khoản payment `SUCCESS` để hoàn
- Số tiền refund đang bị tính sai do dữ liệu lệch

Nếu reject:

- cập nhật `Status = REJECTED`
- bắt buộc nhập `Reject_Reason`

---

## Bước 4: SALES STAFF hoàn tiền thủ công

Nếu accept:

1. Cập nhật `Status = APPROVED`
2. Thực hiện chuyển tiền thủ công theo:
   - `Refund_Method`
   - `Refund_Account_Number`
   - `Refund_Account_Name` (nếu có)
   - `Customer_Account_QR` (nếu có)
3. Sau khi chuyển tiền xong:
   - upload ảnh biên nhận / ảnh xác nhận chuyển khoản
   - nhập mã tham chiếu chuyển khoản nếu có (`Refund_Reference_Code`) --> không cần thiết
4. Cập nhật request:
   - `Status = COMPLETED`
   - `Approved_By`
   - `Approved_Date`
   - `Processed_By`
   - `Processed_Date`
   - `Staff_Refund_Evidence_URL`
   - `Refund_Reference_Code`(có thể không cần)

> [BỔ SUNG MỚI] Tách rõ `Approved_By` và `Processed_By` để sau này audit tốt hơn, dù giai đoạn đầu hai người này có thể là cùng một staff.

---

## Bước 5: Đóng flow và thông báo

Sau khi hoàn tất:

- Thông báo cho CUSTOMER:
  - đơn đã hủy
  - refund đã hoàn tất
  - số tiền đã hoàn (cho phép CUSTOMER xem được ảnh bằng chứng Staff_Refund_Evidence_URL của SALES STAFF đã upload)
- Lưu timeline/audit
- Dashboard staff không còn hiển thị request này trong danh sách chờ xử lý

---

## 8. Mapping dữ liệu với bảng `Return_Exchange`

Đối với flow này, một record `Return_Exchange` đại diện cho **yêu cầu refund cấp order**.

| Cột | Giá trị trong flow hủy đơn |
|---|---|
| `Return_Type` | `REFUND` |
| `Request_Scope` | `ORDER` |
| `Order_ID` | ID đơn bị hủy |
| `User_ID` | CUSTOMER của đơn |
| `Refund_Amount` | số tiền thực thu cần hoàn |
| `Refund_Method` | do CUSTOMER cung cấp |
| `Refund_Account_Number` | do CUSTOMER cung cấp |
| `Refund_Account_Name` | do CUSTOMER cung cấp (nếu có) |
| `Customer_Account_QR` | do CUSTOMER cung cấp (nếu có) |
| `Staff_Refund_Evidence_URL` | ảnh chuyển khoản do staff upload |
| `Status` | `PENDING/APPROVED/REJECTED/COMPLETED` |

> [BỔ SUNG MỚI] Flow hủy đơn trước xác nhận **không cần** `Return_Exchange_Item`.

---

## 9. Validation chi tiết cần có ở backend

### 9.1. Validation khi CUSTOMER hủy đơn

- Order tồn tại
- Order thuộc CUSTOMER hiện tại
- Trạng thái order hợp lệ để tự hủy
- Không có refund request mở
- Nếu order đã bị hủy thì không cho hủy lại
- Nếu chưa có dữ liệu tài khoản hoàn tiền mà refundAmount > 0 thì bắt CUSTOMER nhập

### 9.2. Validation khi staff approve / complete refund

- `Return_Exchange.Status` hiện tại phải đúng
- `Refund_Amount > 0`
- Có `Refund_Method`
- Có `Refund_Account_Number`
- Có `Approved_By`, `Approved_Date` khi chuyển sang `APPROVED`
- Có `Staff_Refund_Evidence_URL`, `Processed_By`, `Processed_Date` khi chuyển sang `COMPLETED`

---

## 10. Ví dụ nghiệp vụ cụ thể

### Ví dụ 1: Đơn đặt cọc trước khi confirm

- Order total: `2.000.000`
- CUSTOMER đã trả cọc: `400.000`
- `Order_Status = PARTIALLY_PAID`
- CUSTOMER bấm hủy trước khi staff xác nhận

Kết quả:

- Order chuyển `CANCELED`
- Hệ thống tính `refundAmount = 400.000`
- Tạo `Return_Exchange` loại `REFUND`, scope `ORDER`, trạng thái `PENDING`
- Staff chuyển khoản thủ công `400.000`, upload ảnh biên nhận
- Request chuyển `COMPLETED`

### Ví dụ 2: Đơn đã trả full online

- Order total: `1.500.000`
- Payment `SUCCESS = 1.500.000`
- `Order_Status = PAID`

Kết quả:

- Order chuyển `CANCELED`
- Tạo request refund số tiền `1.500.000`
- Staff hoàn tiền thủ công
- Lưu evidence chuyển khoản

### Ví dụ 3: Đơn chưa trả tiền

- `Order_Status = PENDING`
- Không có payment `SUCCESS`

Kết quả:

- Order vẫn được `CANCELED`
- Không tạo `Return_Exchange`
- Flow kết thúc ngay

---

## 11. Gợi ý API để triển khai

### 11.1. CUSTOMER hủy đơn

`POST /api/customer/orders/{orderId}/cancel`

Ví dụ payload:

```json
{
  "cancelReason": "Đổi nhu cầu mua hàng",
  "refundMethod": "BANK_TRANSFER",
  "refundAccountNumber": "123456789",
  "refundAccountName": "NGUYEN VAN A",
  "note": "Vui lòng hoàn lại vào tài khoản trên nếu đơn đã thu tiền"
}
```

### 11.2. SALES STAFF reject refund

`PUT /api/staff/return-exchange/{id}/reject`

```json
{
  "rejectReason": "Thông tin tài khoản nhận tiền chưa hợp lệ"
}
```

### 11.3. SALES STAFF approve refund

`PUT /api/staff/return-exchange/{id}/approve`

```json
{
  "approvedNote": "Đã kiểm tra payment SUCCESS hợp lệ"
}
```

### 11.4. SALES STAFF complete manual refund

`PUT /api/staff/return-exchange/{id}/complete-refund`

```json
{
  "refundReferenceCode": "VCB-20260315-8891",
  "staffRefundEvidenceUrl": "https://cdn.example.com/refund-proof/order-123.png"
}
```

---

## 12. Những lỗi thường gặp cần tránh

1. Chỉ check `status != CONFIRMED` thay vì whitelist trạng thái được hủy.
2. Vừa hủy đơn vừa coi như đã hoàn tiền xong dù staff chưa chuyển tiền.
3. Không lưu evidence chuyển khoản.
4. Không tách audit người duyệt và người thực hiện.
5. Không hoàn kho khi hệ thống đã trừ kho từ trước.
6. Tạo nhiều request refund trùng cho cùng order.
7. Cho phép chỉnh `Refund_Amount` bừa bãi mà không đối soát payment thực thu.

---

## 13. Kết luận

Flow hủy đơn trước khi SALES STAFF xác nhận nên được chốt theo hướng:

1. CUSTOMER hủy đơn khi đơn còn ở nhóm trạng thái an toàn.
2. Hệ thống khóa order bằng `CANCELED` và `Invoice.Staus = CANCELED`.
3. Nếu có tiền đã thu thì sinh `Return_Exchange` loại `REFUND`, scope `ORDER`.
4. SALES STAFF hoàn tiền **thủ công** dựa trên `refund_method` và `refund_account_number`.
5. Sau khi hoàn xong phải upload ảnh evidence và lưu audit đầy đủ.
6. Nếu hệ thống đã trừ kho thì phải có nghiệp vụ hoàn kho tương ứng.

Tài liệu này là phiên bản đã được chuẩn hóa lại để phù hợp triển khai thực tế, nhưng vẫn giữ thiết kế đủ đơn giản cho giai đoạn hiện tại.
