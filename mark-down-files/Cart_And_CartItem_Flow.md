# CART AND CART ITEM - LUONG DI NUOC BUOC (CHI TIET)

## 1) Muc tieu tai lieu
Tai lieu nay mo ta day du luong xu ly Cart trong he thong, theo dung code hien tai:
- Luong nguoi dung thao tac gio hang
- Luong backend xu ly tung API
- Cac nhanh thanh cong/that bai
- Cac diem can canh bao de FE, QA, BA, Dev khong hieu sai


## 2) Pham vi API
Base path: `/api/cart`

Danh sach API:
1. `POST /api/cart/add`
2. `GET /api/cart/getAllCart`
3. `DELETE /api/cart/delete/{cartItemId}`
4. `DELETE /api/cart/deleteAllCart`
5. `PUT /api/cart/update`


## 3) Tong quan luong nghiep vu

### 3.1 Nhan dien user
Tat ca API Cart deu lay user dang dang nhap tu SecurityContext.

Neu co van de:
1. Khong co auth hoac auth khong hop le -> loi `UNAUTHENTICATED`.
2. Username trong token khong ton tai trong DB -> loi `USER_NOT_EXISTED`.

Y nghia:
- Cart la theo user hien tai, khong truyen `userId` qua request.
- FE khong can truyen userId cho API cart.


### 3.2 Cart duoc tao khi nao
Cart duoc tao lazy:
1. Khi goi `POST /add`, neu user chua co cart thi backend tao moi.
2. Cac API khac chi doc/sua/xoa tren cart da co.


### 3.3 Quy tac "gop item" trong gio
He thong khong chi so sanh product id, ma so sanh ca prescription:
1. Cung bo san pham (contactLensId/frameId/lensId).
2. Cung bo thong so do (SPH/CYL/AXIS/ADD/PD...)

Neu dung ca 2 dieu kien tren:
- Item duoc xem la trung, se cong don quantity.

Neu khac 1 thong so prescription:
- Tao item moi, khong merge.


## 4) Luong chi tiet tung API

---

## 4.1 POST /api/cart/add

### Muc dich
Them san pham vao gio hoac tang so luong neu item da ton tai.

### Input chinh
- `contactLensId` (optional)
- `frameId` (optional)
- `lensId` (optional)
- `quantity` (required, >= 1)
- cac truong prescription (optional)

### Luong xu ly day du
1. Xac dinh current user.
2. Tim cart cua user:
	- Co -> dung cart cu.
	- Khong -> tao cart moi.
3. Tai entity san pham theo id neu request co gui:
	- contact lens id sai -> `CONTACT_LENS_NOT_FOUND`.
	- frame id sai -> `FRAME_NOT_FOUND`.
	- lens id sai -> `LENS_NOT_FOUND`.
4. Xac dinh item trung trong cart bang 2 lop so sanh:
	- Lop 1: cung bo id san pham.
	- Lop 2: cung prescription.
5. Neu tim thay item trung:
	- quantity moi = quantity cu + quantity request.
	- tinh lai `itemType` (de bat kip thay doi ton kho/preorder).
	- save.
6. Neu chua co item trung:
	- tao cartItem moi.
	- set thong tin product references.
	- set quantity.
	- set `itemType` bang ham phan loai.
	- lay gia tu DB:
	  - framePrice = frame.product.price (neu co frame)
	  - lensPrice = lens.product.price (neu co lens)
	  - contactLensPrice = contactLens.product.price (neu co)
	- tinh `price` tong hop = tong cac gia thanh phan.
	- save item.
7. Neu request co thong so prescription va item la item moi:
	- tao `CartItemPrescription`.
	- luu day du cac truong do mat.
8. Map sang `CartItemResponse` va tra ve 200.

### Decision itemType
Thu tu uu tien:
1. Neu co bat ky thong so prescription -> `PRESCRIPTION`.
2. Neu khong co prescription, nhung co it nhat 1 product thoa:
	- `allowPreorder = true`
	- `availableQuantity = 0`
	-> `PREORDER`.
3. Con lai -> `ORDER`.

### Diem canh bao quan trong
1. `price` hien tai la don gia combo item, khong nhan theo quantity.
2. Request co truong framePrice/lensPrice/price, nhung service dang lay gia tu DB.
3. Validation prescription chua co range nghiep vu sau (vd axis 0-180), nen du lieu la van co the vao.


