# Quy tắc cập nhật status cho flow CUSTOMER hủy đơn trước khi SALES STAFF xác nhận

## 1. Mục tiêu tài liệu

Tài liệu này gộp lại phần giải thích về **thời điểm cập nhật status** cho các bảng liên quan khi CUSTOMER hủy đơn hàng **trước khi SALES STAFF xác nhận**, đặc biệt cho các trạng thái:

- `Order.Order_Status`
- `Invoice.Status`
- `Shipping_Info.Shipping_Status`
- `Payment.Status`
- `Return_Exchange.Status`

Mục tiêu là giúp team FE/BE thống nhất:

1. Hủy đơn ở thời điểm nào.
2. Hoàn tiền ở thời điểm nào.
3. Payment nào đổi sang `CANCELED`, payment nào đổi sang `REFUNDED`.
4. Staff `REJECT/APPROVE/COMPLETE` sẽ ảnh hưởng tới status nào.

---

## 2. Nguyên tắc nghiệp vụ cốt lõi

### 2.1. Hủy đơn và hoàn tiền là 2 nghiệp vụ khác nhau

- **Hủy đơn** là nghiệp vụ dừng vòng đời đơn hàng.
- **Hoàn tiền** là nghiệp vụ xử lý số tiền hệ thống đã thu từ CUSTOMER.

Vì vậy:

- Có thể có đơn `CANCELED` nhưng **chưa refund xong**.
- Có thể có đơn `CANCELED` và **không cần refund** nếu chưa thu đồng nào.
- Không được hiểu `Order_Status = CANCELED` đồng nghĩa với `đã hoàn tiền`.

### 2.2. Chỉ hoàn tiền trên phần tiền đã thu thành công

Số tiền refund được xác định theo nguyên tắc:

```text
refundAmount = tổng Payment.Amount có Status = SUCCESS
             - tổng số tiền đã refund thành công trước đó
```

Không tính vào refund:

- payment `PENDING`
- payment `FAILED`
- payment `CANCELED`
- khoản tiền chưa từng được thu thành công

### 2.3. Giữ đúng sự thật dữ liệu kế toán

Nếu payment đã thu thành công thì **không nên đổi ngay sang `REFUNDED` chỉ vì CUSTOMER vừa tạo yêu cầu hủy**.

Quy tắc đúng là:

- Khoản tiền đã thu vẫn giữ `Payment.Status = SUCCESS`
- Chỉ khi SALES STAFF **thực sự hoàn tiền xong** và bấm **Complete** thì mới đổi sang `Payment.Status = REFUNDED`
- Dấu vết xử lý hoàn tiền được theo dõi qua `Return_Exchange`

---

## 3. Thời điểm cập nhật status tổng quát

## 3.1. Khi CUSTOMER bấm “Hủy đơn hàng” và xác nhận thành công

Sau khi backend validate hợp lệ:

- đúng chủ đơn hàng
- `Order_Status` thuộc whitelist cho phép hủy: `PENDING`, `PARTIALLY_PAID`, `PAID`
- đơn chưa `CONFIRMED`
- chưa có request refund mở

thì hệ thống nên cập nhật **ngay**:

- `Order.Order_Status = CANCELED`
- `Invoice.Status = CANCELED`
- `Shipping_Info.Shipping_Status = CANCELED`

Đây là lựa chọn nên áp dụng vì:

1. Đơn phải được khóa ngay để không tiếp tục đi vào pipeline xử lý.
2. Tránh race condition: staff khác xác nhận đơn trong lúc customer đã hủy.
3. `Return_Exchange` chỉ phản ánh tiến trình refund, không quyết định việc order có bị hủy hay chưa.

> Kết luận quan trọng: **không nên chờ tới lúc SALES STAFF approve/complete refund mới cập nhật Order/Invoice/Shipping sang `CANCELED`**.

---

## 3.2. Ngay sau khi order bị hủy, hệ thống xác định có cần refund hay không

Hệ thống tính:

```text
totalPaidSuccess = SUM(Payment.Amount WHERE Order_ID = ? AND Status = SUCCESS)
totalRefunded    = tổng số tiền đã refund thành công trước đó
refundAmount     = totalPaidSuccess - totalRefunded
```

### Nếu `refundAmount <= 0`

- Không tạo `Return_Exchange`
- Flow kết thúc ngay
- CUSTOMER thấy đơn đã hủy thành công

### Nếu `refundAmount > 0`

- Tạo `Return_Exchange`
- `Return_Exchange.Status = PENDING`
- SALES STAFF sẽ xử lý hoàn tiền thủ công

---

## 4. Quy tắc cập nhật status cho từng bảng

## 4.1. Order

### Rule
- Khi CUSTOMER xác nhận hủy đơn hợp lệ: `Order.Order_Status = CANCELED`

### Không nên làm
- Không nên giữ `Order_Status = PENDING/PARTIALLY_PAID/PAID` chỉ vì refund chưa hoàn tất
- Không nên chờ staff `APPROVE/COMPLETE` mới đổi sang `CANCELED`

