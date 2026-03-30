# Tài liệu tổng hợp: thay đổi cấu trúc tồn kho, nhập kho và xử lý hủy đơn hàng

## 1. Mục tiêu của việc thiết kế lại

Thiết kế cũ đang dùng table `Inventory` cho nhiều mục đích cùng lúc: vừa giống phiếu nhập kho, vừa giống nơi lưu tồn kho hiện tại. Cách này dễ gây nhầm lẫn giữa:

- số lượng đặt nhập
- số lượng thực nhận
- số lượng tồn trước
- số lượng tồn sau
- lịch sử tăng giảm kho do bán hàng, hủy đơn, hoàn hàng, điều chỉnh kho

Thiết kế mới tách rõ ba phần:

1. `Product`: lưu số lượng tồn kho hiện tại để hệ thống kiểm tra nhanh khi khách thêm giỏ hàng, thanh toán, đặt hàng.
2. `Inventory_Receipt` và `Inventory_Receipt_Detail`: lưu nghiệp vụ nhập kho của `OPERATIONS STAFF`.
3. `Inventory_Transaction`: lưu lịch sử biến động kho để audit và truy vết.

Ngoài ra, tài liệu này cũng chốt rõ rule mới nhất của bạn:

- khi CUSTOMER đặt hàng thành công thì **trừ kho ngay**
- khi CUSTOMER hủy đơn hàng thì **hoàn kho lại ngay**
- phần kiểm tra điều kiện được hủy hay không, ví dụ `Order_Status != CONFIRMED`, sẽ do nơi khác đảm nhiệm
- tài liệu này tập trung vào **cấu trúc table**, **ý nghĩa field**, **flow nhập kho**, **flow kiểm tra tồn khi mua**, và **flow update tồn khi hủy đơn**

---

## 2. Các table thay đổi và ý nghĩa

## 2.1. Table `Product`

### Vai trò
Table `Product` vẫn là table sản phẩm trung tâm, nhưng sẽ được bổ sung thêm các field để hệ thống kiểm tra nhanh tình trạng còn hàng.

### Các field mới / cần thay đổi

- `Allow_Preorder BIT NOT NULL DEFAULT 0`
  - Ý nghĩa: sản phẩm có cho phép đặt trước khi không đủ hàng hay không.
  - `0`: không cho preorder.
  - `1`: cho preorder.

- `On_Hand_Quantity INT NOT NULL DEFAULT 0`
  - Ý nghĩa: số lượng thực tế đang có trong kho.
  - Đây là số lượng vật lý hiện có.
  - Đây là field chính được dùng để cộng / trừ kho trong rule hiện tại của bạn.

- `Reserved_Quantity INT NOT NULL DEFAULT 0`
  - Ý nghĩa: số lượng đang được giữ chỗ cho các đơn hàng đã tạo nhưng chưa hoàn tất.
  - Field này hữu ích nếu sau này bạn muốn dùng flow reserve.
  - Tuy nhiên với rule hiện tại của bạn là đặt hàng thành công thì trừ kho luôn, nên field này **chưa bắt buộc tham gia vào flow đặt hàng / hủy đơn**.

- `Available_Quantity AS (On_Hand_Quantity - Reserved_Quantity)`
  - Ý nghĩa: số lượng khả dụng để bán ngay.
  - Đây là field computed.
  - Hệ thống nên ưu tiên dùng field này khi kiểm tra có đủ hàng để bán hay không.
  - Nếu hiện tại `Reserved_Quantity = 0`, thì `Available_Quantity` thực tế sẽ bằng `On_Hand_Quantity`.

- `Is_Active BIT NOT NULL DEFAULT 1`
  - Ý nghĩa: trạng thái hiển thị / hoạt động của sản phẩm.
  - Không phải trạng thái kho, mà là trạng thái sử dụng của sản phẩm.

### Ý nghĩa nghiệp vụ
- `On_Hand_Quantity`: hàng thật đang có.
- `Reserved_Quantity`: hàng đã bị giữ cho đơn nếu sau này dùng reserve flow.
- `Available_Quantity`: hàng còn có thể bán ngay.

### Gợi ý script table `Product`
```sql
CREATE TABLE Product (
    Product_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
    Product_Name NVARCHAR(255) NOT NULL,
    SKU NVARCHAR(50) UNIQUE NULL,
    Product_Type_ID BIGINT NOT NULL,
    Brand_ID BIGINT NOT NULL,

    Price DECIMAL(15,2) NOT NULL,
    Cost_Price DECIMAL(15,2) NOT NULL,

    Allow_Preorder BIT NOT NULL DEFAULT 0,

    On_Hand_Quantity INT NOT NULL DEFAULT 0,
    Reserved_Quantity INT NOT NULL DEFAULT 0,

    Available_Quantity AS (On_Hand_Quantity - Reserved_Quantity),

    Description NVARCHAR(500) NULL,
    Is_Active BIT NOT NULL DEFAULT 1,

    CONSTRAINT FK_Product_ProductType
        FOREIGN KEY (Product_Type_ID) REFERENCES Product_Type(Product_Type_ID),

    CONSTRAINT FK_Product_Brand
        FOREIGN KEY (Brand_ID) REFERENCES Brand(Brand_ID),

    CONSTRAINT CK_Product_Price CHECK (Price >= 0),
    CONSTRAINT CK_Product_CostPrice CHECK (Cost_Price >= 0),
    CONSTRAINT CK_Product_OnHandQuantity CHECK (On_Hand_Quantity >= 0),
    CONSTRAINT CK_Product_ReservedQuantity CHECK (Reserved_Quantity >= 0),
    CONSTRAINT CK_Product_AvailableQuantity CHECK (On_Hand_Quantity >= Reserved_Quantity)
);
```

