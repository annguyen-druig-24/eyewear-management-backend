CREATE DATABASE EyewearManagement
GO
USE EyewearManagement
GO

/* =========================
   1) MASTER TABLES
   ========================= */

CREATE TABLE Role (
                      Role_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                      Type_Name NVARCHAR(50) NOT NULL,
                      CONSTRAINT CK_Role_Type CHECK (Type_Name IN (
                                                                   N'CUSTOMER', N'ADMIN', N'MANAGER', N'SALES STAFF', N'OPERATIONS STAFF'
                          ))
);
GO

CREATE TABLE [User] (
                        User_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
    Username NVARCHAR(50) UNIQUE NOT NULL,
    Password NVARCHAR(100) NOT NULL,
    Email NVARCHAR(100) UNIQUE NOT NULL,
    Phone VARCHAR(15) NOT NULL,
    Role_ID BIGINT NOT NULL,
    Status BIT NOT NULL,
    Name NVARCHAR(100) NOT NULL,
    Address NVARCHAR(255),
    Date_of_Birth DATE NOT NULL,
    ID_Number VARCHAR(20) UNIQUE,
    Province_Code  INT           NULL,
    Province_Name  NVARCHAR(100) NULL,
    District_Code  INT           NULL,
    District_Name  NVARCHAR(100) NULL,
    Ward_Code      NVARCHAR(20)  NULL,
    Ward_Name      NVARCHAR(100) NULL,

    CONSTRAINT FK_User_Role FOREIGN KEY (Role_ID) REFERENCES Role(Role_ID),
    CONSTRAINT CK_User_Status CHECK (Status IN (0,1)),
    CONSTRAINT CK_User_District_Pair CHECK (
(District_Code IS NULL AND District_Name IS NULL)
    OR (District_Code IS NOT NULL AND District_Name IS NOT NULL)
    ),
    CONSTRAINT CK_User_Ward_Pair CHECK (
(Ward_Code IS NULL AND Ward_Name IS NULL)
    OR (Ward_Code IS NOT NULL AND Ward_Name IS NOT NULL)
    )
    );
GO

CREATE TABLE Brand (
                       Brand_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                       Brand_Name NVARCHAR(100) NOT NULL,
                       Description NVARCHAR(255),
                       Logo_URL VARCHAR(MAX),
    Status BIT NOT NULL
);
GO

CREATE TABLE Supplier (
                          Supplier_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                          Supplier_Name NVARCHAR(100) NOT NULL,
                          Supplier_Phone VARCHAR(15) NOT NULL,
                          Supplier_Address NVARCHAR(255) NOT NULL
);
GO

CREATE TABLE Brand_Supplier (
                                Brand_Supplier_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                                Brand_ID BIGINT NOT NULL,
                                Supplier_ID BIGINT NOT NULL,

                                CONSTRAINT FK_BrandSupplier_Brand FOREIGN KEY (Brand_ID) REFERENCES Brand(Brand_ID),
                                CONSTRAINT FK_BrandSupplier_Supplier FOREIGN KEY (Supplier_ID) REFERENCES Supplier(Supplier_ID)
);
GO

CREATE TABLE Product_Type (
                              Product_Type_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                              Type_Name NVARCHAR(100) NOT NULL,
                              Description NVARCHAR(255)
);
GO

CREATE TABLE Product (
                         Product_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                         Product_Name NVARCHAR(255) NOT NULL,
                         SKU NVARCHAR(50) NULL,
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

                         CONSTRAINT FK_Product_ProductType FOREIGN KEY (Product_Type_ID) REFERENCES Product_Type(Product_Type_ID),
                         CONSTRAINT FK_Product_Brand FOREIGN KEY (Brand_ID) REFERENCES Brand(Brand_ID),

                         CONSTRAINT CK_Product_Price CHECK (Price >= 0),
                         CONSTRAINT CK_Product_CostPrice CHECK (Cost_Price >= 0),
                         CONSTRAINT CK_Product_OnHandQuantity CHECK (On_Hand_Quantity >= 0),
                         CONSTRAINT CK_Product_ReservedQuantity CHECK (Reserved_Quantity >= 0),
                         CONSTRAINT CK_Product_AvailableQuantity CHECK (On_Hand_Quantity >= Reserved_Quantity)
);
GO

CREATE TABLE Product_Image (
                               Image_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                               Product_ID BIGINT NOT NULL,
                               Image_URL VARCHAR(MAX) NOT NULL,
    Is_Avatar BIT NOT NULL DEFAULT 0,

    CONSTRAINT FK_ProductImage_Product FOREIGN KEY (Product_ID) REFERENCES Product(Product_ID)
);
GO

/* =========================
   INVENTORY CŨ
   - GIỮ LẠI nếu team muốn tham chiếu schema cũ
   - Không nên dùng cho flow mới
   ========================= */
