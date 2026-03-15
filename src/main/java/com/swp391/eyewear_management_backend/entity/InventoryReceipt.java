package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Inventory_Receipt")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"supplier", "createdBy", "approvedBy", "receivedBy", "details", "transactions"})
public class InventoryReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Inventory_Receipt_ID")
    private Long inventoryReceiptID;

    @Column(name = "Receipt_Code", nullable = false, unique = true, columnDefinition = "NVARCHAR(50)")
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Supplier_ID", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Created_By", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Approved_By")
    private User approvedBy;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Received_By")
    private User receivedBy;

    @Column(name = "Order_Date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "Received_Date")
    private LocalDateTime receivedDate;

    @Column(name = "Status", nullable = false, columnDefinition = "NVARCHAR(30)")
    private String status;

    @Column(name = "Note", columnDefinition = "NVARCHAR(500)")
    private String note;

    @OneToMany(mappedBy = "inventoryReceipt", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryReceiptDetail> details;

    @OneToMany(mappedBy = "inventoryReceipt", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<InventoryTransaction> transactions;

    @PrePersist
    public void prePersist() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
        if (status == null) {
            status = "DRAFT";
        }
    }
}
