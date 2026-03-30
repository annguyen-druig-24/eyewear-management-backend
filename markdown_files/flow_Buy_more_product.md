# Inventory Receipt API Flow and Testcase Specification

## 1) Muc tieu tai lieu

Tai lieu nay mo ta day du luong xu ly cho nhom API nhap kho trong InventoryReceiptController.
Pham vi bao gom:
- Luong nghiep vu tong the.
- Luong chi tiet cho tung API.
- Quy tac validate va rang buoc du lieu.
- Tac dong DB va logging kho.
- Test case API voi so luong toi thieu 2 test cho moi API.

Trong tai lieu nay, bo endpoint dang xet:
- GET /api/inventory-receipts/products/search?supplierId={id}
- POST /api/inventory-receipts
- GET /api/inventory-receipts
- GET /api/inventory-receipts/{id}
- PUT /api/inventory-receipts/{id}/receive

Tat ca endpoint deu co phan quyen:
- ROLE_SALES STAFF
- ROLE_ADMIN
- ROLE_MANAGER
- ROLE_OPERATIONS STAFF

Neu user khong co quyen:
- He thong tra ve HTTP 403.
- ApiResponse code thong thuong la 1007.

Neu user khong dang nhap hoac token khong hop le:
- He thong co the tra ve HTTP 401 tu security layer.
- Hoac AppException UNAUTHENTICATED tai service layer.

## 2) Nguon code tham chieu

- Controller: src/main/java/com/swp391/eyewear_management_backend/controller/InventoryReceiptController.java
- Service: src/main/java/com/swp391/eyewear_management_backend/service/impl/InventoryReceiptServiceImpl.java
- Interface service: src/main/java/com/swp391/eyewear_management_backend/service/InventoryReceiptService.java
- DTO request:
	- InventoryReceiptRequest
	- ReceiptDetailRequest
	- InventoryReceiptReceiveRequest
	- InventoryReceiptReceiveDetailRequest
- DTO response:
	- ProductOfSupplierResponse
	- InventoryReceiptResponse
	- InventoryReceiptConformResponse
	- InventoryReceiptDetailResponse
- Mapper: InventoryReceiptMapper
- Exception handler: GlobalExceptionHandler

## 3) Tong quan luong nghiep vu

Luong tong quan khi nhap kho tu nha cung cap:
1. Nguoi dung tim danh sach san pham thuoc 1 supplier.
2. Nguoi dung tao phieu nhap kho voi danh sach dong hang, don gia, VAT, thanh tien.
3. He thong tao phieu voi trang thai Pending Verification.
4. Nguoi dung xem danh sach phieu da tao.
5. Nguoi dung mo chi tiet phieu de doi chieu.
6. Khi hang den, nguoi dung xac nhan so luong thuc nhan tren tung dong.
7. He thong cap nhat:
	 - Received quantity.
	 - Rejected quantity.
	 - Actual total amount.
	 - So luong ton (on hand) trong Product.
	 - Inventory log.
	 - InventoryTransaction log.
8. Trang thai phieu chuyen thanh:
	 - Fully Entered neu tat ca dong nhan du.
	 - Partially Entered neu co it nhat 1 dong nhan thieu.

## 4) Quy uoc status trong luong

Trong code service co cac status duoc su dung:
- Pending Verification
- Partially Entered
- Fully Entered

Rang buoc luong receive:
- Chi cho phep receive neu status hien tai la Pending Verification hoac Partially Entered.
- Neu status khac, service throw RuntimeException.

## 5) Dinh dang loi va ma loi

Loi AppException:
- Duoc map boi GlobalExceptionHandler.handleAppException.
- Tra ve HTTP status theo ErrorCode.

Loi RuntimeException thong thuong trong service nhap kho:
- Duoc bat boi catch-all Exception handler.
- Tra ve HTTP 500.
- code = 9999.
- message = noi dung throw RuntimeException.

Loi validate @Valid:
- Controller nay hien tai chua dat @Valid cho request body.
- Vi vay nhieu loi du lieu se roi vao RuntimeException/NPE thay vi 400 validation.

## 6) Chi tiet API 1

### 6.1 API

Method: GET
Path: /api/inventory-receipts/products/search
Query param: supplierId
Service: getProductsBySupplierId
Response: List<ProductOfSupplierResponse>

### 6.2 Luong xu ly chi tiet

1. Security check role tai controller.
2. Controller doc query param supplierId.
3. Goi receiptService.getProductsBySupplierId(supplierId).
4. Service goi productRepo.findProductsBySupplierId(supplierId).
5. Mapper ProductMapper map Product -> ProductOfSupplierResponse.
6. Service tra list cho controller.
7. Controller tra HTTP 200 + JSON array.

### 6.3 Dau vao va dau ra

Dau vao:
- supplierId bat buoc.

Dau ra moi item ProductOfSupplierResponse co:
- productID
- productName
- SKU
- productTypeId
- productTypeName
- brandId
- brandName
- price
- costPrice
- frameId
- lensId
- contactLensId

### 6.4 Diem can luu y

- API nay khong throw AppException neu supplier khong ton tai.
- Neu supplierId khong co san pham, thong thuong tra list rong.
- Query phu thuoc implementation repository.

## 7) Chi tiet API 2

### 7.1 API

Method: POST
Path: /api/inventory-receipts
Service: createInventoryReceipt
Request body: InventoryReceiptRequest
Response: InventoryReceiptResponse

### 7.2 Luong xu ly chi tiet

1. Security check role tai controller.
2. Controller nhan request body.
3. Service tim Supplier theo supplierId.
4. Neu khong tim thay supplier -> AppException NOT_FOUND_SUPPLIER.
5. Service lay current user tu SecurityContext.
6. Tao InventoryReceipt master:
	 - receiptCode unique dang IR-yyMMdd-XXXX.
	 - supplier.
	 - createdBy.
	 - orderDate = now.
	 - receivedDate = null.
	 - status = Pending Verification.
	 - note = request.note.
	 - totalAmount = 0.