---

## 2.2. Table `Inventory_Receipt`

### Vai trò
Đây là table header của phiếu nhập kho do `OPERATIONS STAFF` làm việc.

Mỗi lần tạo một phiếu nhập kho từ nhà cung cấp, hệ thống sẽ tạo một record ở đây.

### Các field

- `Inventory_Receipt_ID`
  - Khóa chính.

- `Receipt_Code`
  - Mã phiếu nhập kho, nên unique.
  - Ví dụ: `IR20260315001`.

- `Supplier_ID`
  - Nhà cung cấp của phiếu nhập.

- `Created_By`
  - User tạo phiếu nhập.
  - Thường là `OPERATIONS STAFF`.

- `Approved_By`
  - Người duyệt phiếu nhập, nếu quy trình có bước duyệt.

- `Received_By`
  - Người xác nhận nhận hàng thực tế.

- `Order_Date`
  - Ngày tạo phiếu nhập hoặc ngày đặt hàng với nhà cung cấp.

- `Received_Date`
  - Ngày nhận hàng thực tế.

- `Status`
  - Trạng thái phiếu nhập.
  - Các giá trị nên có:
    - `DRAFT`
    - `ORDERED`
    - `PARTIAL_RECEIVED`
    - `RECEIVED`
    - `CANCELED`

- `Note`
  - Ghi chú chung cho phiếu.

### Ý nghĩa nghiệp vụ
`Inventory_Receipt` chỉ là phần đầu phiếu nhập, không chứa chi tiết từng sản phẩm.

### Gợi ý script
```sql
CREATE TABLE Inventory_Receipt (
    Inventory_Receipt_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
    Receipt_Code NVARCHAR(50) NOT NULL UNIQUE,

    Supplier_ID BIGINT NOT NULL,
    Created_By BIGINT NOT NULL,
    Approved_By BIGINT NULL,
    Received_By BIGINT NULL,

    Order_Date DATETIME NOT NULL DEFAULT GETDATE(),
    Received_Date DATETIME NULL,

    Status NVARCHAR(30) NOT NULL DEFAULT N'DRAFT',
    Note NVARCHAR(500) NULL,

    CONSTRAINT FK_InventoryReceipt_Supplier
        FOREIGN KEY (Supplier_ID) REFERENCES Supplier(Supplier_ID),

    CONSTRAINT FK_InventoryReceipt_CreatedBy
        FOREIGN KEY (Created_By) REFERENCES [User](User_ID),

    CONSTRAINT FK_InventoryReceipt_ApprovedBy
        FOREIGN KEY (Approved_By) REFERENCES [User](User_ID),

    CONSTRAINT FK_InventoryReceipt_ReceivedBy
        FOREIGN KEY (Received_By) REFERENCES [User](User_ID),

    CONSTRAINT CK_InventoryReceipt_Status
        CHECK (Status IN (
            N'DRAFT',
            N'ORDERED',
            N'PARTIAL_RECEIVED',
            N'RECEIVED',
            N'CANCELED'
        ))
);
```

---

## 2.3. Table `Inventory_Receipt_Detail`

### Vai trò
Đây là table chi tiết phiếu nhập kho, mỗi record là một sản phẩm trong phiếu nhập.

### Các field

- `Inventory_Receipt_Detail_ID`
  - Khóa chính.

- `Inventory_Receipt_ID`
  - FK tới `Inventory_Receipt`.

- `Product_ID`
  - Sản phẩm được nhập kho.

- `Ordered_Quantity`
  - Số lượng mà staff đặt nhà cung cấp giao.

- `Received_Quantity`
  - Số lượng thực tế nhận đạt yêu cầu.

- `Rejected_Quantity`
  - Số lượng bị lỗi, không đạt, không nhận.

- `Unit_Cost`
  - Giá nhập tại thời điểm nhập kho.

- `Note`
  - Ghi chú cho dòng chi tiết.

### Ý nghĩa nghiệp vụ
Ví dụ:
- Staff đặt 100 cái.
- Nhà cung cấp giao 100 cái.
- Staff kiểm tra có 2 cái lỗi, chỉ nhận 98 cái.

