package com.swp391.eyewear_management_backend.mapper;

import com.swp391.eyewear_management_backend.dto.response.InventoryReceiptConformResponse;
import com.swp391.eyewear_management_backend.dto.response.InventoryReceiptDetailResponse;
import com.swp391.eyewear_management_backend.dto.response.InventoryReceiptResponse;
import com.swp391.eyewear_management_backend.entity.InventoryReceipt;
import com.swp391.eyewear_management_backend.entity.InventoryReceiptDetail;
import com.swp391.eyewear_management_backend.entity.Product;
import com.swp391.eyewear_management_backend.entity.ProductImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface InventoryReceiptMapper {

    // CHÚ Ý: Bạn cần xem lại Entity Supplier và User của bạn khai báo các cột tên là gì
    // Ví dụ dưới đây giả định Supplier có trường 'name', User có trường 'id' và 'fullName' / 'username'

    @Mapping(source = "supplier.supplierID", target = "supplierId") // Tùy thuộc entity Supplier dùng id hay supplierID
    @Mapping(source = "supplier.supplierName", target = "supplierName") // Thay "name" bằng "supplierName" nếu entity của bạn đặt thế

    @Mapping(source = "createdBy.userId", target = "createdById") // Tùy thuộc entity User dùng id hay userID
    @Mapping(source = "createdBy.username", target = "createdByName") // Thay bằng "fullName" nếu muốn trả về tên thật

    @Mapping(source = "inventoryReceiptID", target = "inventoryReceiptId") // Tùy entity InventoryReceipt dùng id hay receiptID
    InventoryReceiptResponse toResponse(InventoryReceipt receipt);

    //-------------------------------------------------------------------------------------------------------------------------------------
    // --- 1. MAP TỪ INVENTORY_RECEIPT SANG CONFORM_RESPONSE ---
    @Mapping(source = "inventoryReceiptID", target = "inventoryReceiptId") // Đổi "id" thành tên khóa chính thực tế của bảng InventoryReceipt nếu khác

    // Map thông tin Supplier (Tránh bị null)
    @Mapping(source = "supplier.supplierID", target = "supplierId") // Giả sử entity Supplier dùng supplierID
    @Mapping(source = "supplier.supplierName", target = "supplierName")     // Đổi "name" thành "supplierName" nếu Entity của bạn viết vậy
    @Mapping(source = "supplier.supplierPhone", target = "supplierPhone")
    @Mapping(source = "supplier.supplierAddress", target = "supplierAddress")

    // Map thông tin Người tạo (User)
    @Mapping(source = "createdBy.userId", target = "createdById")       // Giả sử entity User dùng id
    @Mapping(source = "createdBy.username", target = "createdByName")

    // Map danh sách chi tiết
    @Mapping(source = "details", target = "details")
    InventoryReceiptConformResponse toConformResponse(InventoryReceipt receipt);


    // --- 2. MAP TỪ INVENTORY_RECEIPT_DETAIL SANG DETAIL_RESPONSE ---
    @Mapping(source = "inventoryReceiptDetailID", target = "receiptDetailId")
    @Mapping(source = "product.productID", target = "productId")
    @Mapping(source = "product.productName", target = "productName")

    // Gọi hàm custom bên dưới để lấy ảnh Avatar
    @Mapping(source = ".", target = "vatRate", qualifiedByName = "calculateVatRate")
    @Mapping(source = "product", target = "productImage", qualifiedByName = "getAvatarFromProduct")
    InventoryReceiptDetailResponse toDetailResponse(InventoryReceiptDetail detail);


    // --- 3. HÀM HỖ TRỢ LẤY ẢNH AVATAR TỪ PRODUCT ---
    @Named("getAvatarFromProduct")
    default String getAvatarFromProduct(Product product) {
        if (product == null || product.getImages() == null || product.getImages().isEmpty()) {
            return "default-placeholder.png"; // Ảnh mặc định nếu ko có ảnh
        }
        for (ProductImage image : product.getImages()) {
            // Tìm ảnh được set làm avatar (isAvatar = true)
            if (Boolean.TRUE.equals(image.getAvatar())) {
                return image.getImageUrl();
            }
        }
        return "default-placeholder.png";
    }

    @Named("calculateVatRate")
    default BigDecimal calculateVatRate(InventoryReceiptDetail detail) {
        if (detail == null
                || detail.getUnitCost() == null
                || detail.getOrderedQuantity() == null
                || detail.getTotalPrice() == null
                || detail.getOrderedQuantity() <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal subTotal = detail.getUnitCost().multiply(BigDecimal.valueOf(detail.getOrderedQuantity()));
        if (subTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal vatAmount = detail.getTotalPrice().subtract(subTotal);
        if (vatAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return vatAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(subTotal, 2, RoundingMode.HALF_UP);
    }
}