7. Save receipt master de lay ID.
8. Toi uu hoa preload du lieu:
	 - validProductIdsForSupplier = tat ca product id cua supplier.
	 - requestedProductMap = map productId -> Product tu danh sach request.
9. Lap qua tung ReceiptDetailRequest:
	 - Kiem tra product ton tai trong requestedProductMap.
	 - Kiem tra product co thuoc supplier khong.
	 - Kiem tra unitCost tu request khop costPrice trong DB.
	 - Tinh expectedDetailTotal = unitCost * quantity + VAT.
	 - Kiem tra expectedDetailTotal khop totalPrice gui len.
	 - Tao InventoryReceiptDetail:
		 - orderedQuantity = quantity.
		 - receivedQuantity = 0.
		 - rejectedQuantity = 0.
		 - unitCost.
		 - note.
		 - totalPrice.
	 - Cong don grandTotal.
10. Cap nhat receipt.totalAmount = grandTotal.
11. Save receipt.
12. Save all details.
13. Map InventoryReceiptResponse va tra ket qua.

### 7.3 Rang buoc du lieu quan trong

- supplierId phai ton tai.
- details nen khong null va co it nhat 1 item.
- Moi productId trong details phai ton tai.
- Product phai thuoc supplier dang duoc chon.
- unitCost phai bang costPrice he thong.
- totalPrice tung dong phai khop cong thuc:
	- totalBeforeTax = unitCost * quantity
	- vatAmount = totalBeforeTax * vatRate / 100
	- expectedDetailTotal = totalBeforeTax + vatAmount

### 7.4 Tac dong du lieu

- Them 1 ban ghi Inventory_Receipt.
- Them N ban ghi Inventory_Receipt_Detail.
- Chua cap nhat ton kho Product o API nay.

## 8) Chi tiet API 3

### 8.1 API

Method: GET
Path: /api/inventory-receipts
Service: getAllReceipts
Response: List<InventoryReceiptResponse>

### 8.2 Luong xu ly

1. Security check role.
2. Service lay tat ca receipt tu receiptRepo.findAll().
3. Mapper map tung receipt sang InventoryReceiptResponse.
4. Controller tra HTTP 200 voi list.

### 8.3 Truong response chinh

- inventoryReceiptId
- receiptCode
- supplierId
- supplierName
- createdById
- createdByName
- orderDate
- receivedDate
- status
- totalAmount
- note

### 8.4 Diem can luu y

- API nay chua phan trang.
- Khi du lieu lon co the can bo sung paging/sorting/filter.

## 9) Chi tiet API 4

### 9.1 API

Method: GET
Path: /api/inventory-receipts/{id}
Service: getReceiptById
Response: InventoryReceiptConformResponse

### 9.2 Luong xu ly

1. Security check role.
2. Service find receipt by id.
3. Neu khong ton tai -> RuntimeException.
4. Mapper map sang InventoryReceiptConformResponse.
5. Controller tra HTTP 200.

### 9.3 Noi dung response

Thong tin tong:
- inventoryReceiptId
- receiptCode
- supplierId
- supplierName
- supplierPhone
- supplierAddress
- createdById
- createdByName
- orderDate
- receivedDate
- status
- totalAmount
- note

Thong tin details:
- receiptDetailId
- productId
- productName
- orderedQuantity
- receivedQuantity
- rejectedQuantity
- unitCost
- note
- vatRate
- totalPrice
- productImage

### 9.4 Tinh vatRate trong mapper

- Mapper tinh nguoc vatRate tu unitCost, orderedQuantity, totalPrice.
- Neu du lieu khong hop le thi vatRate = 0.

## 10) Chi tiet API 5

### 10.1 API

Method: PUT
Path: /api/inventory-receipts/{id}/receive
Service: receiveReceipt
Request body: InventoryReceiptReceiveRequest
Response: InventoryReceiptConformResponse

### 10.2 Luong xu ly chi tiet

1. Security check role.
2. Kiem tra request khong null.
3. Kiem tra request.inventoryReceiptId ton tai va bang voi path id.
4. Tim receipt theo id.
5. Kiem tra status receipt phai la Pending Verification hoac Partially Entered.
6. Lay current user.
7. Khoi tao danh sach inventoryLogs va transactions.
8. Khoi tao calculatedActualTotal = 0.
9. Dat isFullyReceived = true.
10. Tao detailMap tu receipt.getDetails().
11. Lap qua tung item trong request.details:
		- Tim detail theo receiptDetailId.
		- Neu detail null -> RuntimeException.
		- Lay product tu detail.
		- receivedQty = item.receivedQuantity hoac 0 neu null.
		- Validate 0 <= receivedQty <= orderedQuantity.
		- Kiem tra unitCost gui len khop unitCost da luu.
		- Tinh VAT percentage dua tren detail tong ban dau:
			- orderedSubtotal = unitCost * orderedQuantity.
			- orderedVatAmount = totalPrice - orderedSubtotal.
			- vatPercentage = orderedVatAmount * 100 / orderedSubtotal.
		- Tinh expectedActualTotalPrice theo receivedQty.
		- Cho phep sai so lam tron 0.01.
		- Validate expectedActualTotalPrice khop item.totalPrice.
		- Cap nhat detail:
			- receivedQuantity.
			- rejectedQuantity = ordered - received.
			- actualTotalPrice.
			- note neu co.
		- Neu receivedQty > 0:
			- Tinh qtyBefore va qtyAfter.
			- Tao Inventory log.
			- Tao InventoryTransaction log voi type RECEIPT_IN.
			- Cap nhat product.onHandQuantity = qtyAfter.
		- Neu receivedQty < orderedQuantity:
			- isFullyReceived = false.
		- Cong calculatedActualTotal += actualTotalPrice.
12. Validate request.totalAmount khop tong detail voi sai so 0.01.
13. Cap nhat receipt master:
		- actualTotalAmount = calculatedActualTotal.
		- status = Fully Entered hoac Partially Entered.
		- receivedBy = currentUser.
		- receivedDate = now.