CREATE TABLE Inventory (
                           Inventory_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                           Product_ID BIGINT NOT NULL,
                           Quantity_Before INT NOT NULL,
                           Quantity_After INT NOT NULL,
                           User_ID BIGINT NOT NULL,
                           Supplier_ID BIGINT NOT NULL,
                           Order_Date DATETIME NULL,
                           Received_Date DATETIME NULL,
                           Unit NVARCHAR(20) NULL,

                           CONSTRAINT FK_Inventory_Product FOREIGN KEY (Product_ID) REFERENCES Product(Product_ID),
                           CONSTRAINT FK_Inventory_User FOREIGN KEY (User_ID) REFERENCES [User](User_ID),
                           CONSTRAINT FK_Inventory_Supplier FOREIGN KEY (Supplier_ID) REFERENCES Supplier(Supplier_ID)
);
GO

/* =========================
   INVENTORY MỚI
   ========================= */

CREATE TABLE Inventory_Receipt (
                                   Inventory_Receipt_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                                   Receipt_Code NVARCHAR(50) NOT NULL UNIQUE,

                                   Supplier_ID BIGINT NOT NULL,
                                   Created_By BIGINT NOT NULL,
                                   Approved_By BIGINT NULL,
                                   Received_By BIGINT NULL,

                                   Order_Date DATETIME NOT NULL DEFAULT GETDATE(),
                                   Received_Date DATETIME NULL,

                                   Status NVARCHAR(30) NOT NULL CONSTRAINT DF_InventoryReceipt_Status DEFAULT N'DRAFT',
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
GO

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
                                              FOREIGN KEY (Inventory_Receipt_ID)
                                                  REFERENCES Inventory_Receipt(Inventory_Receipt_ID),

                                          CONSTRAINT FK_InventoryReceiptDetail_Product
                                              FOREIGN KEY (Product_ID)
                                                  REFERENCES Product(Product_ID),

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
GO

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

                                       CONSTRAINT CK_InventoryTransaction_QtyBefore
                                           CHECK (Quantity_Before >= 0),

                                       CONSTRAINT CK_InventoryTransaction_QtyAfter
                                           CHECK (Quantity_After >= 0),

                                       CONSTRAINT CK_InventoryTransaction_QtyChange_NotZero
                                           CHECK (Quantity_Change <> 0),

                                       CONSTRAINT CK_InventoryTransaction_Balance
                                           CHECK (Quantity_After = Quantity_Before + Quantity_Change)
);
GO

/* =========================
   2) PRODUCT SUB-TYPES
   ========================= */

CREATE TABLE Frame (
                       Frame_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                       Product_ID BIGINT UNIQUE NOT NULL,
                       Color NVARCHAR(50) NULL,
                       Temple_Length DECIMAL(5,2) NULL,
                       Lens_Width DECIMAL(5,2) NULL,
                       Bridge_Width DECIMAL(5,2) NULL,
                       Frame_Shape_Name NVARCHAR(255) NULL,
                       Frame_Material_Name NVARCHAR(255) NULL,
                       Description NVARCHAR(255) NULL,

                       CONSTRAINT FK_Frame_Product FOREIGN KEY (Product_ID) REFERENCES Product(Product_ID)
);
GO

CREATE TABLE Lens_Type (
                           Lens_Type_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                           Type_Name NVARCHAR(50) NOT NULL,
                           Description NVARCHAR(255) NULL
);
GO

CREATE TABLE Lens (
                      Lens_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                      Product_ID BIGINT UNIQUE NOT NULL,
                      Lens_Type_ID BIGINT NOT NULL,
                      Index_Value DECIMAL(5,2) NULL,
                      Diameter DECIMAL(5,2) NULL,
                      Available_Power_Range NVARCHAR(200) NULL,
                      Is_Blue_Light_Block BIT NULL,
                      Is_Photochromic BIT NULL,
                      Description NVARCHAR(255) NULL,

                      CONSTRAINT FK_Lens_Product FOREIGN KEY (Product_ID) REFERENCES Product(Product_ID),
                      CONSTRAINT FK_Lens_LensType FOREIGN KEY (Lens_Type_ID) REFERENCES Lens_Type(Lens_Type_ID)
);
GO

CREATE TABLE Contact_Lens (
                              Contact_Lens_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                              Product_ID BIGINT UNIQUE NOT NULL,
                              Usage_Type NVARCHAR(50) NULL,
                              Base_Curve DECIMAL(5,2) NULL,
                              Diameter DECIMAL(5,2) NULL,
                              Water_Content DECIMAL(5,2) NULL,
                              Available_Power_Range NVARCHAR(200) NULL,
                              Quantity_Per_Box INT NULL,
                              Lens_Material NVARCHAR(50) NULL,
                              Replacement_Schedule NVARCHAR(50) NULL,
                              Color NVARCHAR(50) NULL,

                              CONSTRAINT FK_ContactLens_Product FOREIGN KEY (Product_ID) REFERENCES Product(Product_ID)
);
GO

/* =========================
   3) PROMOTION (NEW DESIGN)
   ========================= */

