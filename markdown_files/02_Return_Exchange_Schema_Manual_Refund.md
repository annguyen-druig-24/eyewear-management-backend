# Thiết kế schema cho `Return_Exchange` và `Return_Exchange_Item` (phiên bản tối ưu cho hoàn tiền thủ công)

## 1. Mục đích tài liệu

Tài liệu này chuẩn hóa schema database cho 2 flow chính:

1. CUSTOMER hủy đơn trước khi SALES STAFF xác nhận và cần refund cấp **ORDER**.
2. CUSTOMER gửi yêu cầu trả hàng / hoàn tiền do lỗi sản phẩm ở mức **ITEM**.

Phiên bản này tối ưu cho giai đoạn hiện tại:

- STAFF hoàn tiền **thủ công**
- Hoàn tiền dựa vào `refund_method`, `refund_account_number`
- Có lưu bằng chứng chuyển khoản của STAFF để audit
- UI cho phép trả nhiều sản phẩm trong 1 request, nên evidence của CUSTOMER cần lưu theo **từng item**
---

## 2. Ghi chú chỉnh sửa so với file gốc

### 2.1. Phần giữ lại

- Giữ mô hình **Header + Item**:
  - `Return_Exchange` = header cấp yêu cầu
  - `Return_Exchange_Item` = dòng chi tiết item
- Giữ ý tưởng:
  - `REFUND` dùng cho scope `ORDER`
  - `RETURN`, `WARRANTY` dùng cho scope `ITEM`
- Giữ bộ trạng thái đơn giản:
  - `PENDING`
  - `APPROVED`
  - `REJECTED`
  - `COMPLETED`

### 2.2. Phần chỉnh sửa / bổ sung

> [CHỈNH SỬA] Bỏ cột `Quantity` ở header vì dễ trùng và lệch với `Return_Exchange_Item`.
> [CHỈNH SỬA] Không lưu `Customer_Evidence_URL` ở header nữa.
> [CHỈNH SỬA] Lưu evidence CUSTOMER theo từng item tại `Return_Exchange_Item.Item_Evidence_URL`.
> [CHỈNH SỬA] Giữ `Staff_Refund_Evidence_URL` ở header vì đây là bằng chứng chuyển khoản cấp request.
> [BỔ SUNG MỚI] Thêm `Refund_Account_Name` để staff chuyển khoản chính xác hơn.
> [BỔ SUNG MỚI] Thêm `Refund_Reference_Code` để lưu mã giao dịch/ghi chú tham chiếu.
> [BỔ SUNG MỚI] Thêm `Processed_By`, `Processed_Date` để audit bước hoàn tiền thủ công.
> [BỔ SUNG MỚI] Bổ sung check constraint chặt hơn theo lifecycle:
> - `REJECTED` phải có `Reject_Reason`
> - `APPROVED/COMPLETED` phải có người duyệt
> - `COMPLETED` phải có evidence staff nếu là nghiệp vụ có hoàn tiền
> [BỔ SUNG MỚI] Hỗ trợ trả item prescription bằng `Item_Source` + `Prescription_Order_Detail_ID`.

---

## 3. Mô hình dữ liệu tổng quan

## 3.1. `Return_Exchange` là header cấp yêu cầu

Một record trong `Return_Exchange` đại diện cho **một hồ sơ yêu cầu xử lý sau bán**.

Ví dụ:

- Hủy đơn trước xác nhận và cần refund toàn order
- Trả 1 hoặc nhiều item bị lỗi và hoàn tiền
- Gửi yêu cầu bảo hành

## 3.2. `Return_Exchange_Item` là item-level detail

Bảng này dùng khi request ở mức item (`Request_Scope = ITEM`).

Ví dụ:

- Đơn có nhiều item
- CUSTOMER chọn trả nhiều sản phẩm trong cùng một request
- Mỗi sản phẩm phải upload 1 ảnh lỗi tương ứng
- Khi đó `Return_Exchange_Item` chứa:
  - nguồn item (`Item_Source`)
  - id chi tiết tương ứng (`Order_Detail_ID` hoặc `Prescription_Order_Detail_ID`)
  - `Quantity`
  - `Item_Evidence_URL`

---

## 4. Nguyên tắc thiết kế được chốt

### 4.1. Scope theo loại yêu cầu

- `Return_Type = REFUND`
  - `Request_Scope = ORDER`
  - Không cần `Return_Exchange_Item`

- `Return_Type IN (RETURN, WARRANTY)`
  - `Request_Scope = ITEM`
  - Có thể có một hoặc nhiều dòng `Return_Exchange_Item`