14. Save inventory logs.
15. Save transaction logs.
16. Save receipt.
17. Map response conform va tra ket qua.

### 10.3 Tac dong du lieu

- Cap nhat Inventory_Receipt_Detail.
- Cap nhat Product.onHandQuantity.
- Them ban ghi Inventory.
- Them ban ghi InventoryTransaction.
- Cap nhat Inventory_Receipt.actualTotalAmount.
- Cap nhat Inventory_Receipt.status.
- Cap nhat Inventory_Receipt.receivedBy va receivedDate.

### 10.4 Quy tac dung sai 0.01

Trong receiveReceipt, service cho phep sai so 0.01 khi so sanh BigDecimal:
- So sanh detail expectedActualTotalPrice voi client totalPrice.
- So sanh tong request.totalAmount voi calculatedActualTotal.

Dieu nay giup tranh loi do lam tron khi tinh VAT.

## 11) Checklist test truoc khi chay

1. Co tai khoan thuoc role hop le.
2. Co supplier du lieu test.
3. Co product thuoc supplier va costPrice xac dinh.
4. Co product khong thuoc supplier de test am.
5. Co token JWT hop le.
6. Co receipt pending de test receive.
7. Co receipt fully entered de test chan receive.

## 12) Mau du lieu test dung chung

Supplier hop le:
- supplierId: 10

Product thuoc supplier 10:
- productId: 1001
- productName: Frame A
- costPrice: 500000.00

Product thuoc supplier 10:
- productId: 1002
- productName: Lens B
- costPrice: 300000.00

Product khong thuoc supplier 10:
- productId: 2999

User role hop le:
- username: staff.inventory
- role: ROLE_OPERATIONS STAFF

## 13) Test cases cho API 1: GET /products/search

### TC-API1-01: Tim danh sach san pham theo supplier ton tai

Muc tieu:
- Dam bao API tra danh sach san pham cua supplier.

Tien dieu kien:
- supplierId 10 ton tai.
- supplier co it nhat 2 san pham.

Request:
- Method: GET
- URL: /api/inventory-receipts/products/search?supplierId=10
- Header Authorization: Bearer <valid token role allowed>

Buoc thuc hien:
1. Goi endpoint voi supplierId=10.
2. Quan sat status code.
3. Kiem tra cau truc response list.

Ket qua mong doi:
- HTTP 200.
- Response la array.
- Moi item co productID, productName, costPrice.
- Khong co loi security.

### TC-API1-02: supplier ton tai nhung khong co san pham

Muc tieu:
- Dam bao API co the tra list rong.

Tien dieu kien:
- supplierId 11 ton tai.
- supplierId 11 khong co product nao lien ket.

Request:
- Method: GET
- URL: /api/inventory-receipts/products/search?supplierId=11
- Header Authorization hop le.

Buoc thuc hien:
1. Goi endpoint voi supplierId=11.
2. Kiem tra no khong bi loi.

Ket qua mong doi:
- HTTP 200.
- Response = [].

### TC-API1-03: supplierId khong ton tai

Muc tieu:
- Xac nhan hanh vi khi supplier khong co product.

Tien dieu kien:
- supplierId 999999 khong ton tai.

Request:
- GET /api/inventory-receipts/products/search?supplierId=999999

Buoc thuc hien:
1. Goi endpoint.
2. Theo doi ket qua.

Ket qua mong doi:
- Thuong la HTTP 200.
- Response list rong.
- Neu repository custom throw loi thi ghi nhan theo thuc te.

### TC-API1-04: Thieu query param supplierId

Muc tieu:
- Xac nhan framework reject request thieu tham so.

Request:
- GET /api/inventory-receipts/products/search

Buoc thuc hien:
1. Goi endpoint khong co supplierId.

Ket qua mong doi:
- HTTP 400.
- Noi dung loi tu Spring ve missing parameter.

### TC-API1-05: User khong du quyen

Muc tieu:
- Dam bao endpoint duoc bao ve boi @PreAuthorize.

Tien dieu kien:
- Token cua user role CUSTOMER.

Request:
- GET /api/inventory-receipts/products/search?supplierId=10

Buoc thuc hien:
1. Goi bang token role khong hop le.

Ket qua mong doi:
- HTTP 403.
- ApiResponse code 1007 (UNAUTHORIZED) trong nhieu truong hop.

### TC-API1-06: Khong co token

Muc tieu:
- Dam bao endpoint yeu cau xac thuc.

Request:
- GET /api/inventory-receipts/products/search?supplierId=10
- Khong gui Authorization header.

Ket qua mong doi:
- HTTP 401 hoac 403 tuy security chain.

## 14) Test cases cho API 2: POST /api/inventory-receipts

### TC-API2-01: Tao phieu nhap hop le voi 2 dong san pham

Muc tieu:
- Tao duoc phieu Pending Verification va detail dung.

Tien dieu kien:
- Supplier 10 ton tai.
- Product 1001, 1002 thuoc supplier 10.
- costPrice DB khop voi request unitCost.

Request body:
{
	"supplierId": 10,
	"note": "Nhap thang 3",
	"details": [
		{
			"productId": 1001,
			"quantity": 10,
			"unitCost": 500000,
			"vatRate": 8,
			"totalPrice": 5400000,
			"note": "Dot 1"
		},
		{
			"productId": 1002,
			"quantity": 5,
			"unitCost": 300000,
			"vatRate": 8,
			"totalPrice": 1620000,
			"note": "Dot 1"
		}
	]
}

Buoc thuc hien:
1. Goi API POST.
2. Lay inventoryReceiptId tu response.
3. Goi GET /api/inventory-receipts/{id} de doi chieu detail.

Ket qua mong doi:
- HTTP 200.
- status response = Pending Verification.
- totalAmount = 7020000.
- receivedDate = null.
- moi detail co orderedQuantity dung va receivedQuantity = 0.

### TC-API2-02: Supplier khong ton tai

Muc tieu:
- Xac nhan AppException NOT_FOUND_SUPPLIER.