CREATE TABLE Promotion (
                           Promotion_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                           Promotion_Code NVARCHAR(50) NOT NULL UNIQUE,
                           Promotion_Name NVARCHAR(255) NOT NULL,

                           Promotion_Scope NVARCHAR(20) NOT NULL,
                           Discount_Type NVARCHAR(20) NOT NULL,
                           Discount_Value DECIMAL(15,2) NOT NULL,
                           Max_Discount_Value DECIMAL(15,2) NULL,

                           Start_Date DATETIME NOT NULL,
                           End_Date DATETIME NOT NULL,

                           Usage_Limit INT NULL,
                           Used_Count INT NOT NULL DEFAULT 0,
                           Is_Active BIT NOT NULL DEFAULT 1,
                           Description NVARCHAR(500) NULL,

                           CONSTRAINT CK_Promotion_Scope
                               CHECK (Promotion_Scope IN (N'ORDER', N'PRODUCT')),

                           CONSTRAINT CK_Promotion_Discount_Type
                               CHECK (Discount_Type IN (N'PERCENT', N'AMOUNT')),

                           CONSTRAINT CK_Promotion_Discount_Value
                               CHECK (Discount_Value > 0),

                           CONSTRAINT CK_Promotion_Max_Discount_Value
                               CHECK (Max_Discount_Value IS NULL OR Max_Discount_Value > 0),

                           CONSTRAINT CK_Promotion_Date
                               CHECK (Start_Date < End_Date),

                           CONSTRAINT CK_Promotion_Usage
                               CHECK (
                                   Used_Count >= 0
                                       AND (Usage_Limit IS NULL OR Usage_Limit > 0)
                                       AND (Usage_Limit IS NULL OR Used_Count <= Usage_Limit)
                                   )
);
GO

CREATE TABLE Promotion_Order_Rule (
                                      Promotion_Order_Rule_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                                      Promotion_ID BIGINT NOT NULL UNIQUE,
                                      Min_Order_Total DECIMAL(15,2) NOT NULL,

                                      CONSTRAINT FK_PromotionOrderRule_Promotion
                                          FOREIGN KEY (Promotion_ID) REFERENCES Promotion(Promotion_ID),

                                      CONSTRAINT CK_PromotionOrderRule_MinOrderTotal
                                          CHECK (Min_Order_Total > 0)
);
GO

CREATE TABLE Promotion_Product_Target (
                                          Promotion_Product_Target_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                                          Promotion_ID BIGINT NOT NULL,

                                          Target_Type NVARCHAR(30) NOT NULL,
                                          Product_ID BIGINT NULL,
                                          Product_Type_ID BIGINT NULL,
                                          Brand_ID BIGINT NULL,

                                          CONSTRAINT FK_PromotionProductTarget_Promotion
                                              FOREIGN KEY (Promotion_ID) REFERENCES Promotion(Promotion_ID),

                                          CONSTRAINT FK_PromotionProductTarget_Product
                                              FOREIGN KEY (Product_ID) REFERENCES Product(Product_ID),

                                          CONSTRAINT FK_PromotionProductTarget_ProductType
                                              FOREIGN KEY (Product_Type_ID) REFERENCES Product_Type(Product_Type_ID),

                                          CONSTRAINT FK_PromotionProductTarget_Brand
                                              FOREIGN KEY (Brand_ID) REFERENCES Brand(Brand_ID),

                                          CONSTRAINT CK_PromotionProductTarget_TargetType
                                              CHECK (
                                                  Target_Type IN (
                                                                  N'PRODUCT',
                                                                  N'PRODUCT_TYPE',
                                                                  N'BRAND',
                                                                  N'BRAND_PRODUCT_TYPE'
                                                      )
                                                  ),

                                          CONSTRAINT CK_PromotionProductTarget_TargetData
                                              CHECK (
                                                  (
                                                      Target_Type = N'PRODUCT'
                                                          AND Product_ID IS NOT NULL
                                                          AND Product_Type_ID IS NULL
                                                          AND Brand_ID IS NULL
                                                      )
                                                      OR
                                                  (
                                                      Target_Type = N'PRODUCT_TYPE'
                                                          AND Product_ID IS NULL
                                                          AND Product_Type_ID IS NOT NULL
                                                          AND Brand_ID IS NULL
                                                      )
                                                      OR
                                                  (
                                                      Target_Type = N'BRAND'
                                                          AND Product_ID IS NULL
                                                          AND Product_Type_ID IS NULL
                                                          AND Brand_ID IS NOT NULL
                                                      )
                                                      OR
                                                  (
                                                      Target_Type = N'BRAND_PRODUCT_TYPE'
                                                          AND Product_ID IS NULL
                                                          AND Product_Type_ID IS NOT NULL
                                                          AND Brand_ID IS NOT NULL
                                                      )
                                                  )
);
GO

/* =========================
   4) ORDER + SHIPPING + PAYMENT + INVOICE
   ========================= */