Lúc đó:
- `Ordered_Quantity = 100`
- `Received_Quantity = 98`
- `Rejected_Quantity = 2`

### Gợi ý script
```sql
CREATE TABLE Inventory_Receipt_Detail (
    Inventory_Receipt_Detail_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
    Inventory_Receipt_ID BIGINT NOT NULL,
    Product_ID BIGINT NOT NULL,

    Ordered_Quantity INT NOT NULL,
    Received_Quantity INT NOT NULL DEFAULT 0,
    Rejected_Quantity INT NOT NULL DEFAULT 0,

    Unit_Cost DECIMAL(15,2) NOT NULL,
    Note NVARCHAR(500) NULL,

    CONSTRAINT FK_InventoryReceiptDetail_Receipt
        FOREIGN KEY (Inventory_Receipt_ID) REFERENCES Inventory_Receipt(Inventory_Receipt_ID),

    CONSTRAINT FK_InventoryReceiptDetail_Product
        FOREIGN KEY (Product_ID) REFERENCES Product(Product_ID),

    CONSTRAINT UQ_InventoryReceiptDetail_Receipt_Product
        UNIQUE (Inventory_Receipt_ID, Product_ID),

    CONSTRAINT CK_InventoryReceiptDetail_OrderedQty
        CHECK (Ordered_Quantity > 0),

    CONSTRAINT CK_InventoryReceiptDetail_ReceivedQty
        CHECK (Received_Quantity >= 0),

    CONSTRAINT CK_InventoryReceiptDetail_RejectedQty
        CHECK (Rejected_Quantity >= 0),

    CONSTRAINT CK_InventoryReceiptDetail_UnitCost
        CHECK (Unit_Cost >= 0),

    CONSTRAINT CK_InventoryReceiptDetail_QuantityLogic
        CHECK (Received_Quantity + Rejected_Quantity <= Ordered_Quantity)
);
```

---

## 2.4. Table `Inventory_Transaction`

### Vai trò
Đây là table quan trọng nhất để truy vết mọi biến động kho.

Mỗi lần kho tăng hoặc giảm, phải tạo một record ở table này.

### Các field

- `Inventory_Transaction_ID`
  - Khóa chính.

- `Product_ID`
  - Sản phẩm bị thay đổi tồn.

- `Transaction_Type`
  - Loại biến động kho.
  - Nên có:
    - `RECEIPT_IN`
    - `SALE_OUT`
    - `ORDER_CANCEL_IN`
    - `CUSTOMER_RETURN_IN`
    - `RETURN_TO_SUPPLIER_OUT`
    - `DAMAGE_OUT`
    - `ADJUSTMENT_IN`
    - `ADJUSTMENT_OUT`
    - `RESERVE`
    - `RELEASE_RESERVE`

- `Quantity_Change`
  - Số lượng thay đổi.
  - Có thể dương hoặc âm.
  - Ví dụ:
    - nhập kho `+98`
    - bán hàng `-2`
    - hủy đơn hoàn kho `+2`

- `Quantity_Before`
  - Số tồn trước biến động.

- `Quantity_After`
  - Số tồn sau biến động.

- `Reference_Type`
  - Kiểu chứng từ tham chiếu.
  - Ví dụ:
    - `RECEIPT`
    - `ORDER`
    - `RETURN`
    - `ADJUSTMENT`
    - `MANUAL`

- `Reference_ID`
  - ID chứng từ tham chiếu.

- `Order_ID`
  - Nếu biến động liên quan order.

- `Order_Detail_ID`
  - Nếu biến động liên quan dòng sản phẩm trong order.

- `Inventory_Receipt_ID`
  - Nếu biến động liên quan phiếu nhập.

- `Performed_By`
  - Người thực hiện thao tác.

- `Performed_At`
  - Thời điểm thao tác.

- `Note`
  - Ghi chú.

### Ý nghĩa nghiệp vụ
Table này là nơi audit toàn bộ lịch sử kho. Nếu sau này cần kiểm tra vì sao tồn tăng giảm, chỉ cần truy table này.