Request body:
{
	"supplierId": 999999,
	"note": "Invalid supplier",
	"details": [
		{
			"productId": 1001,
			"quantity": 1,
			"unitCost": 500000,
			"vatRate": 0,
			"totalPrice": 500000
		}
	]
}

Ket qua mong doi:
- HTTP 404.
- code = 1078.
- message: Can not found the supplier.

### TC-API2-03: Product khong ton tai

Muc tieu:
- Xac nhan loi RuntimeException khi productId sai.

Request body:
{
	"supplierId": 10,
	"details": [
		{
			"productId": 999888,
			"quantity": 2,
			"unitCost": 10000,
			"vatRate": 0,
			"totalPrice": 20000
		}
	]
}

Ket qua mong doi:
- HTTP 500.
- code = 9999.
- message co chu "Khong tim thay san pham ID".

### TC-API2-04: Product khong thuoc supplier

Muc tieu:
- Chan tao phieu sai nha cung cap.

Request body:
{
	"supplierId": 10,
	"details": [
		{
			"productId": 2999,
			"quantity": 2,
			"unitCost": 500000,
			"vatRate": 0,
			"totalPrice": 1000000
		}
	]
}

Ket qua mong doi:
- HTTP 500.
- code = 9999.
- message co chu "khong do nha cung cap nay phan phoi".

### TC-API2-05: Unit cost sai voi costPrice he thong

Muc tieu:
- Chan gian lan don gia nhap.

Request body:
{
	"supplierId": 10,
	"details": [
		{
			"productId": 1001,
			"quantity": 2,
			"unitCost": 499999,
			"vatRate": 0,
			"totalPrice": 999998
		}
	]
}

Ket qua mong doi:
- HTTP 500.
- code = 9999.
- message co chu "Gia nhap ... bi sai lech".

### TC-API2-06: totalPrice sai cong thuc

Muc tieu:
- Chan du lieu thanh tien sai.

Request body:
{
	"supplierId": 10,
	"details": [
		{
			"productId": 1001,
			"quantity": 10,
			"unitCost": 500000,
			"vatRate": 8,
			"totalPrice": 5300000
		}
	]
}

Ket qua mong doi:
- HTTP 500.
- code = 9999.
- message co chu "Thanh tien ... bi tinh sai".

### TC-API2-07: Request details rong

Muc tieu:
- Danh gia hanh vi khi details = [].

Request body:
{
	"supplierId": 10,
	"details": []
}

Ket qua mong doi:
- Co the HTTP 200 va tao phieu totalAmount = 0 (theo code hien tai).
- Neu nghiep vu khong chap nhan, can them validate trong service.

### TC-API2-08: details = null

Muc tieu:
- Xac nhan he thong se loi do request chua du du lieu.

Request body:
{
	"supplierId": 10,
	"details": null
}

Ket qua mong doi:
- HTTP 500 do NullPointerException.
- code = 9999.

### TC-API2-09: User khong dang nhap

Muc tieu:
- Xac nhan layer security hoac getCurrentUser chan request.

Request:
- POST /api/inventory-receipts
- Khong co token.

Ket qua mong doi:
- HTTP 401 hoac 403 tuy cau hinh security.

### TC-API2-10: User role CUSTOMER

Muc tieu:
- Dam bao role khong hop le bi chan.

Request:
- POST /api/inventory-receipts
- Token role CUSTOMER.

Ket qua mong doi:
- HTTP 403.

## 15) Test cases cho API 3: GET /api/inventory-receipts

### TC-API3-01: Lay danh sach phieu khi co du lieu

Muc tieu:
- Tra ve day du danh sach receipt.

Tien dieu kien:
- Da co toi thieu 2 receipt trong DB.

Request:
- GET /api/inventory-receipts
- Token role allowed.

Ket qua mong doi:
- HTTP 200.
- Response la list co kich thuoc >= 2.
- Moi item co inventoryReceiptId, receiptCode, status.

### TC-API3-02: Lay danh sach khi chua co du lieu

Muc tieu:
- API tra list rong an toan.

Tien dieu kien:
- Xoa du lieu test tam (neu moi truong cho phep).

Request:
- GET /api/inventory-receipts

Ket qua mong doi:
- HTTP 200.
- Response = [].

### TC-API3-03: Kiem tra mapping supplierName

Muc tieu:
- Dam bao mapper map supplierName dung.

Buoc thuc hien:
1. Goi API danh sach.
2. Lay 1 receipt da biet supplier.
3. Doi chieu supplierName voi DB.

Ket qua mong doi:
- supplierName dung voi supplier cua receipt.

### TC-API3-04: Kiem tra mapping createdByName

Muc tieu:
- Dam bao createdByName duoc map tu username.

Buoc thuc hien:
1. Goi API danh sach.
2. Doi chieu createdById va createdByName voi user tao phieu.

Ket qua mong doi:
- createdByName khop username trong DB.

### TC-API3-05: User khong du quyen

Muc tieu:
- Chan role khong hop le.

Request:
- GET /api/inventory-receipts
- Token CUSTOMER.

Ket qua mong doi:
- HTTP 403.

### TC-API3-06: Khong co token

Muc tieu:
- Endpoint bat buoc xac thuc.

Request:
- GET /api/inventory-receipts
- Khong token.

Ket qua mong doi:
- HTTP 401 hoac 403.

## 16) Test cases cho API 4: GET /api/inventory-receipts/{id}

### TC-API4-01: Lay chi tiet receipt ton tai

Muc tieu:
- Tra ve thong tin full conform response.

Tien dieu kien:
- receiptId 501 ton tai.

Request:
- GET /api/inventory-receipts/501

Ket qua mong doi:
- HTTP 200.
- inventoryReceiptId = 501.
- details khong null.
- moi detail co receiptDetailId, productId, productName.

### TC-API4-02: receipt id khong ton tai

Muc tieu:
- Xac nhan loi khi khong tim thay receipt.

Request:
- GET /api/inventory-receipts/999999