CREATE TABLE [Order] (
                         Order_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
    User_ID BIGINT NOT NULL,
    Promotion_ID BIGINT NULL,

    Order_Code NVARCHAR(50) UNIQUE NULL,
    Order_Date DATETIME NOT NULL DEFAULT GETDATE(),

    Sub_Total DECIMAL(15,2) NOT NULL,
    Tax_Amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    Discount_Amount DECIMAL(15,2) NOT NULL DEFAULT 0,

    Shipping_Fee DECIMAL(15,2) NOT NULL DEFAULT 0,
    Total_Amount AS (Sub_Total + Tax_Amount - Discount_Amount + Shipping_Fee),

    Order_Type NVARCHAR(20) NOT NULL,
    Order_Status NVARCHAR(30) NOT NULL,

    CONSTRAINT FK_Order_User FOREIGN KEY (User_ID) REFERENCES [User](User_ID),
    CONSTRAINT FK_Order_Promotion FOREIGN KEY (Promotion_ID) REFERENCES Promotion(Promotion_ID),

    CONSTRAINT CK_Order_Status CHECK (Order_Status IN (
                                      N'PENDING', N'CONFIRMED', N'PARTIALLY_PAID', N'PAID',
                                      N'PROCESSING', N'READY', N'COMPLETED', N'CANCELED', N'RETURNED'
                                                      )),

    CONSTRAINT CK_Order_Type CHECK (Order_Type IN (
                                    N'DIRECT_ORDER', N'PRE_ORDER', N'PRESCRIPTION_ORDER', N'MIX_ORDER'
                                                  ))
    );
GO

CREATE TABLE Shipping_Info (
                               Shipping_Info_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                               Order_ID BIGINT NOT NULL UNIQUE,

                               Recipient_Name NVARCHAR(100) NOT NULL,
                               Recipient_Phone VARCHAR(15) NOT NULL,
                               Recipient_Email VARCHAR(100) NULL,

                               Recipient_Address NVARCHAR(255) NOT NULL,
                               Note NVARCHAR(MAX) NULL,

                               Province_Code INT NULL,
                               Province_Name NVARCHAR(100) NULL,
                               District_Code INT NULL,
                               District_Name NVARCHAR(100) NULL,

                               Ward_Code NVARCHAR(20) NULL,
                               Ward_Name NVARCHAR(100) NULL,

                               Shipping_Method NVARCHAR(30) NOT NULL,
                               Shipping_Fee DECIMAL(15,2) NOT NULL CONSTRAINT DF_ShippingFee DEFAULT 0,

                               Shipping_Status NVARCHAR(30) NOT NULL CONSTRAINT DF_ShippingStatus DEFAULT N'PENDING',
                               Expected_Delivery_At DATETIME2(0) NULL,

                               CONSTRAINT FK_ShippingInfo_Order FOREIGN KEY (Order_ID) REFERENCES [Order](Order_ID),

                               CONSTRAINT CK_Shipping_Status CHECK (Shipping_Status IN (
                                                                                        N'PENDING', N'PACKING', N'SHIPPING', N'DELIVERED',
                                                                                        N'FAILED', N'CANCELED', N'RETURNED'
                                   )),

                               CONSTRAINT CK_Shipping_Province_Pair CHECK (
                                   (Province_Code IS NULL AND Province_Name IS NULL)
                                       OR (Province_Code IS NOT NULL AND Province_Name IS NOT NULL)
                                   ),

                               CONSTRAINT CK_Shipping_District_Pair CHECK (
                                   (District_Code IS NULL AND District_Name IS NULL)
                                       OR (District_Code IS NOT NULL AND District_Name IS NOT NULL)
                                   ),

                               CONSTRAINT CK_Shipping_Ward_Pair CHECK (
                                   (Ward_Code IS NULL AND Ward_Name IS NULL)
                                       OR (Ward_Code IS NOT NULL AND Ward_Name IS NOT NULL)
                                   ),

                               CONSTRAINT CK_Shipping_Admin_Hierarchy CHECK (
                                   (Ward_Code IS NULL OR District_Code IS NOT NULL)
                                       AND (District_Code IS NULL OR Province_Code IS NOT NULL)
                                   )
);
GO

CREATE TABLE Payment (
                         Payment_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                         Order_ID BIGINT NOT NULL,

                         Payment_Purpose NVARCHAR(20) NOT NULL,
                         Created_At DATETIME NOT NULL DEFAULT GETDATE(),
                         Payment_Date DATETIME NULL,

                         Payment_Method NVARCHAR(20) NOT NULL,
                         Amount DECIMAL(15,2) NOT NULL,

                         Status NVARCHAR(20) NOT NULL,

                         CONSTRAINT FK_Payment_Order FOREIGN KEY (Order_ID) REFERENCES [Order](Order_ID),

                         CONSTRAINT CK_Payment_Status CHECK (Status IN (
                                                                        N'PENDING', N'SUCCESS', N'FAILED', N'REFUNDED', N'CANCELED'
                             )),
                         CONSTRAINT CK_Payment_Amount CHECK (Amount > 0),
                         CONSTRAINT CK_Payment_Method CHECK (Payment_Method IN (
                                                                                N'COD', N'MOMO', N'VNPAY', N'PAYOS'
                             )),
                         CONSTRAINT CK_Payment_Purpose CHECK (Payment_Purpose IN (
                                                                                  N'DEPOSIT', N'FULL', N'REMAINING'
                             )),
                         CONSTRAINT CK_Payment_Date_By_Status CHECK (
                             (Status IN (N'PENDING', N'CANCELED') AND Payment_Date IS NULL)
                                 OR (Status IN (N'SUCCESS', N'FAILED', N'REFUNDED'))
                             ),
                         CONSTRAINT CK_Payment_Deposit_Method CHECK (
                             NOT (Payment_Purpose = N'DEPOSIT' AND Payment_Method = N'COD')
                             )
);
GO

