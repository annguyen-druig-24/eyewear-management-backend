package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "Product")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"frame", "lens", "contactLens", "images", "inventories", "orderDetails", "promotions"})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Product_ID")
    private Long productID;

    @Column(name = "Product_Name", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String productName;

    //Luu y coi bang lai de mapping dung table
    @Column(name = "SKU", columnDefinition = "NVARCHAR(50)")
    private String SKU;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Product_Type_ID", nullable = false)
    private ProductType productType;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Brand_ID", nullable = false)
    private Brand brand;

    @Column(name = "Price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "Cost_Price", nullable = false, precision = 15, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "Allow_Preorder", nullable = false)
    private Boolean allowPreorder = false;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "Is_Active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inventory> inventories;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;

    @OneToOne(mappedBy = "product", fetch = FetchType.LAZY)
    private Frame frame;

    @OneToOne(mappedBy = "product", fetch = FetchType.LAZY)
    private Lens lens;

    @OneToOne(mappedBy = "product", fetch = FetchType.LAZY)
    private ContactLens contactLens;

    public Product(String productName, ProductType productType, Brand brand, BigDecimal price,
                   BigDecimal costPrice) {
        this.productName = productName;
        this.productType = productType;
        this.brand = brand;
        this.price = price;
        this.costPrice = costPrice;
    }
}
