package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "Inventory_Transaction")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"product", "order", "orderDetail", "inventoryReceipt", "performedBy"})
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Inventory_Transaction_ID")
    private Long inventoryTransactionID;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Product_ID", nullable = false)
    private Product product;

    @Column(name = "Transaction_Type", nullable = false, columnDefinition = "NVARCHAR(30)")
    private String transactionType;

    @Column(name = "Quantity_Change", nullable = false)
    private Integer quantityChange;

    @Column(name = "Quantity_Before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "Quantity_After", nullable = false)
    private Integer quantityAfter;

    @Column(name = "Reference_Type", columnDefinition = "NVARCHAR(30)")
    private String referenceType;

    @Column(name = "Reference_ID")
    private Long referenceID;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Order_ID")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Order_Detail_ID")
    private OrderDetail orderDetail;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Inventory_Receipt_ID")
    private InventoryReceipt inventoryReceipt;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Performed_By", nullable = false)
    private User performedBy;

    @Column(name = "Performed_At", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "Note", columnDefinition = "NVARCHAR(500)")
    private String note;

    @PrePersist
    public void prePersist() {
        if (performedAt == null) {
            performedAt = LocalDateTime.now();
        }
    }
}