CREATE TABLE Invoice (
                         Invoice_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                         Order_ID BIGINT NOT NULL UNIQUE,
                         Issue_Date DATETIME NOT NULL DEFAULT GETDATE(),
                         Total_Amount DECIMAL(15,2) NOT NULL,
                         Status NVARCHAR(20) NOT NULL,

                         CONSTRAINT FK_Invoice_Order FOREIGN KEY (Order_ID) REFERENCES [Order](Order_ID),
                         CONSTRAINT CK_Invoice_Status CHECK (Status IN (
                                                                        N'UNPAID', N'PARTIALLY_PAID', N'PAID', N'CANCELED'
                             ))
);
GO

CREATE TABLE Order_Detail (
                              Order_Detail_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                              Order_ID BIGINT NOT NULL,
                              Product_ID BIGINT NOT NULL,
                              Unit_Price DECIMAL(15,2) NOT NULL,
                              Note NVARCHAR(500) NULL,
                              Quantity INT NOT NULL,

                              CONSTRAINT FK_OrderDetail_Order FOREIGN KEY (Order_ID) REFERENCES [Order](Order_ID),
                              CONSTRAINT FK_OrderDetail_Product FOREIGN KEY (Product_ID) REFERENCES Product(Product_ID)
);
GO

CREATE TABLE Order_Processing (
                                  Order_Processing_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                                  Order_ID BIGINT NOT NULL,
                                  Changed_By BIGINT NOT NULL,
                                  Changed_At DATETIME NOT NULL DEFAULT GETDATE(),
                                  Note NVARCHAR(255) NULL,

                                  CONSTRAINT FK_OrderProcessing_Order FOREIGN KEY (Order_ID) REFERENCES [Order](Order_ID),
                                  CONSTRAINT FK_OrderProcessing_User FOREIGN KEY (Changed_By) REFERENCES [User](User_ID)
);
GO

CREATE TABLE Return_Exchange (
                                 Return_Exchange_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                                 Order_ID BIGINT NOT NULL,
                                 User_ID BIGINT NOT NULL,
                                 Return_Code NVARCHAR(50) UNIQUE NOT NULL,
                                 Request_Date DATETIME NOT NULL DEFAULT GETDATE(),
                                 Quantity INT NOT NULL,
                                 Return_Reason NVARCHAR(500) NULL,
                                 Product_Condition NVARCHAR(50) NULL,
                                 Refund_Amount DECIMAL(15,2) NULL,
                                 Refund_Method NVARCHAR(50) NULL,
                                 Refund_Account_Number NVARCHAR(50) NULL,
                                 Status NVARCHAR(30) NOT NULL,
                                 Approved_By BIGINT NULL,
                                 Approved_Date DATETIME NULL,
                                 Reject_Reason NVARCHAR(500) NULL,
                                 Image_URL NVARCHAR(500) NULL,
                                 Return_Type NVARCHAR(20) NOT NULL,
                                 Request_Scope NVARCHAR(10) NOT NULL DEFAULT N'ITEM',

                                 CONSTRAINT FK_Return_Order FOREIGN KEY (Order_ID) REFERENCES [Order](Order_ID),
                                 CONSTRAINT FK_Return_User FOREIGN KEY (User_ID) REFERENCES [User](User_ID),
                                 CONSTRAINT FK_Return_ApprovedBy FOREIGN KEY (Approved_By) REFERENCES [User](User_ID),

                                 CONSTRAINT CK_Return_Status CHECK (Status IN (
                                                                               N'PENDING', N'APPROVED', N'REJECTED', N'COMPLETED'
                                     )),
                                 CONSTRAINT CK_Return_Quantity CHECK (Quantity > 0),
                                 CONSTRAINT CK_Return_Refund_Amount CHECK (Refund_Amount IS NULL OR Refund_Amount >= 0),
                                 CONSTRAINT CK_Return_Refund_Method CHECK (Refund_Method IS NULL OR Refund_Method IN (
                                                                                                                      N'CASH', N'MOMO', N'VNPAY', N'PAYOS', N'BANK_TRANSFER'
                                     )),
                                 CONSTRAINT CK_Return_Product_Condition CHECK (Product_Condition IS NULL OR Product_Condition IN (
                                                                                                                                  N'NEW', N'USED', N'DAMAGED'
                                     )),
                                 CONSTRAINT CK_Return_Type CHECK (Return_Type IN (
                                                                                  N'WARRANTY', N'RETURN', N'REFUND'
                                     )),
                                 CONSTRAINT CK_Return_Request_Scope CHECK (Request_Scope IN (N'ITEM', N'ORDER')),
                                 CONSTRAINT CK_Return_Scope_By_Type CHECK (
                                     (Return_Type = N'REFUND' AND Request_Scope = N'ORDER')
                                         OR (Return_Type IN (N'WARRANTY', N'RETURN') AND Request_Scope = N'ITEM')
                                     ),
                                 CONSTRAINT CK_Return_Refund_Required_For_Refund CHECK (
                                     Return_Type <> N'REFUND'
                                         OR (Refund_Amount IS NOT NULL AND Refund_Amount > 0 AND Refund_Method IS NOT NULL)
                                     )
);
GO