### Gợi ý script
```sql
CREATE TABLE Inventory_Transaction (
    Inventory_Transaction_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
    Product_ID BIGINT NOT NULL,

    Transaction_Type NVARCHAR(30) NOT NULL,
    Quantity_Change INT NOT NULL,
    Quantity_Before INT NOT NULL,
    Quantity_After INT NOT NULL,

    Reference_Type NVARCHAR(30) NULL,
    Reference_ID BIGINT NULL,

    Order_ID BIGINT NULL,
    Order_Detail_ID BIGINT NULL,
    Inventory_Receipt_ID BIGINT NULL,

    Performed_By BIGINT NOT NULL,
    Performed_At DATETIME NOT NULL DEFAULT GETDATE(),
    Note NVARCHAR(500) NULL,

    CONSTRAINT FK_InventoryTransaction_Product
        FOREIGN KEY (Product_ID) REFERENCES Product(Product_ID),

    CONSTRAINT FK_InventoryTransaction_Order
        FOREIGN KEY (Order_ID) REFERENCES [Order](Order_ID),

    CONSTRAINT FK_InventoryTransaction_OrderDetail
        FOREIGN KEY (Order_Detail_ID) REFERENCES Order_Detail(Order_Detail_ID),

    CONSTRAINT FK_InventoryTransaction_Receipt
        FOREIGN KEY (Inventory_Receipt_ID) REFERENCES Inventory_Receipt(Inventory_Receipt_ID),

    CONSTRAINT FK_InventoryTransaction_PerformedBy
        FOREIGN KEY (Performed_By) REFERENCES [User](User_ID),

    CONSTRAINT CK_InventoryTransaction_Type
        CHECK (Transaction_Type IN (
            N'RECEIPT_IN',
            N'SALE_OUT',
            N'ORDER_CANCEL_IN',
            N'CUSTOMER_RETURN_IN',
            N'RETURN_TO_SUPPLIER_OUT',
            N'DAMAGE_OUT',
            N'ADJUSTMENT_IN',
            N'ADJUSTMENT_OUT',
            N'RESERVE',
            N'RELEASE_RESERVE'
        )),

    CONSTRAINT CK_InventoryTransaction_QtyBefore CHECK (Quantity_Before >= 0),
    CONSTRAINT CK_InventoryTransaction_QtyAfter CHECK (Quantity_After >= 0),
    CONSTRAINT CK_InventoryTransaction_QtyChange_NotZero CHECK (Quantity_Change <> 0),
    CONSTRAINT CK_InventoryTransaction_Balance CHECK (Quantity_After = Quantity_Before + Quantity_Change)
);
```

---

## 3. OPERATIONS STAFF sẽ làm việc với table nào?

Khi nhập kho, `OPERATIONS STAFF` chủ yếu làm việc với 3 table sau:

1. `Inventory_Receipt`
2. `Inventory_Receipt_Detail`
3. `Inventory_Transaction`

Ngoài ra, khi nhận hàng thực tế thì còn cập nhật `Product.On_Hand_Quantity`.

---

## 3.1. Luồng INSERT khi tạo phiếu nhập kho

### Bước 1: Tạo header phiếu nhập
INSERT vào `Inventory_Receipt`.

Ví dụ:
```sql
INSERT INTO Inventory_Receipt
(
    Receipt_Code,
    Supplier_ID,
    Created_By,
    Order_Date,
    Status,
    Note
)
VALUES
(
    N'IR20260315001',
    1,
    4,
    GETDATE(),
    N'ORDERED',
    N'Phiếu nhập kho tháng 03'
);
```

### Bước 2: Tạo chi tiết phiếu nhập
INSERT vào `Inventory_Receipt_Detail`.

Ví dụ:
```sql
INSERT INTO Inventory_Receipt_Detail
(
    Inventory_Receipt_ID,
    Product_ID,
    Ordered_Quantity,
    Unit_Cost,
    Note
)
VALUES
(1, 1, 100, 3200000, N'Đặt nhập gọng Ray-Ban'),
(1, 7, 50, 2100000, N'Đặt nhập tròng Essilor');
```

### Ý nghĩa
- Lúc này staff mới chỉ tạo phiếu nhập, chưa có hàng thực tế trong kho.
- Do đó:
  - chưa update `Product.On_Hand_Quantity`
  - chưa insert `Inventory_Transaction`

---

## 3.2. Luồng UPDATE khi nhận hàng thực tế

Khi nhà cung cấp giao hàng, staff kiểm tra thực tế và cập nhật:

- `Inventory_Receipt.Status`
- `Inventory_Receipt.Received_Date`
- `Inventory_Receipt.Received_By`
- `Inventory_Receipt_Detail.Received_Quantity`
- `Inventory_Receipt_Detail.Rejected_Quantity`

Ví dụ:
```sql
UPDATE Inventory_Receipt
SET
    Status = N'RECEIVED',
    Received_Date = GETDATE(),
    Received_By = 4
WHERE Inventory_Receipt_ID = 1;
```

```sql
UPDATE Inventory_Receipt_Detail
SET
    Received_Quantity = 98,
    Rejected_Quantity = 2
WHERE Inventory_Receipt_ID = 1
  AND Product_ID = 1;
```

Sau đó hệ thống phải:

### Bước 1: đọc số tồn hiện tại ở `Product`
Ví dụ:
- `Product.On_Hand_Quantity = 20`

### Bước 2: cộng thêm hàng thực nhận
- `20 + 98 = 118`

### Bước 3: update lại `Product`
```sql
UPDATE Product
SET On_Hand_Quantity = On_Hand_Quantity + 98
WHERE Product_ID = 1;
```

