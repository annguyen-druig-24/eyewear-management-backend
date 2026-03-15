package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "Inventory_Receipt_Detail")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"inventoryReceipt", "product"})
public class InventoryReceiptDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Inventory_Receipt_Detail_ID")
    private Long inventoryReceiptDetailID;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Inventory_Receipt_ID", nullable = false)
    private InventoryReceipt inventoryReceipt;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Product_ID", nullable = false)
    private Product product;

    @Column(name = "Ordered_Quantity", nullable = false)
    private Integer orderedQuantity;

    @Column(name = "Received_Quantity", nullable = false)
    private Integer receivedQuantity = 0;

    @Column(name = "Rejected_Quantity", nullable = false)
    private Integer rejectedQuantity = 0;

    @Column(name = "Unit_Cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "Note", columnDefinition = "NVARCHAR(500)")
    private String note;
}