CREATE TABLE Return_Exchange_Item (
                                      Return_Exchange_Item_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                                      Return_Exchange_ID BIGINT NOT NULL,
                                      Order_Detail_ID BIGINT NOT NULL,
                                      Quantity INT NOT NULL,
                                      Note NVARCHAR(500) NULL,

                                      CONSTRAINT FK_ReturnItem_ReturnExchange FOREIGN KEY (Return_Exchange_ID)
                                          REFERENCES Return_Exchange(Return_Exchange_ID),

                                      CONSTRAINT FK_ReturnItem_OrderDetail FOREIGN KEY (Order_Detail_ID)
                                          REFERENCES Order_Detail(Order_Detail_ID),

                                      CONSTRAINT CK_ReturnItem_Quantity CHECK (Quantity > 0),
                                      CONSTRAINT UQ_ReturnItem_ReturnExchange_OrderDetail UNIQUE (Return_Exchange_ID, Order_Detail_ID)
);
GO

/* =========================
   5) PRESCRIPTION
   ========================= */

CREATE TABLE Prescription_Order (
                                    Prescription_Order_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                                    Order_ID BIGINT UNIQUE NOT NULL,
                                    User_ID BIGINT NOT NULL,
                                    Prescription_Date DATETIME NOT NULL,
                                    Note NVARCHAR(500) NULL,
                                    Complete_Date DATE NULL,

                                    CONSTRAINT FK_Prescription_Order FOREIGN KEY (Order_ID) REFERENCES [Order](Order_ID),
                                    CONSTRAINT FK_Prescription_User FOREIGN KEY (User_ID) REFERENCES [User](User_ID)
);
GO

CREATE TABLE Prescription_Order_Detail (
                                           Prescription_Order_Detail_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
                                           Prescription_Order_ID BIGINT NOT NULL,
                                           Frame_ID BIGINT NULL,
                                           Lens_ID BIGINT NULL,

                                           Right_Eye_Sph DECIMAL(5,2) NULL,
                                           Right_Eye_Cyl DECIMAL(5,2) NULL,
                                           Right_Eye_Axis INT NULL,
                                           Left_Eye_Sph DECIMAL(5,2) NULL,
                                           Left_Eye_Cyl DECIMAL(5,2) NULL,
                                           Left_Eye_Axis INT NULL,
                                           PD DECIMAL(4,1) NULL,
                                           PD_Right DECIMAL(4,1) NULL,
                                           PD_Left DECIMAL(4,1) NULL,

                                           Sub_Total DECIMAL(15,2) NOT NULL,

                                           CONSTRAINT FK_PrescriptionDetail_Order FOREIGN KEY (Prescription_Order_ID)
                                               REFERENCES Prescription_Order(Prescription_Order_ID),
                                           CONSTRAINT FK_PrescriptionDetail_Frame FOREIGN KEY (Frame_ID)
                                               REFERENCES Frame(Frame_ID),
                                           CONSTRAINT FK_PrescriptionDetail_Lens FOREIGN KEY (Lens_ID)
                                               REFERENCES Lens(Lens_ID)
);
GO

/* =========================
   6) CART
   ========================= */

CREATE TABLE dbo.Cart (
                          Cart_ID BIGINT IDENTITY(1,1) NOT NULL,
                          User_ID BIGINT NOT NULL,
                          Created_At DATETIME NOT NULL DEFAULT GETDATE(),
                          Updated_At DATETIME NOT NULL DEFAULT GETDATE(),

                          CONSTRAINT PK_Cart PRIMARY KEY (Cart_ID),
                          CONSTRAINT FK_Cart_User FOREIGN KEY (User_ID) REFERENCES dbo.[User](User_ID),
                          CONSTRAINT UQ_Cart_User UNIQUE (User_ID)
);
GO

CREATE TABLE dbo.Cart_Item (
                               Cart_Item_ID BIGINT IDENTITY(1,1) NOT NULL,
                               Cart_ID BIGINT NOT NULL,

                               Product_ID BIGINT NULL,
                               Frame_ID BIGINT NULL,
                               Lens_ID BIGINT NULL,

                               Quantity INT NOT NULL DEFAULT 1,

                               Frame_Price DECIMAL(18,2) NULL,
                               Lens_Price DECIMAL(18,2) NULL,
                               Price DECIMAL(18,2) NULL,

                               CONSTRAINT PK_Cart_Item PRIMARY KEY (Cart_Item_ID),

                               CONSTRAINT FK_CartItem_Cart FOREIGN KEY (Cart_ID) REFERENCES dbo.Cart(Cart_ID),
                               CONSTRAINT FK_CartItem_Product FOREIGN KEY (Product_ID) REFERENCES dbo.Product(Product_ID),
                               CONSTRAINT FK_CartItem_Frame FOREIGN KEY (Frame_ID) REFERENCES dbo.Frame(Frame_ID),
                               CONSTRAINT FK_CartItem_Lens FOREIGN KEY (Lens_ID) REFERENCES dbo.Lens(Lens_ID),

                               CONSTRAINT DF_CartItem_Quantity DEFAULT 1 FOR Quantity
);
GO

