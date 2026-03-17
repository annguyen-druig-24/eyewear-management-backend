# Flow CUSTOMER gửi yêu cầu trả hàng / hoàn tiền do lỗi sản phẩm (manual refund)

## 1. Mục đích tài liệu

Tài liệu này mô tả lại chi tiết flow CUSTOMER gửi yêu cầu trả hàng và hoàn tiền do lỗi sản phẩm, theo hướng:

- rõ bước nghiệp vụ
- dễ implement backend/frontend
- bám theo schema `Return_Exchange` + `Return_Exchange_Item`
- phù hợp với phạm vi hiện tại: **STAFF hoàn tiền thủ công**, sau đó upload ảnh biên nhận chuyển khoản để lưu evidence

---

## 2. Ghi chú chỉnh sửa so với file gốc

### 2.1. Phần được giữ lại

- Giữ ý tưởng CUSTOMER tạo request từ lịch sử đơn hàng.
- Giữ hướng xử lý item-level theo `Order_Detail` và mở rộng thêm `Prescription_Order_Detail`.
- Giữ flow staff:
  - xem request
  - kiểm tra
  - accept hoặc reject
  - nếu accept thì hoàn tiền và upload evidence
- Giữ bộ trạng thái đơn giản:
  - `PENDING`
  - `APPROVED`
  - `REJECTED`
  - `COMPLETED`

### 2.2. Phần được chỉnh sửa / bổ sung

> [BỔ SUNG MỚI] Làm rõ phạm vi: hiện tại chỉ làm **manual refund**.  
> [CHỈNH SỬA] Chuẩn hóa input dữ liệu CUSTOMER cần nhập từ đầu:
> - refund method
> - refund account number
> - refund account name (khuyến nghị)  
> [BỔ SUNG MỚI] Làm rõ validation quantity, eligibility window và chống trùng request.  
> [BỔ SUNG MỚI] Bổ sung mapping rất cụ thể với `Return_Exchange` và `Return_Exchange_Item`.  
> [BỔ SUNG MỚI] Bổ sung ví dụ thực tế, API gợi ý, timeline trạng thái, rule inventory sau khi nhận hàng hoàn (sẽ làm sau về rule inventory sau khi nhận hàng hoàn).

---

## 3. Phạm vi áp dụng

Flow này áp dụng khi:

1. Đơn hàng đã giao thành công / đủ điều kiện tiếp nhận trả hàng theo chính sách.
2. CUSTOMER gửi yêu cầu vì:
   - hàng lỗi
   - sai sản phẩm
   - tình trạng sản phẩm không đúng mô tả
3. Hệ thống cần lưu cả:
   - lý do và bằng chứng CUSTOMER cung cấp
   - kết quả xử lý của SALES STAFF
   - evidence hoàn tiền do staff upload

### 3.1. Giả định nghiệp vụ hiện tại

- Mốc thời gian tiếp nhận yêu cầu: ví dụ trong vòng **7 ngày** kể từ khi đơn hoàn tất / nhận hàng.
- Refund được thực hiện **thủ công**, không gọi API refund của gateway.
- Staff là người ra quyết định cuối cùng dựa trên:
  - policy
  - số lượng hợp lệ
  - tình trạng sản phẩm
  - đối soát order/payment

> [BỔ SUNG MỚI] Mục này được viết lại để tài liệu rõ “scope hiện tại” hơn file gốc.

---

## 4. Mục tiêu nghiệp vụ

1. CUSTOMER có thể chọn đúng item cần trả.
2. Hệ thống lưu được lý do và bằng chứng của khách.
3. SALES STAFF có thể dễ dàng xem, duyệt, từ chối hoặc hoàn tiền.
4. Staff upload được ảnh biên nhận sau khi chuyển khoản.
5. Dữ liệu lưu đủ để:
   - truy vết
   - đối soát
   - hiển thị timeline cho CUSTOMER
   - hỗ trợ báo cáo và vận hành

---

## 5. Mapping với schema

## 5.1. `Return_Exchange`

