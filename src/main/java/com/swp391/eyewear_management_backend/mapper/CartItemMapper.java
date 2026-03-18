package com.swp391.eyewear_management_backend.mapper;

import com.swp391.eyewear_management_backend.dto.response.CartItemResponse;
import com.swp391.eyewear_management_backend.dto.response.PrescriptionResponse;
import com.swp391.eyewear_management_backend.entity.CartItem;
import com.swp391.eyewear_management_backend.entity.CartItemPrescription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CartItemMapper {

    @Mapping(source = "cartItem.cartItemId", target = "cartItemId")
    @Mapping(source = "cartItem.cart.cartId", target = "cartId")
    @Mapping(source = "cartItem.itemType", target = "itemType")

    // --- Contact Lens Mapping ---
    @Mapping(source = "cartItem.contactLens.contactLensID", target = "contactLensId")
    @Mapping(source = "cartItem.contactLens", target = "contactLensName", qualifiedByName = "getContactLensName")
    @Mapping(source = "cartItem.contactLens", target = "contactLensPrice", qualifiedByName = "getContactLensPrice")
    @Mapping(source = "cartItem.contactLens", target = "contactLensImg", qualifiedByName = "getContactLensImg")
    @Mapping(source = "cartItem.contactLens", target = "contactLensAvailableQuantity", qualifiedByName = "getContactLensQuantity")
    @Mapping(source = "cartItem.contactLens", target = "contactLensPreorder", qualifiedByName = "getContactLensPreorder")

    // --- Frame Mapping ---
    @Mapping(source = "cartItem.frame.frameID", target = "frameId")
    @Mapping(source = "cartItem.frame", target = "frameName", qualifiedByName = "getFrameName")
    @Mapping(source = "cartItem.framePrice", target = "framePrice")
    @Mapping(source = "cartItem.frame", target = "frameImg", qualifiedByName = "getFrameImg")
    @Mapping(source = "cartItem.frame", target = "frameAvailableQuantity", qualifiedByName = "getFrameQuantity")
    @Mapping(source = "cartItem.frame", target = "framePreorder", qualifiedByName = "getFramePreorder")

    // --- Lens Mapping ---
    @Mapping(source = "cartItem.lens.lensID", target = "lensId")
    @Mapping(source = "cartItem.lens", target = "lensName", qualifiedByName = "getLensName")
    @Mapping(source = "cartItem.lensPrice", target = "lensPrice")
    @Mapping(source = "cartItem.lens", target = "lensImg", qualifiedByName = "getLensImg")
    @Mapping(source = "cartItem.lens", target = "lensAvailableQuantity", qualifiedByName = "getLensQuantity")
    @Mapping(source = "cartItem.lens", target = "lensPreorder", qualifiedByName = "getLensPreorder")

    @Mapping(source = "cartItem.quantity", target = "quantity")
    @Mapping(source = "cartItem.price", target = "price")
    @Mapping(source = "cartItem", target = "prescription", qualifiedByName = "mapPrescription")
    CartItemResponse toCartItemResponse(CartItem cartItem);

    // ==========================================
    // CONTACT LENS METHODS
    // ==========================================

    @Named("getContactLensName")
    default String getContactLensName(com.swp391.eyewear_management_backend.entity.ContactLens contactLens) {
        if (contactLens == null || contactLens.getProduct() == null) {
            return null;
        }
        return contactLens.getProduct().getProductName();
    }

    @Named("getContactLensPrice")
    default Double getContactLensPrice(com.swp391.eyewear_management_backend.entity.ContactLens contactLens) {
        if (contactLens == null || contactLens.getProduct() == null || contactLens.getProduct().getPrice() == null) {
            return null;
        }
        return ((Number) contactLens.getProduct().getPrice()).doubleValue();
    }

    @Named("getContactLensImg")
    default String getContactLensImg(com.swp391.eyewear_management_backend.entity.ContactLens contactLens) {
        if (contactLens == null || contactLens.getProduct() == null || contactLens.getProduct().getImages() == null) {
            return null;
        }
        return contactLens.getProduct().getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getAvatar()))
                .map(com.swp391.eyewear_management_backend.entity.ProductImage::getImageUrl)
                .findFirst()
                .orElse(null);
    }

    @Named("getContactLensQuantity")
    default Integer getContactLensQuantity(com.swp391.eyewear_management_backend.entity.ContactLens contactLens) {
        if (contactLens == null || contactLens.getProduct() == null || contactLens.getProduct().getAvailableQuantity() == null) {
            return null;
        }
        return contactLens.getProduct().getAvailableQuantity();
    }

    @Named("getContactLensPreorder")
    default Boolean getContactLensPreorder(com.swp391.eyewear_management_backend.entity.ContactLens contactLens) {
        if (contactLens == null || contactLens.getProduct() == null || contactLens.getProduct().getAllowPreorder() == null) {
            return null;
        }
        return contactLens.getProduct().getAllowPreorder();
    }

    // ==========================================
    // FRAME METHODS
    // ==========================================

    @Named("getFrameName")
    default String getFrameName(com.swp391.eyewear_management_backend.entity.Frame frame) {
        if (frame == null || frame.getProduct() == null) {
            return null;
        }
        return frame.getProduct().getProductName();
    }

    @Named("getFrameImg")
    default String getFrameImg(com.swp391.eyewear_management_backend.entity.Frame frame) {
        if (frame == null || frame.getProduct() == null || frame.getProduct().getImages() == null) {
            return null;
        }
        return frame.getProduct().getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getAvatar()))
                .map(com.swp391.eyewear_management_backend.entity.ProductImage::getImageUrl)
                .findFirst()
                .orElse(null);
    }

    @Named("getFrameQuantity")
    default Integer getFrameQuantity(com.swp391.eyewear_management_backend.entity.Frame frame) {
        if (frame == null || frame.getProduct() == null || frame.getProduct().getAvailableQuantity() == null) {
            return null;
        }
        return frame.getProduct().getAvailableQuantity();
    }

    @Named("getFramePreorder")
    default Boolean getFramePreorder(com.swp391.eyewear_management_backend.entity.Frame frame) {
        if (frame == null || frame.getProduct() == null || frame.getProduct().getAllowPreorder() == null) {
            return null;
        }
        return frame.getProduct().getAllowPreorder();
    }

    // ==========================================
    // LENS METHODS
    // ==========================================

    @Named("getLensName")
    default String getLensName(com.swp391.eyewear_management_backend.entity.Lens lens) {
        if (lens == null || lens.getProduct() == null) {
            return null;
        }
        return lens.getProduct().getProductName();
    }

    @Named("getLensImg")
    default String getLensImg(com.swp391.eyewear_management_backend.entity.Lens lens) {
        if (lens == null || lens.getProduct() == null || lens.getProduct().getImages() == null) {
            return null;
        }
        return lens.getProduct().getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getAvatar()))
                .map(com.swp391.eyewear_management_backend.entity.ProductImage::getImageUrl)
                .findFirst()
                .orElse(null);
    }

    @Named("getLensQuantity")
    default Integer getLensQuantity(com.swp391.eyewear_management_backend.entity.Lens lens) {
        if (lens == null || lens.getProduct() == null || lens.getProduct().getAvailableQuantity() == null) {
            return null;
        }
        return lens.getProduct().getAvailableQuantity();
    }

    @Named("getLensPreorder")
    default Boolean getLensPreorder(com.swp391.eyewear_management_backend.entity.Lens lens) {
        if (lens == null || lens.getProduct() == null || lens.getProduct().getAllowPreorder() == null) {
            return null;
        }
        return lens.getProduct().getAllowPreorder();
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================

    @Named("convertPrice")
    default Double convertPrice(Object price) {
        if (price == null) {
            return null; // Đổi thành null để tránh rác JSON nếu hàm này được dùng
        }
        return ((Number) price).doubleValue();
    }

    @Named("mapPrescription")
    default PrescriptionResponse mapPrescription(CartItem cartItem) {
        if (cartItem == null || cartItem.getPrescription() == null) {
            return null;
        }

        CartItemPrescription prescription = cartItem.getPrescription();

        return PrescriptionResponse.builder()
                .leftSPH(prescription.getLeftEyeSph() != null ? prescription.getLeftEyeSph().toString() : "0")
                .leftCYL(prescription.getLeftEyeCyl() != null ? prescription.getLeftEyeCyl().toString() : "0")
                .leftAXIS(prescription.getLeftEyeAxis() != null ? prescription.getLeftEyeAxis().toString() : "0")
                .leftADD(prescription.getLeftEyeAdd() != null ? prescription.getLeftEyeAdd().toString() : "0")
                .leftPD(prescription.getPdLeft() != null ? prescription.getPdLeft().toString() : "0")
                .rightSPH(prescription.getRightEyeSph() != null ? prescription.getRightEyeSph().toString() : "0")
                .rightCYL(prescription.getRightEyeCyl() != null ? prescription.getRightEyeCyl().toString() : "0")
                .rightAXIS(prescription.getRightEyeAxis() != null ? prescription.getRightEyeAxis().toString() : "0")
                .rightADD(prescription.getRightEyeAdd() != null ? prescription.getRightEyeAdd().toString() : "0")
                .rightPD(prescription.getPdRight() != null ? prescription.getPdRight().toString() : "0")
                .build();
    }
}