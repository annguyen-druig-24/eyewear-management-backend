package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.dto.response.ProductInventoryResponse;
import com.swp391.eyewear_management_backend.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ProductRepo extends JpaRepository<Product,Long> {
    @Query("SELECT p FROM Product p WHERE " +
            "p.isActive = true AND " +
            "(:name IS NULL OR p.productName LIKE %:name%) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:category IS NULL OR p.brand.brandName LIKE %:category%)")
    List<Product> searchProducts(@Param("name") String productName,
                                 @Param("minPrice") Double minPrice,
                                 @Param("maxPrice") Double maxPrice,
                                 @Param("category") String brand);


    @Query("SELECT p FROM Product p WHERE " +
            "(:name IS NULL OR p.productName LIKE %:name%) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:category IS NULL OR p.brand.brandName LIKE %:category%)")
    List<Product> searchProductsOfAdmin(@Param("name") String productName,
                                 @Param("minPrice") Double minPrice,
                                 @Param("maxPrice") Double maxPrice,
                                 @Param("category") String brand);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.productType.typeName = :typeName AND p.productID <> :excludeId ORDER BY p.productID DESC")
    List<Product> findByProductTypeNameExcludingId(@Param("typeName") String typeName, 
                                                     @Param("excludeId") Long excludeId);

    boolean existsBySKU(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Product p
            where p.productID in :productIds
            order by p.productID asc
            """)
    List<Product> findByIdsForUpdate(@Param("productIds") List<Long> productIds);

    @Query("""
            SELECT new com.swp391.eyewear_management_backend.dto.response.ProductInventoryResponse(
                p.productID,
                p.productName,
                pt.typeName,
                p.SKU,
                b.brandName,
                f.frameMaterialName,
                f.frameShapeName,
                lt.typeName,
                l.indexValue,
                l.isBlueLightBlock,
                l.isPhotochromic,
                cl.usageType,
                cl.lensMaterial,
                cl.baseCurve,
                cl.waterContent,
                cl.replacementSchedule,
                COALESCE(p.onHandQuantity, 0),
                COALESCE(p.reservedQuantity, 0),
                COALESCE(p.availableQuantity, 0),
                p.isActive
            )
            FROM Product p
            LEFT JOIN p.productType pt
            LEFT JOIN p.brand b
            LEFT JOIN p.frame f
            LEFT JOIN p.lens l
            LEFT JOIN l.lensType lt
            LEFT JOIN p.contactLens cl
            ORDER BY p.productID DESC
            """)
    List<ProductInventoryResponse> findAllProductsWithInventoryQuantity();

}