Đây là bản ghi header của request.

Các field chính dùng trong flow này:

- `Order_ID`: đơn hàng gốc
- `User_ID`: CUSTOMER tạo yêu cầu
- `Return_Type = RETURN`
- `Request_Scope = ITEM`
- `Return_Reason`: lý do trả hàng
- `Refund_Method`
- `Refund_Account_Number`
- `Refund_Account_Name`
- `Customer_Account_QR`: hỉnh ảnh mã QR từ tài khoản ngân hàng hoặc Momo của CUSTOMER
- `Status`
- `Approved_By`, `Approved_Date`
- `Processed_By`, `Processed_Date`
- `Reject_Reason`
- `Staff_Refund_Evidence_URL`

## 5.2. `Return_Exchange_Item`

Mỗi dòng thể hiện một item cụ thể CUSTOMER muốn trả:

- `Return_Exchange_ID`
- `Item_Source`
- `Order_Detail_ID` hoặc `Prescription_Order_Detail_ID`
- `Quantity`
- `Item_Evidence_URL` (ảnh bằng chứng của từng sản phẩm)
- `Item_Reason`
- `Note`

> [CHỈNH SỬA] File gốc giải thích mapping đúng hướng nhưng còn ngắn. Bản này làm rõ hơn để dev dễ code.

---

## 6. Điều kiện tạo request

CUSTOMER chỉ được tạo request nếu thỏa mãn tất cả điều kiện sau:

1. Order thuộc đúng CUSTOMER hiện tại.
2. Order ở trạng thái hợp lệ theo policy, ví dụ:
   - `COMPLETED`
   - hoặc đã giao thành công theo logic hệ thống
3. Yêu cầu được gửi trong thời hạn cho phép, ví dụ:
   - trong vòng 7 ngày từ `delivered_at` hoặc `completed_at` (Dựa theo Shipping_Info.Delivered_At - Delivered_At là thời gian CUSTOMER nhận được hàng)
4. Item được chọn thật sự thuộc order.
5. Tổng quantity xin trả không vượt số lượng đã mua và chưa trả trước đó.
6. Không có request mở trùng cho cùng item nếu policy không cho phép.

---

## 7. Dữ liệu CUSTOMER cần nhập

Khi tạo request, giao diện nên yêu cầu CUSTOMER nhập:

### 7.1. Dữ liệu chung của request

- `return_reason`
- `request_note` / mô tả chi tiết lỗi

### 7.2. Dữ liệu refund

- `refund_method` (CUSTOMER có thể tự nhập method CUSTOMER mong muốn. VD: ABC, VCB, Momo, ...)
- `refund_account_number`
- `refund_account_name` (khuyến nghị thêm)
- `customer_account_qr` (khuyến nghị thêm để staff chuyển khoản nhanh hơn)

### 7.3. Dữ liệu theo từng item

- `item_source` (`ORDER_DETAIL` hoặc `PRESCRIPTION_ORDER_DETAIL`)
- `order_detail_id` hoặc `prescription_order_detail_id`
- `quantity`
- `item_evidence_url` (mỗi item 1 ảnh theo yêu cầu mới)
- `item_reason`
- `note`

> [BỔ SUNG MỚI] File gốc chưa nói rõ customer cần nhập refund info từ lúc nào. Bản này chốt luôn nhập từ bước tạo request để staff có thể hoàn tiền ngay sau khi approve. Sau khi SALES STAFF bấm xác nhận (APPROVED) thì trên UI sẽ có phần upload file ảnh để SALES STAFF có thể upload file ảnh chuyển tiền bằng chứng. SALES STAFF chỉ có thể bấm "Hoàn thành" (COMPLETED) khi và chỉ khi đã upload file ảnh chuyển tiền bằng chứng.

---

## 8. Flow chi tiết end-to-end

## Bước 1: CUSTOMER tạo Return Request