CREATE TABLE dbo.Cart_Item_Prescription (
                                            Prescription_ID BIGINT IDENTITY(1,1) NOT NULL,
                                            Cart_Item_ID BIGINT NOT NULL,

                                            Right_Eye_Sph DECIMAL(5,2) NULL,
                                            Right_Eye_Cyl DECIMAL(5,2) NULL,
                                            Right_Eye_Axis INT NULL,
                                            Right_Eye_Add DECIMAL(5,2) NULL,

                                            Left_Eye_Sph DECIMAL(5,2) NULL,
                                            Left_Eye_Cyl DECIMAL(5,2) NULL,
                                            Left_Eye_Axis INT NULL,
                                            Left_Eye_Add DECIMAL(5,2) NULL,

                                            PD DECIMAL(4,1) NULL,
                                            PD_Right DECIMAL(4,1) NULL,
                                            PD_Left DECIMAL(4,1) NULL,

                                            CONSTRAINT PK_Cart_Item_Prescription PRIMARY KEY (Prescription_ID),
                                            CONSTRAINT FK_CartItemPrescription_CartItem FOREIGN KEY (Cart_Item_ID) REFERENCES dbo.Cart_Item(Cart_Item_ID),
                                            CONSTRAINT UQ_CartItemPrescription_CartItem UNIQUE (Cart_Item_ID)
);
GO

/* =========================
   8) SEED DATA
   ========================= */

-- ✅ Seed Role (để insert User không lỗi FK)
INSERT INTO Role (Type_Name) VALUES
(N'CUSTOMER'),
(N'ADMIN'),
(N'MANAGER'),
(N'SALES STAFF'),
(N'OPERATIONS STAFF');
GO

INSERT INTO [User]
(Username, Password, Email, Phone, Role_ID, Status, Name, Address, Date_of_Birth, ID_Number)
VALUES
(N'customer01', N'123456', 'customer01@gmail.com','0901112222', 1, 1, N'Nguyễn Văn A', N'Quận 1, TP.HCM', '1998-05-10', '0123456789'),
(N'annguyen', N'annguyen123', 'annguyen@gmail.com','0123456789', 2, 1, N'Ân Nguyễn', N'Landmark 81, Quận 1, TP.HCM', '1990-02-15', '012345678901'),
(N'huyvu', N'huyvu123', 'huyvu@gmail.com', '0123456788', 2, 1, N'Huy Vũ', N'Landmark 82, Quận 1, TP.HCM', '1980-02-15', '012345678902'),
(N'quangnhat', N'quangnnhat123', 'quangnhat@gmail.com','0123456787', 5, 1, N'Quang Trịnh',   N'Phú Mỹ Hưng, Quận 7, TP.HCM', '2005-08-20', '012345678903'),
(N'phatvo', N'phatvo123', 'phatvo@gmail.com','0123456786', 3, 1, N'Phát Võ',   N'Thảo Điền, Quận 9, TP.HCM', '2004-08-20', '012345678904'),
(N'kienpham', N'kienpham123', 'kienpham@gmail.com','0123456785', 4, 1, N'Kiên Phạm', N'Thảo Điền, Quận 9, TP.HCM', '2003-08-20', '012345678905');
GO

INSERT INTO Brand (Brand_Name, Description, Logo_URL, Status) VALUES
(N'Ray-Ban', N'Thương hiệu kính nổi tiếng của Mỹ', NULL, 1),
(N'Oakley', N'Kính thể thao cao cấp', NULL, 1),
(N'Gucci', N'Thương hiệu thời trang xa xỉ', NULL, 1),
(N'Prada', N'Thương hiệu thời trang cao cấp', NULL, 1),
(N'Gentle Monster', N'Thương hiệu kính Hàn Quốc', NULL, 1),
(N'Essilor', N'Hãng tròng kính Pháp', NULL, 1),
(N'HOYA', N'Hãng tròng kính Nhật Bản', NULL, 1),
(N'Acuvue', N'Thương hiệu kính áp tròng', NULL, 1);
GO

INSERT INTO Product_Type (Type_Name, Description) VALUES
(N'Gọng Kính', N'Gọng kính'),
(N'Tròng Kính', N'Tròng kính'),
(N'Kính Áp Tròng', N'Kính áp tròng');
GO

