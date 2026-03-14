package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "Inventory")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"product", "user", "supplier"})
public class    Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Inventory_ID")
    private Long inventoryID;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Product_ID", nullable = false)
    private Product product;

    @Column(name = "Quantity_Before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "Quantity_After", nullable = false)
    private Integer quantityAfter;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "User_ID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Supplier_ID", nullable = false)
    private Supplier supplier;

    @Column(name = "Order_Date")
    private LocalDateTime orderDate;

    @Column(name = "Received_Date")
    private LocalDateTime receivedDate;

    @Column(name = "Unit", columnDefinition = "NVARCHAR(20)")
    private String unit;

    public Inventory(Product product, Integer quantityBefore, Integer quantityAfter, User user,
                     Supplier supplier, LocalDateTime orderDate, LocalDateTime receivedDate, String unit) {
        this.product = product;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.user = user;
        this.supplier = supplier;
        this.orderDate = orderDate;
        this.receivedDate = receivedDate;
        this.unit = unit;
    }
}