1. CUSTOMER vào lịch sử đơn hàng.
2. Chọn `Order` cần trả hàng.
3. Chọn một hoặc nhiều item từ `Order_Detail` hoặc `Prescription_Order_Detail`.
4. Nhập:
   - lý do trả hàng
   - mô tả lỗi
   - ảnh bằng chứng theo từng sản phẩm
   - số lượng cần trả cho từng item
   - thông tin nhận hoàn tiền
5. Hệ thống validate.
6. Nếu hợp lệ, tạo:
   - `Return_Exchange` với `Status = PENDING`
   - các dòng `Return_Exchange_Item`

### 8.1.1. Dữ liệu tạo mẫu

`Return_Exchange`

- `Return_Type = RETURN`
- `Request_Scope = ITEM`
- `Status = PENDING`

`Return_Exchange_Item`

- 1 hoặc nhiều dòng theo item được chọn

---

## Bước 2: Hệ thống thông báo cho SALES STAFF

Sau khi tạo thành công:

- hiển thị request mới trong dashboard staff
- gửi email/in-app notification nếu có
- CUSTOMER nhìn thấy request ở trạng thái `PENDING`

---

## Bước 3: SALES STAFF tiếp nhận và kiểm tra

SALES STAFF mở request và xem:

- thông tin order
- customer
- các item yêu cầu trả
- quantity
- ảnh lỗi của từng item
- refund method / refund account number / refund account name
- customer account QR (nếu có)

### 8.3.1. Những gì staff cần kiểm tra

1. Đúng order của customer hay không
2. Item có thuộc order hay không
3. Quantity có hợp lệ hay không
4. Request có còn trong thời hạn policy hay không (Trong vòng 7 ngày kể từ khi nhận được hàng, Shipping_Info.Delivered_At chính là thời gian CUSTOMER nhận được hàng)
5. Lý do / evidence có đủ cơ sở hay không
6. Payment của order có đủ điều kiện hoàn tiền hay không

> [BỔ SUNG MỚI] Bước này được chi tiết hóa hơn để tránh hiểu “staff chỉ nhìn và bấm accept/reject”.

---

## Bước 4: SALES STAFF ra quyết định

### 8.4.1. Nhánh từ chối

Nếu request không hợp lệ:

1. Staff bấm **Reject**
2. Nhập `reject_reason`
3. Hệ thống cập nhật:
   - `Status = REJECTED`
   - `Reject_Reason = ...`

CUSTOMER sẽ thấy kết quả từ chối và lý do.

### 8.4.2. Nhánh chấp nhận

Nếu request hợp lệ:

1. Staff bấm **Approve**
2. Hệ thống cập nhật:
   - `Status = APPROVED`
   - `Approved_By`
   - `Approved_Date`

Sau đó staff tiến hành hoàn tiền thủ công.

---

## Bước 5: SALES STAFF hoàn tiền thủ công

Sau khi request đã `APPROVED`, staff thực hiện:

1. Chuyển tiền thủ công theo:
   - `Refund_Method`
   - `Refund_Account_Number`
   - `Refund_Account_Name`
   - `Customer_Account_QR`
2. Kiểm tra số tiền hoàn theo policy
3. Upload ảnh chuyển khoản / ảnh biên nhận
4. Nhập `Refund_Reference_Code` nếu có
5. Hệ thống cập nhật:
   - `Status = COMPLETED`
   - `Refund_Amount`
   - `Processed_By`
   - `Processed_Date`
   - `Staff_Refund_Evidence_URL`
   - `Refund_Reference_Code`

> [CHỈNH SỬA QUAN TRỌNG] Vì hiện tại bạn chỉ làm refund thủ công, tài liệu này bỏ logic auto-refund và mô tả rất rõ hành vi thực tế của staff.

---

## Bước 6: Xử lý hàng hoàn và tồn kho 

> [BỔ SUNG MỚI - QUAN TRỌNG]

Nếu item trả hàng đã được hệ thống / staff xác nhận nhận lại thực tế, cần có rule vận hành cho tồn kho:

- Nếu sản phẩm còn bán lại được:
  - tạo nghiệp vụ nhập lại kho / inventory return