INSERT INTO Product (Product_Name, SKU, Product_Type_ID, Brand_ID, Price, Cost_Price, Allow_Preorder, Description) VALUES
-- Frames (Product_ID dự kiến: 1..6)
(N'Ray-Ban Aviator Classic RB3025', N'RB-AVI-3025', 1, 1, 4500000, 3200000, 0, N'Gọng kính phi công cổ điển, chất liệu kim loại cao cấp'),
(N'Ray-Ban Wayfarer RB2140', N'RB-WAY-2140', 1, 1, 4200000, 3000000, 0, N'Gọng kính phong cách retro'),
(N'Oakley Flak 2.0 XL', N'OAK-FLK-20XL', 1, 2, 5800000, 4100000, 1, N'Gọng kính thể thao chuyên nghiệp, siêu nhẹ'),
(N'Gucci GG0061O', N'GUC-GG0061O', 1, 3, 12000000, 8500000, 1, N'Gọng kính sang trọng với logo Gucci nổi bật'),
(N'Prada VPR 16M', N'PRA-VPR16M', 1, 4, 11500000, 8200000, 0, N'Gọng kính thời trang cao cấp, thiết kế thanh lịch'),
(N'Gentle Monster VACANCES', N'GM-VACANCES', 1, 5, 6500000, 4800000, 0, N'Gọng kính oversize phong cách Hàn Quốc'),

-- Lenses (Product_ID dự kiến: 7..10)
(N'Essilor Crizal Sapphire UV', N'ESS-CRI-SAPH', 2, 6, 3200000, 2100000, 0, N'Tròng kính chống phản chiếu cao cấp, chống tia UV'),
(N'Essilor Varilux X Series', N'ESS-VAR-X', 2, 6, 8500000, 5800000, 0, N'Tròng kính đa tròng thế hệ mới, chuyển tiêu mượt mà'),
(N'HOYA BlueControl', N'HOY-BLC-001', 2, 7, 2800000, 1900000, 0, N'Tròng kính lọc ánh sáng xanh từ màn hình'),
(N'HOYA Sensity', N'HOY-SEN-001', 2, 7, 4500000, 3100000, 0, N'Tròng kính đổi màu thông minh'),

-- Contact Lenses (Product_ID dự kiến: 11..12)
(N'Acuvue Oasys 1-Day', N'ACU-OAS-1D30', 3, 8, 450000, 320000, 0, N'Kính áp tròng ngày, hộp 30 miếng, độ ẩm cao'),
(N'Acuvue Oasys 2-Week', N'ACU-OAS-2W6', 3, 8, 580000, 410000, 0, N'Kính áp tròng 2 tuần, hộp 6 miếng, công nghệ Hydraclear Plus');
GO

/* ✅ SỬA SEED FRAME:
   Trước bạn insert Frame(Product_ID) = 2..7 => lệch.
   Vì 6 frame nằm ở Product_ID = 1..6.
*/
INSERT INTO Frame (Product_ID, Color, Temple_Length, Lens_Width, Bridge_Width, Frame_Shape_Name, Frame_Material_Name, Description) VALUES
(1, N'Vàng Gold', 140.00, 58.00, 14.00, N'Tròn', N'Kim loại', N'Gọng kính phi công cổ điển màu vàng gold'),
(2, N'Đen Bóng', 150.00, 50.00, 22.00, N'Vuông', N'Nhựa', N'Gọng kính vuông màu đen bóng phong cách retro'),
(3, N'Đen Nhám', 133.00, 59.00, 12.00, N'Đa Giác', N'Titan', N'Gọng kính thể thao siêu nhẹ màu đen nhám'),
(4, N'Nâu Havana', 140.00, 53.00, 18.00, N'Mắt Mèo', N'Nhựa', N'Gọng kính mắt mèo sang trọng màu nâu havana'),
(5, N'Đỏ Burgundy', 135.00, 52.00, 17.00, N'Oval', N'Kim loại + Nhựa', N'Gọng kính oval màu đỏ burgundy thanh lịch'),
(6, N'Trắng Trong Suốt', 145.00, 56.00, 20.00, N'Oversized', N'Nhựa', N'Gọng kính oversized trong suốt phong cách Hàn Quốc');
GO

INSERT INTO Lens_Type (Type_Name, Description) VALUES
(N'Đơn tròng', N'Tròng kính đơn tròng (Single Vision) ...'),
(N'Đa tròng', N'Kính đa tròng (Progressive) ...'),
(N'Hai tròng', N'Kính hai tròng ...');
GO

INSERT INTO Lens (Product_ID, Lens_Type_ID, Index_Value, Diameter, Available_Power_Range, Is_Blue_Light_Block, Is_Photochromic, Description) VALUES
(7, 1, 1.67, 65.00, N'-10.00 đến +6.00', 0, 0, N'Tròng kính đơn tròng chống phản chiếu cao cấp'),
(8, 2, 1.67, 65.00, N'+0.75 đến +3.50', 0, 0, N'Tròng kính đa tròng thế hệ mới'),
(9, 3, 1.60, 70.00, N'-8.00 đến +4.00', 1, 0, N'Tròng kính hai tròng lọc ánh sáng xanh hiệu quả');
GO

INSERT INTO Contact_Lens (Product_ID, Usage_Type, Base_Curve, Diameter, Water_Content, Available_Power_Range, Quantity_Per_Box, Lens_Material, Replacement_Schedule, Color) VALUES
(11, N'Thể thao', 8.50, 14.30, 38.00, N'-12.00 đến +6.00', 30, N'Senofilcon A', N'1 Ngày', N'Trong Suốt'),
(12, N'Làm việc văn phòng', 8.40, 14.00, 38.00, N'-9.00 đến +6.00', 6, N'Senofilcon A', N'2 Tuần', N'Trong Suốt');
GO
