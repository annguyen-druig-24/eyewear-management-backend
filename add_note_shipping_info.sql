IF COL_LENGTH('Shipping_Info', 'Note') IS NULL
BEGIN
    ALTER TABLE Shipping_Info
    ADD Note NVARCHAR(MAX) NULL;
END
GO