- Nếu sản phẩm lỗi, hư hỏng, không bán lại:
  - không cộng lại vào kho bán
  - có thể nhập vào kho lỗi / kho chờ xử lý nếu hệ thống hỗ trợ

Tài liệu này không ép phải làm ngay trong `Return_Exchange`, nhưng khuyến nghị:

- inventory transaction nên là bảng/nghiệp vụ riêng
- không cộng số lượng tồn kho bằng tay

---

## 9. State machine đề xuất

| Trạng thái | Ý nghĩa | Ai cập nhật |
|---|---|---|
| `PENDING` | CUSTOMER vừa tạo request | Hệ thống |
| `APPROVED` | STAFF đồng ý xử lý request | SALES STAFF |
| `REJECTED` | STAFF từ chối request | SALES STAFF |
| `COMPLETED` | STAFF đã hoàn tiền xong và upload evidence | SALES STAFF |

### 9.1. Rule chuyển trạng thái

- `PENDING -> APPROVED`
- `PENDING -> REJECTED`
- `APPROVED -> COMPLETED`

Không cho phép:

- `REJECTED -> COMPLETED`
- `COMPLETED -> PENDING`
- `PENDING -> COMPLETED` trực tiếp nếu chưa có thao tác duyệt

> [BỔ SUNG MỚI] File gốc có nêu status nhưng chưa nói rõ allowed transitions.

---

## 10. Validation chi tiết cần có

### 10.1. Validation khi CUSTOMER tạo request

- Order tồn tại
- Order thuộc user hiện tại
- Order đủ điều kiện thời gian
- `Return_Type = RETURN`
- `Request_Scope = ITEM`
- Có ít nhất 1 item
- Mỗi `Order_Detail_ID` đều thuộc order đó
- Với `Item_Source = PRESCRIPTION_ORDER_DETAIL`, mỗi `Prescription_Order_Detail_ID` cũng phải map đúng về `Order_ID`
- `Quantity > 0`
- Tổng số lượng xin trả không vượt số lượng đã mua
- Mỗi item phải có `Item_Evidence_URL` nếu policy yêu cầu
- Có refund method và refund account number nếu flow cần hoàn tiền

### 10.2. Validation khi staff approve

- Request đang ở `PENDING`
- Item hợp lệ
- Request đủ điều kiện refund theo policy
- Không có dấu hiệu request trùng / gian lận

### 10.3. Validation khi staff complete

- Request đang ở `APPROVED`
- `Refund_Amount > 0`
- Có `Staff_Refund_Evidence_URL`
- Có `Processed_By`, `Processed_Date`

---

## 11. Ví dụ nghiệp vụ cụ thể

### Ví dụ 1: Trả 1 gọng kính bị lỗi bản lề

- Order có 2 item
- CUSTOMER chọn item `Order_Detail_ID = 1001`
- quantity trả = `1`
- upload 1 ảnh lỗi cho item đó tại `Item_Evidence_URL`
- nhập:
  - `Refund_Method = BANK_TRANSFER`
  - `Refund_Account_Number = 123456789`
  - `Refund_Account_Name = NGUYEN VAN A`

Kết quả:

- tạo 1 header `Return_Exchange`
- tạo 1 dòng `Return_Exchange_Item`
- request ở trạng thái `PENDING`

Sau khi staff xác minh:

- request -> `APPROVED`
- staff chuyển khoản
- upload ảnh biên nhận
- request -> `COMPLETED`

### Ví dụ 2: CUSTOMER xin trả vượt số lượng đã mua

- Order_Detail quantity mua = `1`
- CUSTOMER chọn quantity trả = `2`

Kết quả:

- backend reject ngay tại bước validate
- không tạo request

### Ví dụ 3: CUSTOMER gửi trùng request cho cùng item

- item đang nằm trong request `PENDING`
- CUSTOMER tiếp tục bấm tạo request mới cho cùng item

Kết quả:

- backend chặn
- trả message phù hợp, ví dụ:
  - `A return request for this item is already being processed`

---