### Bước 4: ghi log vào `Inventory_Transaction`
```sql
INSERT INTO Inventory_Transaction
(
    Product_ID,
    Transaction_Type,
    Quantity_Change,
    Quantity_Before,
    Quantity_After,
    Reference_Type,
    Reference_ID,
    Inventory_Receipt_ID,
    Performed_By,
    Note
)
VALUES
(
    1,
    N'RECEIPT_IN',
    98,
    20,
    118,
    N'RECEIPT',
    1,
    1,
    4,
    N'Nhập kho từ phiếu IR20260315001'
);
```

---

## 3.3. DELETE với phiếu nhập kho phải làm như thế nào?

### Không nên cho DELETE cứng khi phiếu đã nhận hàng
Nếu phiếu đã ở trạng thái `RECEIVED` và đã cập nhật tồn kho rồi, không nên xóa cứng (`DELETE`) vì sẽ làm mất lịch sử nghiệp vụ.

### Chỉ nên DELETE cứng trong trường hợp
- phiếu ở trạng thái `DRAFT`
- chưa có `Received_Date`
- chưa phát sinh `Inventory_Transaction`
- chưa cộng tồn vào `Product`

Ví dụ:
```sql
DELETE FROM Inventory_Receipt_Detail
WHERE Inventory_Receipt_ID = 1;

DELETE FROM Inventory_Receipt
WHERE Inventory_Receipt_ID = 1
  AND Status = N'DRAFT';
```

### Chuẩn hơn
Khi phiếu đã phát sinh nghiệp vụ thật, chỉ nên:
- đổi `Status = CANCELED`
- không xóa dữ liệu

Ví dụ:
```sql
UPDATE Inventory_Receipt
SET Status = N'CANCELED'
WHERE Inventory_Receipt_ID = 1
  AND Status IN (N'DRAFT', N'ORDERED');
```

---

## 4. Ở trang ProductDetail kiểm tra tồn kho và preorder như thế nào?

Ở trang `ProductDetail`, khi user bấm **Thêm vào giỏ hàng**, backend cần kiểm tra các field của `Product`:

- `Available_Quantity`
- `Allow_Preorder`
- `Status`

## 4.1. Logic nghiệp vụ

### Trường hợp 1: sản phẩm còn hàng
Nếu:
- `Is_Active = 1`
- `Available_Quantity >= requestedQty`

thì cho thêm vào giỏ hàng bình thường.

### Trường hợp 2: sản phẩm không còn đủ hàng nhưng cho preorder
Nếu:
- `Is_Active = 1`
- `Available_Quantity < requestedQty`
- `Allow_Preorder = 1`

thì vẫn cho thêm vào giỏ hàng, nhưng frontend nên hiển thị rõ:
- đây là sản phẩm preorder
- thời gian giao có thể lâu hơn
- khi checkout hệ thống có thể tạo `PRE_ORDER` hoặc `MIX_ORDER`

### Trường hợp 3: sản phẩm hết hàng và không cho preorder
Nếu:
- `Available_Quantity < requestedQty`
- `Allow_Preorder = 0`

thì không cho thêm vào giỏ hàng.

---

## 4.2. Điều kiện kiểm tra nên dùng

Không nên dùng:
```text
Product.Quantity > 0 AND Allow_Preorder = true
```

Vì điều kiện này sai với trường hợp hàng hết nhưng được preorder.

Nên dùng:
```text
(Available_Quantity >= requestedQty)
OR
(Available_Quantity < requestedQty AND Allow_Preorder = 1)
```

---

## 4.3. Gợi ý pseudocode backend cho Add To Cart

```java
Product product = productRepository.findById(productId)
        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

if (!product.getStatus()) {
    throw new AppException(ErrorCode.PRODUCT_INACTIVE);
}

if (Boolean.TRUE.equals(product.getTrackInventory())) {
    int availableQty = product.getAvailableQuantity();
    boolean allowPreorder = Boolean.TRUE.equals(product.getAllowPreorder());

    if (availableQty >= requestedQty) {
        // cho add cart bình thường
    } else if (allowPreorder) {
        // cho add cart dưới dạng preorder
    } else {
        throw new AppException(ErrorCode.OUT_OF_STOCK);
    }
}
```

---

## 4.4. Gợi ý hiển thị trên ProductDetail

Frontend nên hiển thị một trong ba trạng thái:

- `Còn hàng`
  - khi `Available_Quantity > 0`

- `Hết hàng nhưng cho đặt trước`
  - khi `Available_Quantity <= 0 AND Allow_Preorder = 1`

- `Hết hàng`
  - khi `Available_Quantity <= 0 AND Allow_Preorder = 0`

---

## 5. Ở trang Cart kiểm tra tồn kho và preorder như thế nào trước khi bấm thanh toán?

Ở trang `Cart`, trước khi cho user bấm **Thanh toán** và chuyển qua `ConfirmPage`, backend phải re-check toàn bộ giỏ hàng.

Lý do:
- số lượng tồn kho có thể đã thay đổi sau khi sản phẩm được add vào cart
- không được tin dữ liệu cũ ở frontend

---