### 4.2. Phạm vi của refund thủ công hiện tại

Refund thủ công sẽ lưu các trường sau ở `Return_Exchange`:

- `Refund_Amount`
- `Refund_Method`
- `Refund_Account_Number`
- `Refund_Account_Name`
- `Refund_Reference_Code`
- `Staff_Refund_Evidence_URL`
- `Processed_By`, `Processed_Date`

### 4.3. Evidence được tách rõ người upload và cấp lưu

- Evidence của CUSTOMER: lưu theo từng item ở `Return_Exchange_Item.Item_Evidence_URL`
- Evidence của STAFF: lưu ở header `Return_Exchange.Staff_Refund_Evidence_URL`

Lý do: UI cho phép trả nhiều sản phẩm trong 1 request, mỗi sản phẩm cần ảnh lỗi riêng.

---

## 5. Script SQL đề xuất

```sql
/* =========================================================
   1) HEADER TABLE: Return_Exchange
   ========================================================= */
CREATE TABLE Return_Exchange (
                                 Return_Exchange_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                                 Order_ID BIGINT NOT NULL,
                                 User_ID BIGINT NOT NULL,
                                 Return_Code NVARCHAR(50) NOT NULL UNIQUE,
                                 Request_Date DATETIME NOT NULL DEFAULT GETDATE(),

    /* Ghi chú tổng quát cho request */
                                 Request_Note NVARCHAR(1000) NULL,

    /* Lý do khách tạo request */
                                 Return_Reason NVARCHAR(500) NULL,

    /* REFUND / RETURN / WARRANTY */
                                 Return_Type NVARCHAR(20) NOT NULL,

    /* ORDER / ITEM */
                                 Request_Scope NVARCHAR(10) NOT NULL DEFAULT N'ITEM',

    /* Thông tin refund thủ công */
                                 Refund_Amount DECIMAL(15,2) NULL,
                                 Refund_Method NVARCHAR(30) NULL,
                                 Refund_Account_Number NVARCHAR(100) NULL,
                                 Refund_Account_Name NVARCHAR(100) NULL,
                                 Refund_Reference_Code NVARCHAR(100) NULL,
                                 Customer_Account_QR NVARCHAR(500) NULL,

    /* Evidence do STAFF upload sau khi chuyển khoản */
                                 Staff_Refund_Evidence_URL NVARCHAR(500) NULL,

    /* PENDING / APPROVED / REJECTED / COMPLETED */
                                 Status NVARCHAR(30) NOT NULL,

    /* Người duyệt request */
                                 Approved_By BIGINT NULL,
                                 Approved_Date DATETIME NULL,

    /* Người thực hiện hoàn tiền thủ công */
                                 Processed_By BIGINT NULL,
                                 Processed_Date DATETIME NULL,

    /* Lý do từ chối nếu REJECTED */
                                 Reject_Reason NVARCHAR(500) NULL,

                                 CONSTRAINT FK_ReturnExchange_Order
                                     FOREIGN KEY (Order_ID) REFERENCES [Order](Order_ID),

                                 CONSTRAINT FK_ReturnExchange_User
                                     FOREIGN KEY (User_ID) REFERENCES [User](User_ID),

                                 CONSTRAINT FK_ReturnExchange_ApprovedBy
                                     FOREIGN KEY (Approved_By) REFERENCES [User](User_ID),

                                 CONSTRAINT FK_ReturnExchange_ProcessedBy
                                     FOREIGN KEY (Processed_By) REFERENCES [User](User_ID),

                                 CONSTRAINT CK_ReturnExchange_Status
                                     CHECK (Status IN (
                                                       N'PENDING', N'APPROVED', N'REJECTED', N'COMPLETED'
                                         )),

                                 CONSTRAINT CK_ReturnExchange_ReturnType
                                     CHECK (Return_Type IN (
                                                            N'REFUND', N'RETURN', N'WARRANTY'
                                         )),

                                 CONSTRAINT CK_ReturnExchange_RequestScope
                                     CHECK (Request_Scope IN (N'ORDER', N'ITEM')),

    /* REFUND = ORDER, RETURN/WARRANTY = ITEM */
                                 CONSTRAINT CK_ReturnExchange_ScopeByType
                                     CHECK (
                                         (Return_Type = N'REFUND' AND Request_Scope = N'ORDER')
                                             OR
                                         (Return_Type IN (N'RETURN', N'WARRANTY') AND Request_Scope = N'ITEM')
                                         ),

                                 CONSTRAINT CK_ReturnExchange_RefundAmount
                                     CHECK (Refund_Amount IS NULL OR Refund_Amount >= 0),

    /* Nếu REJECTED thì phải có lý do từ chối */
                                 CONSTRAINT CK_ReturnExchange_RejectReason_Required
                                     CHECK (
                                         Status <> N'REJECTED'
                                             OR (Reject_Reason IS NOT NULL AND LTRIM(RTRIM(Reject_Reason)) <> N'')
                                         ),

    /* Nếu APPROVED hoặc COMPLETED thì phải có người duyệt */
                                 CONSTRAINT CK_ReturnExchange_Approval_Required
                                     CHECK (
                                         Status NOT IN (N'APPROVED', N'COMPLETED')
                                             OR (Approved_By IS NOT NULL AND Approved_Date IS NOT NULL)
                                         ),

    /* Nếu COMPLETED và là nghiệp vụ có hoàn tiền thì phải có dữ liệu hoàn tiền + evidence */
                                 CONSTRAINT CK_ReturnExchange_CompletedRefund_Required
                                     CHECK (
                                         Status <> N'COMPLETED'
                                             OR Return_Type = N'WARRANTY'
                                             OR (
                                             Refund_Amount IS NOT NULL
                                                 AND Refund_Amount > 0
                                                 AND Refund_Method IS NOT NULL
                                                 AND LTRIM(RTRIM(Refund_Method)) <> N''
                                                 AND Refund_Account_Number IS NOT NULL
                                                 AND LTRIM(RTRIM(Refund_Account_Number)) <> N''
                                                 AND Staff_Refund_Evidence_URL IS NOT NULL
                                                 AND LTRIM(RTRIM(Staff_Refund_Evidence_URL)) <> N''
                                                 AND Processed_By IS NOT NULL
                                                 AND Processed_Date IS NOT NULL
                                             )
                                         )
);
GO

/* =========================================================
   2) DETAIL TABLE: Return_Exchange_Item
   ========================================================= */
CREATE TABLE Return_Exchange_Item (
                                      Return_Exchange_Item_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                                      Return_Exchange_ID BIGINT NOT NULL,

    /* Nguồn item cần xử lý */
                                      Item_Source NVARCHAR(40) NOT NULL DEFAULT N'ORDER_DETAIL',

    /* Item thường */
                                      Order_Detail_ID BIGINT NULL,

    /* Item prescription frame+lens */
                                      Prescription_Order_Detail_ID BIGINT NULL,

                                      Quantity INT NOT NULL,

    /* Evidence do CUSTOMER upload cho từng item */
                                      Item_Evidence_URL NVARCHAR(500) NULL,

    /* Lý do riêng cho item nếu cần */
                                      Item_Reason NVARCHAR(500) NULL,

                                      Note NVARCHAR(500) NULL,

                                      CONSTRAINT FK_ReturnExchangeItem_ReturnExchange
                                          FOREIGN KEY (Return_Exchange_ID)
                                              REFERENCES Return_Exchange(Return_Exchange_ID),

                                      CONSTRAINT FK_ReturnExchangeItem_OrderDetail
                                          FOREIGN KEY (Order_Detail_ID)
                                              REFERENCES Order_Detail(Order_Detail_ID),

                                      CONSTRAINT FK_ReturnExchangeItem_PrescriptionOrderDetail
                                          FOREIGN KEY (Prescription_Order_Detail_ID)
                                              REFERENCES Prescription_Order_Detail(Prescription_Order_Detail_ID),

                                      CONSTRAINT CK_ReturnExchangeItem_ItemSource
                                          CHECK (Item_Source IN (N'ORDER_DETAIL', N'PRESCRIPTION_ORDER_DETAIL')),

                                      CONSTRAINT CK_ReturnExchangeItem_ExactlyOneDetail
                                          CHECK (
                                              (Item_Source = N'ORDER_DETAIL'
                                                  AND Order_Detail_ID IS NOT NULL
                                                  AND Prescription_Order_Detail_ID IS NULL)
                                                  OR
                                              (Item_Source = N'PRESCRIPTION_ORDER_DETAIL'
                                                  AND Order_Detail_ID IS NULL
                                                  AND Prescription_Order_Detail_ID IS NOT NULL)
                                              ),

                                      CONSTRAINT CK_ReturnExchangeItem_Quantity
                                          CHECK (Quantity > 0)
);
GO

/* =========================================================
   3) INDEXES KHUYẾN NGHỊ
   ========================================================= */
CREATE INDEX IX_ReturnExchange_Order_Status
    ON Return_Exchange(Order_ID, Status);

CREATE INDEX IX_ReturnExchange_User_Status
    ON Return_Exchange(User_ID, Status);

CREATE INDEX IX_ReturnExchange_RequestDate
    ON Return_Exchange(Request_Date);

CREATE INDEX IX_ReturnExchangeItem_OrderDetail
    ON Return_Exchange_Item(Order_Detail_ID);

CREATE INDEX IX_ReturnExchangeItem_PrescriptionOrderDetail
    ON Return_Exchange_Item(Prescription_Order_Detail_ID);
GO

/* =========================================================
   4) FILTERED UNIQUE INDEX KHUYẾN NGHỊ
   Chặn 1 order có nhiều REFUND request mở cùng lúc
   ========================================================= */
CREATE UNIQUE INDEX UX_ReturnExchange_OpenRefund_Order
ON Return_Exchange(Order_ID, Return_Type, Request_Scope)
WHERE Return_Type = N'REFUND'
  AND Request_Scope = N'ORDER'
  AND Status IN (N'PENDING', N'APPROVED');
GO
```