### Lý do
- Order lifecycle phải dừng ngay tại thời điểm customer hủy thành công
- Refund là luồng tài chính đi sau, không phải điều kiện để order được xem là còn hiệu lực

---

## 4.2. Invoice

### Rule
- Khi CUSTOMER xác nhận hủy đơn hợp lệ: `Invoice.Status = CANCELED`

### Giải thích
`Invoice.Status = CANCELED` nghĩa là chứng từ bán hàng/đơn bán đã bị hủy.

Điều này **không mâu thuẫn** với việc trước đó hệ thống đã thu tiền, vì sự thật thu tiền và hoàn tiền sẽ được phản ánh ở:

- `Payment.Status`
- `Return_Exchange.Status`
- audit / evidence / processed date

### Không nên làm
- Không nên đổi invoice về `UNPAID` một cách máy móc nếu tiền đã từng thu thành công

---

## 4.3. Shipping_Info

### Rule
- Khi CUSTOMER xác nhận hủy đơn hợp lệ: `Shipping_Info.Shipping_Status = CANCELED`

### Lý do
Flow này chỉ áp dụng trước khi SALES STAFF xác nhận, nên shipping chưa nên đi tiếp.
Khi order đã bị hủy thì shipping cũng phải dừng ngay.

---

## 4.4. Payment

Payment là phần cần tách rule rõ nhất.

### Rule tổng quát

#### a) Payment đã thu thành công (`SUCCESS`)
- Giữ nguyên `SUCCESS` khi customer vừa tạo yêu cầu hủy
- Chỉ đổi sang `REFUNDED` khi staff **đã hoàn tiền thực tế** và bấm **Complete refund**

#### b) Payment chưa thu (`PENDING`)
- Nếu order bị hủy và khoản tiền đó sẽ không còn được thu nữa thì cập nhật ngay sang `CANCELED`

#### c) Payment thất bại (`FAILED`)
- Giữ nguyên đúng bản chất thất bại, không tham gia refund

### Tóm tắt tư duy
- `SUCCESS` = tiền đã vào hệ thống
- `REFUNDED` = tiền đã trả lại customer
- `PENDING` = tiền chưa thu
- `CANCELED` = khoản tiền bị hủy, không thu nữa

---

## 4.5. Return_Exchange

`Return_Exchange` dùng để theo dõi tiến trình refund thủ công.

### Các trạng thái
- `PENDING`: customer vừa tạo yêu cầu refund, chờ staff xử lý
- `APPROVED`: staff đồng ý hoàn tiền
- `REJECTED`: staff từ chối refund và phải có lý do
- `COMPLETED`: staff đã hoàn tiền xong và upload evidence

### Ý nghĩa
- `Return_Exchange.Status` **không thay thế** cho `Order.Order_Status`
- `Order` có thể đã `CANCELED` trong khi `Return_Exchange` vẫn đang `PENDING` hoặc `APPROVED`

---

## 5. Giải đáp theo từng case nghiệp vụ

## 5.1. Case 1: Có cọc online + phần còn lại COD

Ví dụ:

- Record 1: `Payment_Purpose = DEPOSIT`, `Payment_Method = VNPAY`, `Status = SUCCESS`
- Record 2: `Payment_Purpose = REMAINING`, `Payment_Method = COD`, `Status = PENDING`

### Cập nhật đúng nên là

#### Record DEPOSIT / VNPAY / SUCCESS
- Khi CUSTOMER vừa hủy đơn thành công: **giữ `SUCCESS`**
- Khi SALES STAFF hoàn tiền xong và bấm Complete: đổi sang **`REFUNDED`**

#### Record REMAINING / COD / PENDING
- Khi CUSTOMER hủy đơn thành công: đổi ngay sang **`CANCELED`**

### Vì sao?
- Khoản `DEPOSIT` là tiền đã thu thật, nên chưa thể đánh dấu `REFUNDED` nếu staff chưa chuyển lại tiền
- Khoản `REMAINING COD` chưa thu, nên sau khi đơn bị hủy thì phải dừng luôn bằng status `CANCELED`

### Kết luận case 1
- `DEPOSIT SUCCESS` -> `REFUNDED` **khi complete refund**
- `REMAINING COD PENDING` -> `CANCELED` **ngay khi cancel order**

---

## 5.2. Case 2: Không có cọc, thanh toán full bằng COD

Ví dụ:

- `Payment_Purpose = FULL`, `Payment_Method = COD`, `Status = PENDING`

### Cập nhật đúng nên là
- Khi CUSTOMER hủy đơn thành công: đổi ngay sang **`CANCELED`**

### Vì sao?
- Đây là khoản tiền chưa thu
- Không có `Payment.SUCCESS` nên không phát sinh refund
- Không cần tạo `Return_Exchange`

### Kết luận case 2
- `FULL COD PENDING` -> `CANCELED` **ngay khi cancel order**

---

## 5.3. Case 3: Không có cọc, thanh toán full bằng VNPAY/PAYOS