## 5.1. Tại sao phải kiểm tra lại ở Cart?

Ví dụ:
- 10:00 khách A thêm sản phẩm vào cart
- 10:05 khách B mua hết sản phẩm
- 10:10 khách A mới bấm thanh toán

Nếu không re-check ở cart / checkout thì sẽ bị bán vượt tồn.

---

## 5.2. Logic kiểm tra từng cart item

Với mỗi `Cart_Item`, backend cần xác định `Product_ID` thực tế cần trừ kho.

### Trường hợp:
- `Product_ID` có giá trị: dùng trực tiếp
- `Frame_ID`: phải join sang `Frame` để lấy `Frame.Product_ID`
- `Lens_ID`: phải join sang `Lens` để lấy `Lens.Product_ID`

Sau đó kiểm tra product tương ứng bằng logic:

```text
(Available_Quantity >= requestedQty)
OR
(Available_Quantity < requestedQty AND Allow_Preorder = 1)
```

---

## 5.3. Kết quả xử lý ở Cart

### Nếu tất cả item đều hợp lệ
Cho phép bấm thanh toán, chuyển sang `ConfirmPage`.

### Nếu có item không hợp lệ
Không cho thanh toán và trả về danh sách item lỗi, ví dụ:
- sản phẩm đã hết hàng
- số lượng yêu cầu vượt số lượng khả dụng
- sản phẩm không cho preorder

Frontend nên hiển thị rõ item nào bị lỗi để user chỉnh lại giỏ hàng.

---

## 5.4. Gợi ý pseudocode backend khi bấm Checkout

```
List<CartItem> cartItems = cartItemRepository.findByCartId(cartId);

for (CartItem item : cartItems) {
    Product product = resolveProductFromCartItem(item);

    int requestedQty = item.getQuantity() != null && item.getQuantity() > 0
            ? item.getQuantity()
            : 1;

    int availableQty = product.getAvailableQuantity();
    boolean allowPreorder = Boolean.TRUE.equals(product.getAllowPreorder());

    if (availableQty >= requestedQty) {
        // item hợp lệ
        continue;
    }

    if (allowPreorder) {
        // item hợp lệ theo dạng preorder
        continue;
    }

    throw new AppException(ErrorCode.CART_ITEM_OUT_OF_STOCK);
}
```

---

## 5.5. Phân loại order ở ConfirmPage

Sau khi kiểm tra toàn bộ cart, hệ thống có thể phân loại:

- tất cả item đủ hàng:
  - `DIRECT_ORDER`

- tất cả item thiếu hàng nhưng được preorder:
  - `PRE_ORDER`

- có item đủ hàng và có item preorder:
  - `MIX_ORDER`

Điều này rất phù hợp với schema `Order_Type` hiện tại của bạn.

---

## 6. Rule chốt hiện tại về tồn kho khi đặt hàng

Ở giai đoạn trước có thể cân nhắc flow `Reserved_Quantity`, nhưng **rule mới nhất mà bạn đã chốt** là:

- khi CUSTOMER đặt hàng thành công thì **giảm luôn các field cần thiết trong table `Product`**
- **không cần chờ `Order_Status = CONFIRMED`**
- **không cần dùng reserve flow cho bước đặt hàng**

Vì vậy trong flow hiện tại:

### Khi đặt hàng thành công
- giảm `Product.On_Hand_Quantity`
- `Available_Quantity` tự giảm theo nếu là computed column
- `Reserved_Quantity` không cần dùng trong flow này
- ghi `Inventory_Transaction` loại `SALE_OUT`

### Ví dụ
Ban đầu:
- `On_Hand_Quantity = 10`
- `Reserved_Quantity = 0`
- `Available_Quantity = 10`

Customer đặt mua 2 sản phẩm.

Sau khi đặt hàng thành công:
- `On_Hand_Quantity = 8`
- `Reserved_Quantity = 0`
- `Available_Quantity = 8`

---

## 7. Flow chi tiết khi CUSTOMER hủy đơn hàng

Phần này là phần quan trọng nhất theo rule mới của bạn.

### Rule đã chốt
- CUSTOMER có thể tự hủy đơn khi điều kiện nghiệp vụ bên ngoài cho phép
- việc kiểm tra điều kiện như `Order_Status != CONFIRMED` sẽ do chỗ khác xử lý
- tài liệu này chỉ nói rõ: **nếu đã đi vào flow hủy đơn**, thì hệ thống phải update field nào

---

## 7.1. Khi CUSTOMER hủy đơn thì cần update các table nào?

Khi customer hủy đơn, về mặt tồn kho bạn nên update:

1. `Product`
2. `Inventory_Transaction`
3. `Order`

Nếu hệ thống của bạn có payment, invoice, shipping thì có thể cần update thêm các table đó, nhưng trong phạm vi tài liệu này, phần tồn kho chắc chắn liên quan đến:

- `Product`
- `Inventory_Transaction`
- `Order`

---