---

## 7. Rule cần xử lý ở service layer (DB khó enforce hoàn toàn)

### 7.1. Validate item thuộc đúng order của header

- Nếu `Item_Source = ORDER_DETAIL`:
  - `Order_Detail.Order_ID` phải bằng `Return_Exchange.Order_ID`

- Nếu `Item_Source = PRESCRIPTION_ORDER_DETAIL`:
  - `Prescription_Order_Detail -> Prescription_Order -> Order_ID`
    phải bằng `Return_Exchange.Order_ID`

### 7.2. Chống trùng request mở cho cùng item

Cần chặn trường hợp một item đã có request mở (`PENDING/APPROVED`) nhưng user tạo request mới trùng item.

### 7.3. Validate tổng quantity đã trả

Tổng số lượng:

```text
đã trả thành công + đang chờ xử lý <= số lượng đã mua
```

Rule này nên xử lý ở service/query.

---

## 8. Cách dùng schema cho từng flow

## 8.1. Flow hủy đơn trước xác nhận

`Return_Exchange`:

- `Return_Type = REFUND`
- `Request_Scope = ORDER`
- có `Refund_Amount`, `Refund_Method`, `Refund_Account_Number`
- không cần `Return_Exchange_Item`

## 8.2. Flow trả hàng và hoàn tiền do lỗi sản phẩm