---

## 4.2 GET /api/cart/getAllCart

### Muc dich
Lay toan bo item gio hang cua user hien tai.

### Luong xu ly
1. Xac dinh current user.
2. Tim cart theo user.
3. Neu khong co cart -> tra ve `[]`.
4. Neu co cart -> map tung `CartItem` sang `CartItemResponse`.
5. Tra ve 200.

### Y nghia cho FE
1. Day la API nguon de render cart page.
2. Empty cart duoc bieu dien boi mang rong, khong phai loi.


---

## 4.3 PUT /api/cart/update

### Muc dich
Cap nhat quantity cua 1 cartItem co san.

### Input
- `cartItemId` (required)
- `quantity` (required, >=1)

### Luong xu ly
1. Validate request body (`cartItemId`, `quantity`).
2. Xac dinh current user.
3. Tim cartItem theo id:
	- khong ton tai -> `CART_ITEM_NOT_FOUND`.
4. Kiem tra owner:
	- Neu cartItem khong thuoc user hien tai -> `UNAUTHENTICATED`.
5. Set quantity moi.
6. Save cartItem.
7. Map response va tra 200.

### Luu y
1. API nay cap nhat quantity truc tiep, khong cong don.
2. `price` khong duoc tinh lai theo quantity trong code hien tai.


---

## 4.4 DELETE /api/cart/delete/{cartItemId}

### Muc dich
Xoa 1 item khoi gio.

### Luong xu ly
1. Xac dinh current user.
2. Tim cartItem theo id.
3. Neu khong tim thay:
	- code hien tai throw `UNAUTHENTICATED` (diem can cai tien).
4. Neu tim thay, kiem tra owner:
	- dung owner -> xoa.
	- sai owner -> `UNAUTHENTICATED`.
5. Tra `204 No Content`.

### Canh bao
Trong nghiep vu thuong gap, item khong ton tai nen tra `CART_ITEM_NOT_FOUND` (404),
nhung hien tai code dang tra huong unauthenticated.


---

## 4.5 DELETE /api/cart/deleteAllCart

### Muc dich
Xoa tat ca item cua gio hien tai.

### Luong xu ly
1. Xac dinh current user.
2. Tim cart theo user.
3. Neu co cart -> clear danh sach cartItems, save cart.
4. Neu khong co cart -> bo qua (khong loi).
5. Tra `204 No Content`.

### Y nghia
API an toan de goi nhieu lan; khong co cart van khong bi loi.


## 5) Luong FE thao tac thuc te

### 5.1 Them san pham vao gio
1. FE goi `POST /add` voi combo product + quantity (+ prescription neu co).
2. Backend merge hoac tao item moi.
3. FE co the:
	- hien toast thanh cong.
	- goi lai `GET /getAllCart` de dong bo UI.

### 5.2 Tang/giam so luong tren man hinh cart
1. FE goi `PUT /update` khi user bam +/-. 
2. Neu thanh cong, update dong item do trong UI.
3. Neu 404/401, buoc FE reload gio bang `GET /getAllCart`.

### 5.3 Xoa 1 dong san pham
1. FE goi `DELETE /delete/{id}`.
2. Thanh cong -> remove dong tren UI.
3. That bai -> thong bao user + refresh gio.

### 5.4 Xoa toan bo gio
1. FE goi `DELETE /deleteAllCart`.
2. Thanh cong -> dat danh sach item = rong.


## 6) Bang quy tac merge item (rat quan trong)

Hai request se merge neu:
1. Cung `contactLensId` (hoac cung null).
2. Cung `frameId` (hoac cung null).
3. Cung `lensId` (hoac cung null).
4. Cung toan bo prescription fields.

Chi can khac 1 trong cac dieu kien tren:
- Khong merge, tao item moi.


## 7) Bang ma loi hay gap

1. `UNAUTHENTICATED`
	- khong token / token khong hop le / khong dung owner item.

2. `USER_NOT_EXISTED`
	- user trong token khong ton tai DB.

3. `CONTACT_LENS_NOT_FOUND`
4. `FRAME_NOT_FOUND`
5. `LENS_NOT_FOUND`
6. `CART_ITEM_NOT_FOUND`