Ket qua mong doi:
- HTTP 500.
- code = 9999.
- message co chu "Khong tim thay phieu nhap kho".

### TC-API4-03: Kiem tra supplier metadata

Muc tieu:
- Doi chieu supplierPhone va supplierAddress.

Buoc thuc hien:
1. Goi API voi id hop le.
2. Doi chieu thong tin supplier voi DB.

Ket qua mong doi:
- supplierPhone dung.
- supplierAddress dung.

### TC-API4-04: Kiem tra vatRate tinh nguoc

Muc tieu:
- Dam bao vatRate map dung theo cong thuc mapper.

Buoc thuc hien:
1. Lay detail co unitCost, orderedQuantity, totalPrice.
2. Tinh tay VAT.
3. So sanh voi response.vatRate.

Ket qua mong doi:
- Sai so trong muc cho phep do rounding 2 chu so.

### TC-API4-05: User role khong hop le

Muc tieu:
- Xac nhan 403.

Request:
- GET /api/inventory-receipts/501
- Token CUSTOMER.

Ket qua mong doi:
- HTTP 403.

### TC-API4-06: Khong token

Muc tieu:
- Xac nhan endpoint can auth.

Request:
- GET /api/inventory-receipts/501
- Khong token.

Ket qua mong doi:
- HTTP 401 hoac 403.

## 17) Test cases cho API 5: PUT /api/inventory-receipts/{id}/receive

### TC-API5-01: Receive full 100% tat ca dong

Muc tieu:
- Chuyen status sang Fully Entered.
- Tang ton kho dung cho tat ca san pham.

Tien dieu kien:
- receipt 700 status Pending Verification.
- Co 2 detail:
	- detail 1 ordered=10 unitCost=500000 vatRate 8
	- detail 2 ordered=5 unitCost=300000 vatRate 8

Request body:
{
	"inventoryReceiptId": 700,
	"totalAmount": 7020000,
	"details": [
		{
			"productId": 1001,
			"receiptDetailId": 9001,
			"receivedQuantity": 10,
			"unitCost": 500000,
			"totalPrice": 5400000,
			"note": "Nhap du"
		},
		{
			"productId": 1002,
			"receiptDetailId": 9002,
			"receivedQuantity": 5,
			"unitCost": 300000,
			"totalPrice": 1620000,
			"note": "Nhap du"
		}
	]
}

Buoc thuc hien:
1. Goi API receive.
2. Kiem tra response status.
3. Kiem tra Product.onHandQuantity da tang.
4. Kiem tra Inventory log duoc tao.
5. Kiem tra InventoryTransaction duoc tao.

Ket qua mong doi:
- HTTP 200.
- status = Fully Entered.
- actualTotalAmount = 7020000.
- receivedBy va receivedDate da set.

### TC-API5-02: Receive thieu mot phan

Muc tieu:
- Chuyen status sang Partially Entered.

Tien dieu kien:
- receipt 701 Pending Verification.

Request body:
{
	"inventoryReceiptId": 701,
	"totalAmount": 6500000,
	"details": [
		{
			"productId": 1001,
			"receiptDetailId": 9011,
			"receivedQuantity": 9,
			"unitCost": 500000,
			"totalPrice": 4860000,
			"note": "Thieu 1"
		},
		{
			"productId": 1002,
			"receiptDetailId": 9012,
			"receivedQuantity": 5,
			"unitCost": 300000,
			"totalPrice": 1620000,
			"note": "Du"
		}
	]
}

Ket qua mong doi:
- HTTP 200.
- status = Partially Entered.
- detail co rejectedQuantity > 0 voi dong nhan thieu.

### TC-API5-03: Path id khong khop body inventoryReceiptId

Muc tieu:
- Chan request sai doi tuong.

Request:
- PUT /api/inventory-receipts/700/receive

Body:
{
	"inventoryReceiptId": 701,
	"totalAmount": 0,
	"details": []
}

Ket qua mong doi:
- HTTP 500.
- message co chu "ID phieu nhap khong khop".

### TC-API5-04: Receipt khong ton tai

Muc tieu:
- Xac nhan loi not found theo RuntimeException.

Request:
- PUT /api/inventory-receipts/999999/receive

Body:
{
	"inventoryReceiptId": 999999,
	"totalAmount": 0,
	"details": []
}

Ket qua mong doi:
- HTTP 500.
- code = 9999.

### TC-API5-05: Receipt status khong cho phep receive

Muc tieu:
- Chan receive khi status la Fully Entered hoac status khac.

Tien dieu kien:
- receipt 702 status Fully Entered.

Request:
- PUT /api/inventory-receipts/702/receive

Ket qua mong doi:
- HTTP 500.
- message co chu "Chi co the xac nhan ... Pending Verification hoac Partially Entered".

### TC-API5-06: receiptDetailId khong ton tai trong receipt

Muc tieu:
- Chan cap nhat detail khong thuoc phieu.

Request body:
{
	"inventoryReceiptId": 700,
	"totalAmount": 5400000,
	"details": [
		{
			"productId": 1001,
			"receiptDetailId": 999123,
			"receivedQuantity": 10,
			"unitCost": 500000,
			"totalPrice": 5400000
		}
	]
}

Ket qua mong doi:
- HTTP 500.
- message co chu "Chi tiet phieu nhap khong ton tai".

### TC-API5-07: receivedQuantity am

Muc tieu:
- Chan so luong am.

Request body:
{
	"inventoryReceiptId": 700,
	"totalAmount": 0,
	"details": [
		{
			"productId": 1001,
			"receiptDetailId": 9001,
			"receivedQuantity": -1,
			"unitCost": 500000,
			"totalPrice": 0
		}
	]
}

Ket qua mong doi:
- HTTP 500.
- message co chu "So luong thuc nhan khong hop le".

### TC-API5-08: receivedQuantity lon hon orderedQuantity

Muc tieu:
- Chan nhap qua so luong dat.