Ví dụ:

- `Payment_Purpose = FULL`, `Payment_Method = VNPAY/PAYOS`, `Status = SUCCESS`

### Cập nhật đúng nên là
- Khi CUSTOMER vừa hủy đơn: **giữ `SUCCESS`**
- Khi SALES STAFF đã hoàn tiền thủ công xong và bấm Complete: đổi sang **`REFUNDED`**

### Vì sao?
- Đây là tiền đã thu thành công
- Nếu đổi sang `REFUNDED` ngay khi customer tạo request thì dữ liệu sẽ sai thực tế
- Chỉ `Return_Exchange.Status = COMPLETED` mới chứng minh rằng refund đã được xử lý xong

### Kết luận case 3
- `FULL online SUCCESS` -> `REFUNDED` **sau khi complete refund**

---

## 6. SALES STAFF reject / approve / complete ảnh hưởng thế nào?

## 6.1. Khi staff REJECT

Cập nhật:
- `Return_Exchange.Status = REJECTED`
- bắt buộc có `Reject_Reason`

### Quan trọng
- `Order.Order_Status` **vẫn là `CANCELED`**
- `Invoice.Status` **vẫn là `CANCELED`**
- `Shipping_Info.Shipping_Status` **vẫn là `CANCELED`**

### Không nên làm
- Không nên tự động mở lại order chỉ vì refund request bị reject

### Lý do
Reject refund không đồng nghĩa với việc order được khôi phục lại.
Thông thường reject chỉ phản ánh rằng staff chưa thể xử lý hoàn tiền do thiếu/sai thông tin hoặc lý do nghiệp vụ khác.

---

## 6.2. Khi staff APPROVE

Cập nhật:
- `Return_Exchange.Status = APPROVED`
- lưu `Approved_By`, `Approved_Date`

### Quan trọng
- Chưa đổi `Payment.SUCCESS` thành `REFUNDED`
- Vì lúc này staff mới chỉ đồng ý hoàn tiền, chưa chắc đã chuyển khoản xong

---

## 6.3. Khi staff COMPLETE refund

Cập nhật:
- `Return_Exchange.Status = COMPLETED`
- lưu evidence chuyển khoản
- lưu `Processed_By`, `Processed_Date`
- cập nhật các payment `SUCCESS` liên quan sang `REFUNDED`

### Đây là thời điểm đúng để đổi `Payment.Status = REFUNDED`
Vì đây là lúc refund đã thực sự hoàn thành.

---

## 7. Bảng rule tổng hợp ngắn gọn

| Tình huống | Order | Invoice | Shipping | Payment SUCCESS | Payment PENDING | Return_Exchange |
|---|---|---|---|---|---|---|
| CUSTOMER xác nhận hủy đơn hợp lệ | `CANCELED` | `CANCELED` | `CANCELED` | Giữ nguyên `SUCCESS` | Đổi `CANCELED` nếu không còn thu | Nếu cần refund thì tạo `PENDING` |
| Không cần refund | `CANCELED` | `CANCELED` | `CANCELED` | Không có | `CANCELED` | Không tạo |
| Staff APPROVE refund | Giữ `CANCELED` | Giữ `CANCELED` | Giữ `CANCELED` | Vẫn `SUCCESS` | Không đổi thêm | `APPROVED` |
| Staff REJECT refund | Giữ `CANCELED` | Giữ `CANCELED` | Giữ `CANCELED` | Vẫn `SUCCESS` | Không đổi thêm | `REJECTED` |
| Staff COMPLETE refund | Giữ `CANCELED` | Giữ `CANCELED` | Giữ `CANCELED` | `REFUNDED` | Không đổi thêm | `COMPLETED` |

---

## 8. Kết luận cuối cùng để chốt với team

### Rule nên áp dụng

1. **Ngay khi CUSTOMER xác nhận hủy đơn hợp lệ**:
   - `Order_Status = CANCELED`
   - `Invoice.Status = CANCELED`
   - `Shipping_Info.Shipping_Status = CANCELED`

2. **Đối với Payment**:
   - khoản nào `PENDING` và sẽ không còn thu nữa -> `CANCELED`
   - khoản nào `SUCCESS` -> giữ nguyên `SUCCESS` cho đến khi hoàn tiền thật sự xong

3. **Nếu có tiền đã thu thành công**:
   - tạo `Return_Exchange.Status = PENDING`
   - staff xử lý theo luồng `PENDING -> APPROVED/REJECTED -> COMPLETED`

4. **Chỉ khi SALES STAFF hoàn tiền xong và bấm Complete**:
   - mới cập nhật `Payment.Status = REFUNDED`

### Một câu chốt ngắn gọn

**Hủy đơn phải diễn ra ngay khi customer xác nhận hủy hợp lệ; refund là bước xử lý tài chính đi sau. Vì vậy Order/Invoice/Shipping nên `CANCELED` ngay, còn Payment đã thu thì chỉ chuyển sang `REFUNDED` khi staff hoàn tiền xong.**