## 7.2. Khi CUSTOMER hủy đơn thì cần update field nào trong table `Product`?

### `On_Hand_Quantity`
Đây là field quan trọng nhất.

Vì trước đó khi đặt hàng thành công bạn đã **trừ kho thật ngay lập tức**, nên khi hủy đơn phải **cộng lại đúng số lượng đã mua**:

```text
On_Hand_Quantity = On_Hand_Quantity + canceledQty
```

### `Available_Quantity`
Nếu `Available_Quantity` là computed column:

```text
Available_Quantity = On_Hand_Quantity - Reserved_Quantity
```

thì bạn **không cần update trực tiếp**.  
Khi `On_Hand_Quantity` tăng, `Available_Quantity` sẽ tự tăng theo.

### `Reserved_Quantity`
Với rule hiện tại:
- đặt hàng thành công là trừ thẳng kho
- không dùng reserve flow

thì khi customer hủy đơn:
- **không cần update `Reserved_Quantity`**

### Kết luận ngắn
Khi customer hủy đơn:
- update `Product.On_Hand_Quantity`
- không update `Reserved_Quantity`
- `Available_Quantity` tự thay đổi nếu là computed

---

## 7.3. Flow xử lý chi tiết khi CUSTOMER hủy đơn

Giả sử:

- Order có nhiều dòng hàng trong `Order_Detail`
- mỗi dòng tương ứng một sản phẩm và một số lượng cụ thể

Khi customer hủy đơn, backend nên xử lý theo thứ tự sau:

### Bước 1: Lấy order cần hủy
Từ `Order_ID`, lấy ra order tương ứng.

### Bước 2: Lấy toàn bộ `Order_Detail` của order
Ví dụ order có:
- Product A, qty = 2
- Product B, qty = 1

### Bước 3: Với từng `Order_Detail`, hoàn lại tồn kho vào `Product`

#### Với Product A
```text
On_Hand_Quantity = On_Hand_Quantity + 2
```

#### Với Product B
```text
On_Hand_Quantity = On_Hand_Quantity + 1
```

### Bước 4: Ghi log `Inventory_Transaction`
Với mỗi product được hoàn kho, insert một record có:

- `Transaction_Type = ORDER_CANCEL_IN`

### Bước 5: Update trạng thái order
Thông thường:
- `Order.Order_Status = CANCELED`

---

## 7.4. SQL minh họa khi CUSTOMER hủy đơn với 1 sản phẩm

Giả sử:
- `Order_ID = 1001`
- trong order có `Product_ID = 1`, quantity = 2
- hiện tại `Product.On_Hand_Quantity = 8`

Khi hủy đơn:

### Update lại `Product`
```sql
UPDATE Product
SET On_Hand_Quantity = On_Hand_Quantity + 2
WHERE Product_ID = 1;
```

Sau update:
- `On_Hand_Quantity = 10`

### Ghi log vào `Inventory_Transaction`
```sql
INSERT INTO Inventory_Transaction
(
    Product_ID,
    Transaction_Type,
    Quantity_Change,
    Quantity_Before,
    Quantity_After,
    Reference_Type,
    Reference_ID,
    Order_ID,
    Performed_By,
    Note
)
VALUES
(
    1,
    N'ORDER_CANCEL_IN',
    2,
    8,
    10,
    N'ORDER',
    1001,
    1001,
    1,
    N'Customer hủy đơn, hoàn lại tồn kho'
);
```

### Update trạng thái order
```sql
UPDATE [Order]
SET Order_Status = N'CANCELED'
WHERE Order_ID = 1001;
```

---

## 7.5. SQL minh họa khi CUSTOMER hủy đơn với nhiều sản phẩm

Giả sử order có:
- Product_ID = 1, qty = 2
- Product_ID = 7, qty = 1

Khi hủy đơn:

### Product 1
```sql
UPDATE Product
SET On_Hand_Quantity = On_Hand_Quantity + 2
WHERE Product_ID = 1;
```

### Product 7
```sql
UPDATE Product
SET On_Hand_Quantity = On_Hand_Quantity + 1
WHERE Product_ID = 7;
```

Sau đó insert 2 dòng vào `Inventory_Transaction`, mỗi dòng ứng với một product.

---

## 7.6. Nếu order có combo Frame + Lens thì hoàn kho như thế nào?

Trong dự án của bạn, một cart item / order có thể là:

- chỉ 1 `Product_ID`
- hoặc `Frame_ID`
- hoặc `Lens_ID`
- hoặc combo frame + lens

Nhưng **tồn kho được quản lý ở cấp `Product`**.

Do đó khi hủy đơn, bạn phải hoàn lại kho theo **`Product_ID` gốc**.

### Cách hiểu đúng
- Nếu `Order_Detail` đang lưu trực tiếp `Product_ID` thì dùng luôn
- Nếu logic liên quan tới `Frame_ID`
  - phải map sang `Frame.Product_ID`
- Nếu logic liên quan `Lens_ID`
  - phải map sang `Lens.Product_ID`