Request body:
{
	"inventoryReceiptId": 700,
	"totalAmount": 0,
	"details": [
		{
			"productId": 1001,
			"receiptDetailId": 9001,
			"receivedQuantity": 999,
			"unitCost": 500000,
			"totalPrice": 0
		}
	]
}

Ket qua mong doi:
- HTTP 500.
- thong bao so luong khong hop le.

### TC-API5-09: unitCost thuc nhan khong khop unitCost da luu

Muc tieu:
- Chan thay doi don gia trai phep khi receive.

Request body:
{
	"inventoryReceiptId": 700,
	"totalAmount": 5400000,
	"details": [
		{
			"productId": 1001,
			"receiptDetailId": 9001,
			"receivedQuantity": 10,
			"unitCost": 500001,
			"totalPrice": 5400000
		}
	]
}

Ket qua mong doi:
- HTTP 500.
- message co chu "Don gia thuc nhan khong khop".

### TC-API5-10: totalPrice tung dong sai so voi cong thuc

Muc tieu:
- Chan thanh tien detail sai.

Request body:
{
	"inventoryReceiptId": 700,
	"totalAmount": 5300000,
	"details": [
		{
			"productId": 1001,
			"receiptDetailId": 9001,
			"receivedQuantity": 10,
			"unitCost": 500000,
			"totalPrice": 5300000
		}
	]
}

Ket qua mong doi:
- HTTP 500.
- message co chu "Thanh tien thuc te ... sai lech".

### TC-API5-11: Tong totalAmount body khong khop tong detail

Muc tieu:
- Chan gian lan tong tien receipt.

Request body:
{
	"inventoryReceiptId": 700,
	"totalAmount": 1,
	"details": [
		{
			"productId": 1001,
			"receiptDetailId": 9001,
			"receivedQuantity": 10,
			"unitCost": 500000,
			"totalPrice": 5400000
		}
	]
}

Ket qua mong doi:
- HTTP 500.
- message co chu "Tong tien thuc te khong khop".

### TC-API5-12: receivedQuantity = 0

Muc tieu:
- Kiem tra khong tao inventory log neu khong nhan hang.

Request body:
{
	"inventoryReceiptId": 700,
	"totalAmount": 0,
	"details": [
		{
			"productId": 1001,
			"receiptDetailId": 9001,
			"receivedQuantity": 0,
			"unitCost": 500000,
			"totalPrice": 0,
			"note": "Hang loi, tra lai"
		}
	]
}

Ket qua mong doi:
- HTTP 200 neu cong thuc tien khop 0 va VAT.
- detail.rejectedQuantity = orderedQuantity.
- khong co InventoryTransaction duoc tao cho dong nay.
- status thuong la Partially Entered.

### TC-API5-13: Cho phep sai so 0.01 trong so sanh detail

Muc tieu:
- Xac nhan tolerance rounding.

Tien dieu kien:
- Co detail tao ra VAT co the gay le.

Request body:
- totalPrice detail sai lech 0.01 so voi expected.

Ket qua mong doi:
- API van chap nhan (HTTP 200).

### TC-API5-14: Sai so vuot qua 0.01 trong detail

Muc tieu:
- Xac nhan vuot tolerance thi reject.

Request body:
- totalPrice detail sai lech 0.02 hoac lon hon.

Ket qua mong doi:
- HTTP 500.

### TC-API5-15: User role khong hop le khi receive

Muc tieu:
- Chan CUSTOMER receive phieu.

Request:
- PUT /api/inventory-receipts/700/receive
- Token CUSTOMER.

Ket qua mong doi:
- HTTP 403.

### TC-API5-16: Khong token khi receive

Muc tieu:
- Bat buoc xac thuc.

Request:
- PUT /api/inventory-receipts/700/receive
- Khong token.

Ket qua mong doi:
- HTTP 401 hoac 403.

## 18) Bang ma tran testcase tong hop

| Ma test | API | Muc tieu | Ket qua mong doi |
|---|---|---|---|
| TC-API1-01 | GET products/search | Tim san pham supplier hop le | 200 + list |
| TC-API1-02 | GET products/search | Supplier khong co san pham | 200 + [] |
| TC-API1-03 | GET products/search | Supplier id khong ton tai | 200 + []/theo repo |
| TC-API1-04 | GET products/search | Thieu supplierId | 400 |
| TC-API1-05 | GET products/search | Role khong hop le | 403 |
| TC-API1-06 | GET products/search | Khong token | 401/403 |
| TC-API2-01 | POST receipts | Tao phieu hop le | 200 |
| TC-API2-02 | POST receipts | Supplier khong ton tai | 404 |
| TC-API2-03 | POST receipts | Product khong ton tai | 500 |
| TC-API2-04 | POST receipts | Product khong thuoc supplier | 500 |
| TC-API2-05 | POST receipts | Unit cost sai | 500 |
| TC-API2-06 | POST receipts | totalPrice sai | 500 |
| TC-API2-07 | POST receipts | details rong | 200/yeu cau nghiep vu |
| TC-API2-08 | POST receipts | details null | 500 |
| TC-API2-09 | POST receipts | Khong token | 401/403 |
| TC-API2-10 | POST receipts | Role CUSTOMER | 403 |
| TC-API3-01 | GET receipts | Co du lieu | 200 |
| TC-API3-02 | GET receipts | Khong du lieu | 200 + [] |
| TC-API3-03 | GET receipts | Mapping supplierName | Dung voi DB |
| TC-API3-04 | GET receipts | Mapping createdByName | Dung voi DB |
| TC-API3-05 | GET receipts | Role khong hop le | 403 |
| TC-API3-06 | GET receipts | Khong token | 401/403 |
| TC-API4-01 | GET by id | receipt ton tai | 200 |
| TC-API4-02 | GET by id | receipt khong ton tai | 500 |
| TC-API4-03 | GET by id | supplier metadata | Dung voi DB |
| TC-API4-04 | GET by id | vatRate mapping | Dung cong thuc |
| TC-API4-05 | GET by id | Role khong hop le | 403 |
| TC-API4-06 | GET by id | Khong token | 401/403 |
| TC-API5-01 | PUT receive | Nhan du | 200 + Fully Entered |
| TC-API5-02 | PUT receive | Nhan thieu | 200 + Partially Entered |
| TC-API5-03 | PUT receive | id path/body lech | 500 |
| TC-API5-04 | PUT receive | receipt khong ton tai | 500 |
| TC-API5-05 | PUT receive | status khong hop le | 500 |
| TC-API5-06 | PUT receive | receiptDetail sai | 500 |
| TC-API5-07 | PUT receive | received am | 500 |
| TC-API5-08 | PUT receive | received vuot ordered | 500 |
| TC-API5-09 | PUT receive | unitCost sai | 500 |
| TC-API5-10 | PUT receive | detail total sai | 500 |
| TC-API5-11 | PUT receive | tong total sai | 500 |
| TC-API5-12 | PUT receive | received=0 | 200/partially |
| TC-API5-13 | PUT receive | sai so 0.01 | 200 |
| TC-API5-14 | PUT receive | sai so >0.01 | 500 |
| TC-API5-15 | PUT receive | Role CUSTOMER | 403 |
| TC-API5-16 | PUT receive | Khong token | 401/403 |