## 12. Gợi ý API để triển khai

### 12.1. CUSTOMER tạo return request

`POST /api/customer/return-exchange`

Ví dụ payload:

```json
{
  "orderId": 123,
  "returnType": "RETURN",
  "requestScope": "ITEM",
  "returnReason": "Gọng kính bị lỗi bản lề",
  "requestNote": "Kính phát ra tiếng kêu khi mở càng, bản lề lỏng",
  "refundMethod": "BANK_TRANSFER",
  "refundAccountNumber": "123456789",
  "refundAccountName": "NGUYEN VAN A",
  "customerAccountQr": "https://cdn.example.com/qr/nguyen-van-a.png",
  "items": [
    {
      "itemSource": "ORDER_DETAIL",
      "orderDetailId": 1001,
      "quantity": 1,
      "itemEvidenceUrl": "https://cdn.example.com/return/item-1001-proof.jpg",
      "itemReason": "Lỗi bản lề",
      "note": "Sản phẩm nhận ngày 10/03"
    }
  ]
}
```

### 12.2. SALES STAFF approve request

`PUT /api/staff/return-exchange/{id}/approve`

```json
{
  "approvedNote": "Đã xác nhận lỗi hợp lệ theo chính sách"
}
```

### 12.3. SALES STAFF reject request

`PUT /api/staff/return-exchange/{id}/reject`

```json
{
  "rejectReason": "Yêu cầu đã quá thời hạn tiếp nhận đổi trả"
}
```

### 12.4. SALES STAFF complete manual refund

`PUT /api/staff/return-exchange/{id}/complete-refund`

```json
{
  "refundAmount": 450000,
  "refundReferenceCode": "VCB-20260315-9912",
  "staffRefundEvidenceUrl": "https://cdn.example.com/refund-proof/return-123.png",
  "processedNote": "Đã hoàn tiền thủ công cho khách"
}
```

---

## 13. Gợi ý UI / timeline cho CUSTOMER

CUSTOMER nên thấy timeline như sau:

1. `PENDING` — yêu cầu đã được ghi nhận
2. `APPROVED` — staff đã chấp nhận xử lý
3. `REJECTED` — staff từ chối, hiển thị lý do
4. `COMPLETED` — đã hoàn tiền xong

Nếu muốn hiển thị rõ hơn trên UI, có thể thêm:

- số tiền hoàn
- ngày staff xử lý
- mã tham chiếu hoàn tiền
- ảnh evidence (nếu policy cho phép customer xem) --> Sẽ cho phép CUSTOMER xem được Staff_Refund_Evidence_URL

---

## 14. Những lỗi thường gặp cần tránh

1. Không validate item có thuộc order hay không.
2. Không validate quantity còn được phép trả.
3. Không lưu refund info ngay từ đầu khiến staff phải hỏi lại khách.
4. Không lưu evidence chuyển khoản.
5. Cho phép chuyển `PENDING -> COMPLETED` trực tiếp.
6. Không tách bước approve và complete trong manual refund.
7. Không xử lý tồn kho sau khi nhận hàng trả thực tế.

---

## 15. Kết luận

Flow trả hàng / hoàn tiền do lỗi sản phẩm nên được chốt như sau:

1. CUSTOMER chọn order và item cần trả.
2. CUSTOMER nhập lý do, bằng chứng và thông tin nhận hoàn tiền.
3. Hệ thống tạo `Return_Exchange` + `Return_Exchange_Item` ở trạng thái `PENDING`.
4. SALES STAFF kiểm tra và:
   - reject nếu không hợp lệ
   - approve nếu hợp lệ
5. SALES STAFF hoàn tiền **thủ công**.
6. SALES STAFF upload ảnh biên nhận chuyển khoản.
7. Hệ thống cập nhật `COMPLETED` và lưu audit đầy đủ.

Đây là phiên bản chi tiết hơn, đầy đủ hơn và thực tế hơn file gốc, nhưng vẫn giữ đủ đơn giản để bạn triển khai ngay ở giai đoạn hiện tại.