### Rule quan trọng
Hủy đơn là **hoàn lại số lượng cho đúng product thực sự đã bị trừ kho lúc đặt hàng**.

---

## 7.7. Hủy đơn có cần update `Inventory_Receipt` không?

**Không.**

Vì:
- `Inventory_Receipt`
- `Inventory_Receipt_Detail`

chỉ phục vụ nghiệp vụ nhập kho của `OPERATIONS STAFF`.

Customer hủy đơn là nghiệp vụ:
- hoàn lại tồn kho bán hàng

Do đó khi customer hủy đơn:
- không update `Inventory_Receipt`
- không update `Inventory_Receipt_Detail`
- chỉ update:
  - `Product`
  - `Inventory_Transaction`
  - `Order`

---

## 7.8. Pseudocode backend khi CUSTOMER hủy đơn

```java
Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(orderId);

for (OrderDetail detail : orderDetails) {
    Product product = productRepository.findById(detail.getProduct().getProductId())
            .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

    int cancelQty = detail.getQuantity() == null ? 0 : detail.getQuantity();
    int beforeQty = product.getOnHandQuantity();
    int afterQty = beforeQty + cancelQty;

    product.setOnHandQuantity(afterQty);

    inventoryTransactionRepository.save(
        InventoryTransaction.builder()
            .product(product)
            .transactionType("ORDER_CANCEL_IN")
            .quantityChange(cancelQty)
            .quantityBefore(beforeQty)
            .quantityAfter(afterQty)
            .referenceType("ORDER")
            .referenceId(order.getOrderId())
            .order(order)
            .performedBy(order.getUser())
            .note("Customer hủy đơn, hoàn lại tồn kho")
            .build()
    );
}

order.setOrderStatus("CANCELED");
```

---

## 7.9. Các lưu ý quan trọng khi code flow hủy đơn

### Phải chạy trong cùng một database transaction
Toàn bộ flow hủy đơn nên ở trong một transaction DB.

Lý do:
- tránh trường hợp đã cộng lại kho cho `Product`
- nhưng chưa update `Order`
- hoặc chưa insert `Inventory_Transaction`

Chuẩn nhất:
- update kho
- insert log kho
- update order
- commit cùng lúc

### Không được cộng kho 2 lần
Nếu order đã hủy rồi thì không được cho chạy lại flow hoàn kho thêm lần nữa.

Dù phần check điều kiện hủy sẽ do nơi khác xử lý, service hoàn kho vẫn nên cẩn thận để tránh duplicate cancel.

### Nếu một order có nhiều dòng cùng `Product_ID`
Ví dụ:
- dòng 1: Product_ID = 1, qty = 1
- dòng 2: Product_ID = 1, qty = 2

thì khi hủy đơn có 2 cách:

#### Cách 1: xử lý từng dòng
- cộng 1
- rồi cộng 2

#### Cách 2: gom nhóm theo `Product_ID`
- tổng quantity = 3
- cộng 1 lần

### Khuyến nghị
Nên gom nhóm theo `Product_ID` trước rồi update:
- giảm số lần query/update DB
- dễ tối ưu hơn
- dễ lock row chính xác hơn

---

## 8. Tóm tắt rất ngắn gọn đúng theo rule hiện tại

### Khi đặt hàng thành công
- trừ luôn `Product.On_Hand_Quantity`
- insert `Inventory_Transaction` loại `SALE_OUT`

### Khi CUSTOMER hủy đơn
- cộng lại `Product.On_Hand_Quantity`
- không cần update `Reserved_Quantity`
- `Available_Quantity` tự tăng nếu là computed column
- insert `Inventory_Transaction` loại `ORDER_CANCEL_IN`
- update `Order.Order_Status = CANCELED`

---

## 9. Kết luận cuối cùng

Nếu bạn **không dùng flow reserve** và đã chốt rằng **đặt hàng thành công là trừ kho luôn**, thì cấu trúc và cách làm việc với các table nên hiểu như sau:

### Table dùng để kiểm tra số lượng và tình trạng còn hàng
- `Product`
  - `On_Hand_Quantity`
  - `Available_Quantity`
  - `Allow_Preorder`
  - `Track_Inventory`

### Table dùng cho nghiệp vụ nhập kho
- `Inventory_Receipt`
- `Inventory_Receipt_Detail`

### Table dùng để ghi lịch sử tăng giảm kho
- `Inventory_Transaction`

### Khi customer hủy đơn, table cần update
- `Product`
- `Inventory_Transaction`
- `Order`

### Field quan trọng nhất cần update trong `Product`
```text
On_Hand_Quantity = On_Hand_Quantity + canceledQty
```

### Field không cần update trong flow hủy đơn hiện tại
- `Reserved_Quantity`

### Field tự đổi nếu là computed
- `Available_Quantity`

Đó là toàn bộ tài liệu tổng hợp đầy đủ theo đúng rule mới nhất mà bạn vừa chốt.