7. Validation errors:
	- `QUANTITY_REQUIRED`
	- `QUANTITY_MUST_BE_GREATER_THAN_0`

8. Media/header:
	- 415 neu Content-Type sai.
	- 406 neu Accept khong chap nhan json.


## 8) Decision tree ngan gon de debug nhanh

### Khi add item that bai
1. Kiem tra token.
2. Kiem tra quantity co >=1.
3. Kiem tra id product co ton tai.
4. Kiem tra payload JSON dung type.
5. Kiem tra header Content-Type, Accept.

### Khi add item khong merge nhu ky vong
1. So sanh lai prescription tung field.
2. Kiem tra alias JSON (`rightADD`, `PD_Right`, ...).
3. Kiem tra gia tri null vs 0 co bi gui khac nhau.

### Khi update/delete bao 401
1. Kiem tra cartItem co thuoc user dang dang nhap khong.
2. Kiem tra token co dung account khong.


## 9) Kich ban nghiep vu can test uu tien cao

### P0 - Bat buoc
1. Add item moi thanh cong.
2. Add trung item -> quantity cong don.
3. Add cung product nhung khac prescription -> tao dong moi.
4. Update quantity thanh cong.
5. Delete item thanh cong.
6. Clear cart thanh cong.
7. Unauthorized khong token -> 401.

### P1 - Quan trong
1. ItemType PRESCRIPTION khi co do.
2. ItemType PREORDER khi het hang nhung cho preorder.
3. GET cart khi chua co cart -> []
4. Validate quantity = 0 -> 400.

### P2 - Mo rong
1. Payload malformed.
2. Race condition add/update/delete dong thoi.
3. So luong lon bat thuong.


## 10) Vi du payload de FE copy dung ngay

### 10.1 Add frame + lens
```json
{
  "frameId": 101,
  "lensId": 205,
  "quantity": 1
}
```

### 10.2 Add item co prescription
```json
{
  "frameId": 101,
  "lensId": 205,
  "quantity": 1,
  "rightEyeSph": -1.25,
  "leftEyeSph": -1.00,
  "pd": 62.0
}
```

### 10.3 Update quantity
```json
{
  "cartItemId": 9001,
  "quantity": 3
}
```


## 11) Kien nghi cai tien ky thuat

1. Doi logic delete item khong ton tai sang `CART_ITEM_NOT_FOUND` de ro nghia.
2. Them validation nghiep vu cho prescription (axis/pd range).
3. Xac nhan lai nghiep vu field `price`:
	- la don gia item,
	- hay tong theo quantity.
4. Neu can, bo bot cac truong gia trong request add de tranh FE hieu nham.


## 12) Tom tat 1 dong
Cart flow hien tai du on cho nghiep vu mua hang co prescription; diem can luu y lon nhat la quy tac merge theo prescription, cach xac dinh itemType, va mot vai ma loi chua that su "than thien nghiep vu" (dac biet delete item not found).


## 13) Timeline chi tiet theo Actor (FE -> API -> Service -> DB)

### 13.1 Timeline POST /add (truong hop tao item moi)
1. FE gui request JSON.
2. Controller nhan body va validate bean.
3. Service lay current user tu SecurityContext.
4. Service tim cart theo user.
5. Service load product references theo id.
6. Service quet cart de tim item trung.
7. Khong co item trung -> khoi tao CartItem moi.
8. Service set itemType.
9. Service set gia thanh phan + gia tong.
10. Service save CartItem.
11. Neu co prescription -> tao CartItemPrescription va save.
12. Mapper map sang CartItemResponse.
13. Controller tra 200.

### 13.2 Timeline POST /add (truong hop merge item cu)
1. FE gui request add cung combo.
2. Service tim thay item trung (product + prescription).
3. quantity cu + quantity moi.
4. itemType tinh lai.
5. save CartItem.
6. bo qua tao prescription moi.
7. tra response item da merge.

### 13.3 Timeline PUT /update
1. FE gui `cartItemId`, `quantity`.
2. Validate request.
3. Service lay user.
4. Service tim CartItem theo id.
5. Kiem tra owner.
6. update quantity.
7. save.
8. map response.
9. tra 200.

### 13.4 Timeline DELETE /delete/{id}
1. FE gui delete item id.
2. Service lay user.
3. Service tim CartItem theo id.
4. Kiem tra owner.
5. deleteById.
6. tra 204.

