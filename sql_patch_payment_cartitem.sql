IF OBJECT_ID('Payment_CartItem', 'U') IS NULL
BEGIN
    CREATE TABLE Payment_CartItem (
        Payment_CartItem_ID BIGINT IDENTITY(1,1) PRIMARY KEY,
        Payment_ID BIGINT NOT NULL,
        Cart_Item_ID BIGINT NOT NULL,
        Created_At DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_PaymentCartItem_Payment FOREIGN KEY (Payment_ID) REFERENCES Payment(Payment_ID),
        CONSTRAINT UQ_PaymentCartItem UNIQUE (Payment_ID, Cart_Item_ID)
    );
    CREATE INDEX IX_PaymentCartItem_CartItem ON Payment_CartItem (Cart_Item_ID);
END
GO
