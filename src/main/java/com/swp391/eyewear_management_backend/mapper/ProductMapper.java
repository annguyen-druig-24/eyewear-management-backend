package com.swp391.eyewear_management_backend.mapper;

import com.swp391.eyewear_management_backend.dto.response.ProductOfSupplierResponse;
import com.swp391.eyewear_management_backend.dto.response.extend.ContactLensResponse;
import com.swp391.eyewear_management_backend.dto.response.extend.FrameResponse;
import com.swp391.eyewear_management_backend.dto.response.extend.LensResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductDetailResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductResponse;
import com.swp391.eyewear_management_backend.entity.Product;
import com.swp391.eyewear_management_backend.entity.ProductImage;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import java.util.List;
import java.util.stream.Collectors;


@Mapper(componentModel = "spring")
public interface ProductMapper {
    String DEFAULT_IMAGE_URL = "default-placeholder.png";

    @Mapping(source = "productID", target = "id")
    @Mapping(source = "productName", target = "name")
    @Mapping(source = "SKU", target = "sku")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "brand.brandName", target = "brand")
    @Mapping(source = "productType.typeName", target = "product_Type")
    @Mapping(source = "images", target = "image_URL", qualifiedByName = "getAvatarUrl")
    @Mapping(source = "product", target = "frameId", qualifiedByName = "getFrameId")
    @Mapping(source = "product", target = "lensId", qualifiedByName = "getLensId")
    @Mapping(source = "product", target = "contactLensId", qualifiedByName = "getContactLensId")
    @Mapping(source = "frame.frameShapeName", target = "frameShapeName")
    @Mapping(source = "frame.frameMaterialName", target = "frameMaterialName")
    @Mapping(source = "frame.color", target = "color")
    @Mapping(source = "lens.isBlueLightBlock", target = "isBlueLightBlock")
    @Mapping(source = "lens.isPhotochromic", target = "isPhotochromic")
    @Mapping(source = "lens.lensType.typeName", target = "typeName")
    @Mapping(source = "contactLens.usageType", target = "usageType")
    @Mapping(source = "contactLens.lensMaterial", target = "lensMaterial")
    @Mapping(source = "contactLens.replacementSchedule", target = "replacementSchedule")
    ProductResponse toProductResponse(Product product);

    @Named("getAvatarUrl")
    default String getAvatarUrl(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return DEFAULT_IMAGE_URL;
        }

        // Tối ưu: dùng vòng lặp truyền thống thay vì stream cho performance tốt hơn
        for (ProductImage image : images) {
            if (Boolean.TRUE.equals(image.getAvatar())) {
                return image.getImageUrl();
            }
        }
        return DEFAULT_IMAGE_URL;
    }

    @Named("getFrameId")
    default Long getFrameId(Product product) {
        // ProductType ID = 1 là Frame
        if (product.getProductType() != null && product.getProductType().getProductTypeID() == 1) {
            return product.getFrame() != null ? product.getFrame().getFrameID() : null;
        }
        return null;
    }

    @Named("getLensId")
    default Long getLensId(Product product) {
        // ProductType ID = 2 là Lens
        if (product.getProductType() != null && product.getProductType().getProductTypeID() == 2) {
            return product.getLens() != null ? product.getLens().getLensID() : null;
        }
        return null;
    }

    @Named("getContactLensId")
    default Long getContactLensId(Product product) {
        // ProductType ID = 3 là Contact Lens
        if (product.getProductType() != null && product.getProductType().getProductTypeID() == 3) {
            return product.getContactLens() != null ? product.getContactLens().getContactLensID() : null;
        }
        return null;
    }

    default ProductDetailResponse toDetailResponse(Product product) {
        if (product.getFrame() != null) {
            return toFrameResponse(product);
        } else if (product.getLens() != null) {
            return toLensResponse(product);
        } else if (product.getContactLens() != null) {
            return toContactLensResponse(product);
        }
        // Trường hợp fallback nếu dữ liệu lỗi
        return null;
    }

    @Mapping(source = "productID", target = "id")
    @Mapping(source = "productName", target = "name")
    @Mapping(source = "SKU", target = "sku")
    @Mapping(source = "price", target = "price")
    @Mapping(source = "brand.brandName", target = "brandName")
    @Mapping(target = "imageUrls", expression = "java(mapImages(product.getImages()))")
    @Mapping(source = "productType.typeName", target = "product_Type")
    ProductDetailResponse baseProductMapping(Product product);

    // 1. Map cho Frame
    @InheritConfiguration(name = "baseProductMapping")
    // Map các trường riêng của Frame
    @Mapping(source = "frame.frameID", target = "frameId")
    @Mapping(source = "frame.color", target = "color")
    @Mapping(source = "frame.frameMaterialName", target = "material")
    @Mapping(source = "frame.frameShapeName", target = "frameShape")
    @Mapping(source = "frame.description", target = "description")
    @Mapping(source = "productType.typeName", target = "product_Type")
    @Mapping(target = "relatedFrames", ignore = true)
    @Mapping(target = "relatedLenses", ignore = true)
    FrameResponse toFrameResponse(Product product);

    // 2. Map cho Lens
    @InheritConfiguration(name = "baseProductMapping")
    // Map các trường riêng của Lens
    @Mapping(source = "lens.lensID", target = "lensId")
    @Mapping(source = "lens.indexValue", target = "indexValue")
    @Mapping(source = "lens.description", target = "description")
    @Mapping(source = "productType.typeName", target = "product_Type")
    @Mapping(target = "relatedLenses", ignore = true)
    @Mapping(target = "relatedFrames", ignore = true)
    LensResponse toLensResponse(Product product);

    // 3. Map cho Contact Lens
    @InheritConfiguration(name = "baseProductMapping")
    // Map các trường riêng
    @Mapping(source = "contactLens.contactLensID", target = "contactLensId")
    @Mapping(source = "contactLens.waterContent", target = "waterContent")
    @Mapping(source = "contactLens.diameter", target = "diameter")
    @Mapping(source = "productType.typeName", target = "product_Type")
    @Mapping(target = "relatedContactLenses", ignore = true)
    ContactLensResponse toContactLensResponse(Product product);

    // Helper map images (như cũ)
    default List<String> mapImages(List<ProductImage> images) {
        if (images == null) return null;
        return images.stream().map(ProductImage::getImageUrl).collect(Collectors.toList());
    }

    // ... các code cũ trong mapper ...

    @Mapping(source = "productType.productTypeID", target = "productTypeId") // Chú ý sửa lại tên getter cho đúng với entity của bạn
    @Mapping(source = "productType.typeName", target = "productTypeName")

    @Mapping(source = "brand.brandID", target = "brandId")
    @Mapping(source = "brand.brandName", target = "brandName")

    // Giả sử các class Frame, Lens, ContactLens của bạn có khóa chính lần lượt là frameID, lensID, contactLensID
    @Mapping(source = "frame.frameID", target = "frameId")
    @Mapping(source = "lens.lensID", target = "lensId")
    @Mapping(source = "contactLens.contactLensID", target = "contactLensId")
    ProductOfSupplierResponse toProductOfSupplierResponse(Product product);
}