### 13.5 Timeline DELETE /deleteAllCart
1. FE gui request clear.
2. Service lay user.
3. Service tim cart.
4. clear danh sach cartItems.
5. save cart.
6. tra 204.


## 14) Ma tran truoc/sau du lieu (state transition)

### 14.1 POST /add - tao item moi
Truoc:
1. Cart co the chua ton tai.
2. Khong co CartItem trung.

Sau:
1. Cart chac chan ton tai.
2. Co them 1 CartItem.
3. Co them CartItemPrescription neu request co do.

### 14.2 POST /add - merge
Truoc:
1. Da co CartItem trung.

Sau:
1. So dong CartItem khong doi.
2. Quantity cua dong do tang len.
3. Prescription cu giu nguyen.

### 14.3 PUT /update
Truoc:
1. Co CartItem hop le va dung owner.

Sau:
1. Quantity doi theo request.
2. Cac field gia va product references khong doi.

### 14.4 DELETE /delete
Truoc:
1. Co CartItem hop le va dung owner.

Sau:
1. CartItem bi xoa.
2. Prescription lien ket bi xoa theo orphan removal.

### 14.5 DELETE /deleteAllCart
Truoc:
1. Cart co N item.

Sau:
1. Cart con 0 item.
2. Cart record cua user van ton tai.


## 15) Bang quy tac prescription (chi tiet tung field)

He thong xem request "co prescription" neu BAT KY field duoi day khac null:
1. rightEyeSph
2. rightEyeCyl
3. rightEyeAxis
4. rightEyeAdd
5. leftEyeSph
6. leftEyeCyl
7. leftEyeAxis
8. leftEyeAdd
9. pd
10. pdRight
11. pdLeft

He thong xem 2 prescription la giong nhau neu tat ca field tren deu bang nhau (Objects.equals).

He qua nghiep vu:
1. Chi can khac 1 field -> item khong merge.
2. Request khong co do va item cu co do -> khong merge.
3. Request co do va item cu khong co do -> khong merge.


## 16) Bang alias JSON can nho cho FE

`rightEyeAdd` chap nhan:
1. rightADD
2. rightAdd
3. RIGHT_ADD
4. right_add

`leftEyeAdd` chap nhan:
1. leftADD
2. leftAdd
3. LEFT_ADD
4. left_add

`pd` chap nhan:
1. PD
2. Pd
3. pD

`pdRight` chap nhan:
1. PD_Right
2. PDRight
3. pd_right
4. rightPD
5. rightPd
6. pdright
7. PD_RIGHT

`pdLeft` chap nhan:
1. PD_Left
2. PDLeft
3. pd_left
4. leftPD
5. leftPd
6. pdleft
7. PD_LEFT


## 17) Response mapping chi tiet (CartItemMapper)

Khi map response, backend tra them du lieu "thuan UI":
1. `contactLensName`, `frameName`, `lensName` lay tu Product.productName.
2. `contactLensImg`, `frameImg`, `lensImg` lay anh avatar (isAvatar=true).
3. `contactLensAvailableQuantity`, `frameAvailableQuantity`, `lensAvailableQuantity`.
4. `contactLensPreorder`, `framePreorder`, `lensPreorder`.
5. `prescription` map sang chuoi, null thi tra "0" cho tung truong.

Y nghia:
1. FE co the render full cart card ma khong can goi API product rieng.
2. Can xu ly truong anh null khi product chua co avatar.


## 18) Playbook cho FE (de implement UI on dinh)

### 18.1 Trang Product Detail -> Add to cart
1. Thu thap combo item (frame/lens/contact lens).
2. Thu thap prescription neu user co nhap do.
3. Goi `POST /add`.
4. Neu 200:
	- update badge cart.
	- hien thong bao thanh cong.
5. Neu 401:
	- dieu huong login.
6. Neu 404 product not found:
	- thong bao san pham khong con ton tai.

### 18.2 Trang Cart
1. Load lan dau bang `GET /getAllCart`.
2. Khi tang/giam quantity:
	- optimistic update (optional) hoac disable button trong luc goi API.
	- goi `PUT /update`.
3. Khi xoa item:
	- confirm modal.
	- goi `DELETE /delete/{id}`.
4. Khi clear all:
	- confirm modal co canh bao.
	- goi `DELETE /deleteAllCart`.