## 19) Huong dan viet test tu dong (goi y)

Neu viet integration test cho Spring Boot:
- Su dung @SpringBootTest + @AutoConfigureMockMvc.
- Dung MockMvc de call endpoint.
- Seed du lieu bang SQL script test.
- Dung @WithMockUser cho role test nhanh.
- Dung transactional rollback sau moi testcase.

Mau assertion cho API loi:
- status().isInternalServerError()
- jsonPath("$.code").value(9999)
- jsonPath("$.message").exists()

Mau assertion cho API thanh cong:
- status().isOk()
- jsonPath("$.inventoryReceiptId").exists()

## 20) Danh sach test data de tai su dung

Bo du lieu A:
- supplierId = 10
- productId = 1001
- orderedQty = 10
- unitCost = 500000
- vatRate = 8
- totalPrice = 5400000

Bo du lieu B:
- supplierId = 10
- productId = 1002
- orderedQty = 5
- unitCost = 300000
- vatRate = 8
- totalPrice = 1620000

Bo du lieu C (invalid product):
- productId = 999888

Bo du lieu D (wrong supplier product):
- productId = 2999

Bo du lieu E (wrong unit cost):
- unitCost = costPrice +/- 1

Bo du lieu F (wrong total):
- totalPrice = expected +/- 100000

## 21) Kich ban regression de nghi

Kich ban R1:
1. Tao phieu hop le.
2. List tat ca phieu.
3. Mo chi tiet phieu.
4. Receive du so luong.
5. Mo lai chi tiet phieu.
6. Kiem tra status va so luong ton.

Kich ban R2:
1. Tao phieu hop le.
2. Receive thieu so luong.
3. Kiem tra status Partially Entered.
4. Receive lan 2 bo sung (neu nghiep vu cho phep).
5. Kiem tra status Fully Entered.

Kich ban R3:
1. Tao phieu voi 1 dong sai don gia.
2. Dam bao he thong reject.
3. Tao lai phieu dung don gia.
4. Dam bao he thong chap nhan.

Kich ban R4:
1. Tao phieu voi 2 dong.
2. Receive voi totalAmount sai.
3. Dam bao reject.
4. Receive lai voi totalAmount dung.
5. Dam bao thanh cong.

## 22) Cac risk hien tai trong code can ghi nhan

Risk 1:
- Nhieu validate dang throw RuntimeException -> tra 500.
- Nen doi sang AppException + ErrorCode nghiep vu de tra 4xx ro rang hon.

Risk 2:
- Controller chua @Valid cho body InventoryReceiptRequest va InventoryReceiptReceiveRequest.
- De gay NPE khi field null.

Risk 3:
- API getAllReceipts chua co paging.
- Co the gay cham khi du lieu lon.

Risk 4:
- API getReceiptById throw RuntimeException cho not found.
- Nen chuyen AppException ORDER/RECEIPT_NOT_FOUND de tra 404.

Risk 5:
- Nhieu status dang la string hard-code.
- Nen dung enum de tranh typo.

## 23) De xuat cai tien sau khi test

De xuat 1:
- Them @Valid vao request body cua POST/PUT.

De xuat 2:
- Them annotation validation cho DTO:
	- @NotNull supplierId
	- @NotEmpty details
	- @Min(1) quantity
	- @DecimalMin cho unitCost

De xuat 3:
- Dung AppException cho cac loi business hien dang RuntimeException.

De xuat 4:
- Bo sung ErrorCode rieng:
	- RECEIPT_NOT_FOUND
	- RECEIPT_DETAIL_NOT_FOUND
	- INVALID_RECEIVE_QUANTITY
	- PRODUCT_NOT_BELONG_TO_SUPPLIER

De xuat 5:
- Them endpoint paging:
	- GET /api/inventory-receipts?page=0&size=20

De xuat 6:
- Dung enum cho status:
	- PENDING_VERIFICATION
	- PARTIALLY_ENTERED
	- FULLY_ENTERED

## 24) Mau script Postman cho API 2

Pre-request script:
pm.environment.set("token", "<paste-token>");

Request headers:
Authorization: Bearer {{token}}
Content-Type: application/json

Tests script:
pm.test("Status code is 200", function () {
		pm.response.to.have.status(200);
});
pm.test("Has inventoryReceiptId", function () {
		var jsonData = pm.response.json();
		pm.expect(jsonData.inventoryReceiptId).to.not.eql(null);
});

## 25) Mau script Postman cho API 5

Tests script thanh cong:
pm.test("Status is 200", function () {
	pm.response.to.have.status(200);
});
pm.test("Status field is updated", function () {
	var body = pm.response.json();
	pm.expect(["Fully Entered", "Partially Entered"]).to.include(body.status);
});