`Return_Exchange`:

- `Return_Type = RETURN`
- `Request_Scope = ITEM`
- có `Customer_Evidence_URL`
- có thể có `Refund_Method`, `Refund_Account_Number` từ lúc customer tạo request
- refund chỉ hoàn tất khi staff complete

`Return_Exchange_Item`:

- mỗi dòng là 1 sản phẩm cần xử lý
- có `Item_Source`
- map tới `Order_Detail_ID` hoặc `Prescription_Order_Detail_ID`
- có `Item_Evidence_URL` cho từng sản phẩm

---

## 9. Ví dụ dữ liệu minh họa

### 9.1. Refund cấp order do hủy trước confirm

`Return_Exchange`

- `Return_Type = REFUND`
- `Request_Scope = ORDER`
- `Refund_Amount = 400000`
- `Refund_Method = BANK_TRANSFER`
- `Refund_Account_Number = 123456789`
- `Refund_Account_Name = NGUYEN VAN A`
- `Status = PENDING`

Không có dòng trong `Return_Exchange_Item`.

### 9.2. Return nhiều item lỗi sau khi giao hàng

`Return_Exchange`

- `Return_Type = RETURN`
- `Request_Scope = ITEM`
- `Status = PENDING`

`Return_Exchange_Item`

- dòng 1: `Item_Source = ORDER_DETAIL`, `Order_Detail_ID = 1001`, `Quantity = 1`, `Item_Evidence_URL = ...`
- dòng 2: `Item_Source = ORDER_DETAIL`, `Order_Detail_ID = 1003`, `Quantity = 1`, `Item_Evidence_URL = ...`
- dòng 3: `Item_Source = PRESCRIPTION_ORDER_DETAIL`, `Prescription_Order_Detail_ID = 555`, `Quantity = 1`, `Item_Evidence_URL = ...`

---

## 11. Kết luận

Schema trong tài liệu này được chốt theo nguyên tắc:

1. Đơn giản đủ để triển khai ngay.
2. Rõ vai trò từng field.
3. Evidence CUSTOMER lưu theo item để khớp UI trả nhiều sản phẩm.
4. Evidence STAFF lưu ở header để audit hoàn tiền thủ công.
5. Hỗ trợ item prescription rõ ràng, không ép map sai về `Order_Detail`

Đây là phiên bản phù hợp để dùng làm tài liệu chuẩn hóa cho backend, frontend và database team.