### 18.3 UX fallback
Neu thao tac update/delete fail:
1. Hien thong bao loi.
2. Goi lai `GET /getAllCart` de dong bo state thuc.


## 19) Playbook cho QA (manual runbook)

### 19.1 Chuan bi data
1. 1 account user A.
2. 1 account user B.
3. It nhat 1 frame in-stock.
4. It nhat 1 lens in-stock.
5. It nhat 1 contact lens allowPreorder=true va availableQuantity=0.

### 19.2 Chuoi test smoke 10 phut
1. Login A.
2. Add frame+lens quantity=1 -> pass.
3. Add lai cung payload -> quantity tang.
4. Add cung payload + them prescription -> tao dong moi.
5. Update quantity dong 1 -> pass.
6. Delete dong 2 -> pass.
7. Login B, thu update cartItem cua A -> 401.
8. A clear all -> cart rong.

### 19.3 Chuoi test media/header
1. Sai Content-Type -> 415.
2. Accept text/plain -> 406.
3. JSON malformed -> 400.


## 20) Cookbook loi theo trieu chung

### Trieu chung 1: "Them vao gio nhung khong thay tang so luong"
Kiem tra:
1. Payload co khac prescription khong.
2. Gia tri null/0 cua prescription co dong nhat khong.
3. Product combo co khop hoan toan khong.

### Trieu chung 2: "User A xoa item cua User B bi 401"
Ket luan:
1. Hanh vi dung theo owner check.

### Trieu chung 3: "Delete id khong ton tai cung ra 401"
Ket luan:
1. Do code hien tai throw `UNAUTHENTICATED` o nhanh not found.
2. Day la technical debt, khong phai loi deploy.

### Trieu chung 4: "Gia tri price khong doi khi quantity tang"
Ket luan:
1. Theo code hien tai, `price` la don gia item.
2. Tong tien UI nen tu tinh: `price * quantity`.


## 21) Decision table nhanh cho backend debug

1. request co prescription?
	- co -> itemType toi da la PRESCRIPTION.
	- khong -> check preorder.

2. product co preorder?
	- co bat ky product allowPreorder=true va available=0 -> PREORDER.
	- khong -> ORDER.

3. item da ton tai (same product + same prescription)?
	- co -> merge quantity.
	- khong -> tao item moi.


## 22) De xuat nang cap tiep theo (roadmap)

### 22.1 Nhom "do ro nghia API"
1. DELETE item not found -> doi sang `CART_ITEM_NOT_FOUND`.
2. Them error code rieng cho owner mismatch (neu can).

### 22.2 Nhom "nghiep vu prescription"
1. Validate range axis/pd/add.
2. Quy uoc ro null/0 cho tung field.

### 22.3 Nhom "hieu nang"
1. Toi uu query cart list neu cart rat lon.
2. Xem xet index cho cart item theo cart_id.

### 22.4 Nhom "du lieu gia"
1. Chot quy uoc price field la unit hay extended.
2. Neu la extended thi tinh lai khi update quantity.


## 23) Chuan giao tiep FE-BE de tranh sai lech

1. FE KHONG gui userId cho cart APIs.
2. FE co the bo qua framePrice/lensPrice/price trong add request.
3. FE phai gui quantity >= 1.
4. FE nen dong bo prescription keys theo alias da cho phep.
5. FE coi `GET /getAllCart` la source of truth sau moi thao tac quan trong.


## 24) Phu luc - Mau kich ban E2E chi tiet

### Kich ban E2E-01: mua kinh thuong (khong do)
1. User vao trang product.
2. Add frame + lens.
3. Gio hien 1 dong itemType ORDER.
4. Tang quantity len 2.
5. Checkout doc cart.

### Kich ban E2E-02: mua kinh co do
1. User chon frame + lens.
2. Nhap do mat.
3. Add cart.
4. itemType = PRESCRIPTION.
5. Add lai cung combo cung do -> merge.
6. Add lai cung combo khac do -> tach dong.

### Kich ban E2E-03: san pham preorder
1. Product allowPreorder=true, available=0.
2. Add cart khong prescription.
3. itemType = PREORDER.

### Kich ban E2E-04: clear cart va tao lai
1. Cart co 3 item.
2. Clear all.
3. Add item moi.
4. Cart hoat dong binh thuong.