Tests script loi:
pm.test("Status is 500 for invalid total", function () {
	pm.response.to.have.status(500);
});
pm.test("Error code is 9999", function () {
	var body = pm.response.json();
	pm.expect(body.code).to.eql(9999);
});

## 26) Bang kiem tra DB sau testcase receive

Can doi chieu cac bang:
- Inventory_Receipt
- Inventory_Receipt_Detail
- Product
- Inventory
- Inventory_Transaction

Kiem tra cot Inventory_Receipt:
- Status
- Actual_Total_Amount
- Received_By
- Received_Date

Kiem tra cot Inventory_Receipt_Detail:
- Received_Quantity
- Rejected_Quantity
- Actual_Total_Price

Kiem tra cot Product:
- On_Hand_Quantity

Kiem tra cot Inventory:
- Quantity_Before
- Quantity_After

Kiem tra cot Inventory_Transaction:
- Transaction_Type = RECEIPT_IN
- Quantity_Change
- Reference_Type = INVENTORY_RECEIPT

## 27) Ky luat tinh toan duoc dung trong testcase

Cong thuc 1:
- Subtotal = UnitCost * Quantity

Cong thuc 2:
- VAT Amount = Subtotal * VAT Rate / 100

Cong thuc 3:
- TotalPrice = Subtotal + VAT Amount

Cong thuc 4:
- RejectedQuantity = OrderedQuantity - ReceivedQuantity

Cong thuc 5:
- QtyAfter = QtyBefore + ReceivedQuantity

## 28) Mau tinh nhanh cho bo du lieu A

Input:
- quantity = 10
- unitCost = 500000
- vatRate = 8

Tinh:
- subtotal = 5000000
- vat = 400000
- total = 5400000

## 29) Mau tinh nhanh cho bo du lieu B

Input:
- quantity = 5
- unitCost = 300000
- vatRate = 8

Tinh:
- subtotal = 1500000
- vat = 120000
- total = 1620000

## 30) Tieu chi pass/fail cho dot test

Pass khi:
1. Tat ca testcase mandatory pass.
2. Cac testcase loi tra dung status mong doi.
3. Du lieu DB sau test khop voi expected.
4. Khong phat sinh loi race condition trong receive.

Fail khi:
1. Co testcase happy path that bai.
2. Co sai lech so luong ton kho.
3. Co sai lech totalAmount/actualTotalAmount.
4. Co loi security bo qua role check.

## 31) Danh sach testcase mandatory

Mandatory cho API 1:
- TC-API1-01
- TC-API1-05

Mandatory cho API 2:
- TC-API2-01
- TC-API2-02
- TC-API2-04
- TC-API2-06

Mandatory cho API 3:
- TC-API3-01
- TC-API3-05

Mandatory cho API 4:
- TC-API4-01
- TC-API4-02

Mandatory cho API 5:
- TC-API5-01
- TC-API5-02
- TC-API5-05
- TC-API5-10
- TC-API5-11

## 32) Danh sach testcase mo rong

Mo rong API 1:
- TC-API1-02
- TC-API1-03
- TC-API1-04
- TC-API1-06

Mo rong API 2:
- TC-API2-03
- TC-API2-05
- TC-API2-07
- TC-API2-08
- TC-API2-09
- TC-API2-10

Mo rong API 3:
- TC-API3-02
- TC-API3-03
- TC-API3-04
- TC-API3-06

Mo rong API 4:
- TC-API4-03
- TC-API4-04
- TC-API4-05
- TC-API4-06

Mo rong API 5:
- TC-API5-03
- TC-API5-04
- TC-API5-06
- TC-API5-07
- TC-API5-08
- TC-API5-09
- TC-API5-12
- TC-API5-13
- TC-API5-14
- TC-API5-15
- TC-API5-16

## 33) Kich ban test hieu nang co ban

Kich ban performance 1:
- Tao 1 receipt voi 100 details.
- Xac nhan create khong qua nguong timeout de ra.

Kich ban performance 2:
- Goi getAllReceipts khi co 10.000 receipt.
- Do thoi gian response.

Kich ban performance 3:
- Receive receipt 100 details.
- Kiem tra transaction + inventory saveAll hoat dong on dinh.

## 34) Kich ban test dong thoi

Scenario C1:
- Hai user cung receive mot receipt cung luc.
- Mong doi he thong co co che tranh ghi de khong nhat quan.

Scenario C2:
- Mot user receive, mot user getById lien tuc.
- Mong doi du lieu nhat quan sau commit.

## 35) Kich ban test bao mat

Security S1:
- Token het han.
- Mong doi 401.

Security S2:
- Token sai chu ky.
- Mong doi 401.

Security S3:
- Token role CUSTOMER hit API receive.
- Mong doi 403.

Security S4:
- User hop le nhung thieu authority theo @PreAuthorize.
- Mong doi 403.

## 36) Ghi chu cho QA

1. Vi service dung RuntimeException cho nhieu nhanh nghiep vu,
	 status loi hien tai thuong la 500 thay vi 4xx.
2. Khi report bug, can ghi ro day la hanh vi hien tai hay mong doi nghiep vu.
3. Nen attach request/response raw va snapshot du lieu DB truoc/sau.

## 37) Ghi chu cho Dev

1. Can can nhac refactor loi nghiep vu sang AppException.
2. Can them @Valid cho request body POST/PUT.
3. Can bo sung unit test cho cong thuc tinh VAT va sai so 0.01.
4. Can bo sung optimistic locking neu co race condition receive.

## 38) Ket luan

Tai lieu nay da bao gom:
- Luong nghiep vu day du cho 5 API trong InventoryReceiptController.
- Nhieu testcase cho moi API (vuot muc toi thieu 2 testcase/API).
- Danh sach risk va de xuat cai tien de nang chat luong he thong.

Sau khi chay xong bo testcase nay, doi du an co the:
- Tu tin hon ve tinh dung cua luong nhap kho.
- Som phat hien sai lech tien te, so luong, phan quyen.
- Co co so de refactor code huong toi 4xx error nghiep vu ro rang hon.
