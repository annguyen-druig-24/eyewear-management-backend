package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "[Order]")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "orderDetails", "payments", "inventoryTransactions", "invoice", "orderProcessings", "orderPromotions", "prescriptionOrder"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Order_ID")
    private Long orderID;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "User_ID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Promotion_ID")
    private Promotion promotion; // nullable

    @Column(name = "Order_Code", unique = true, columnDefinition = "NVARCHAR(50)")
    private String orderCode;

    @Column(name = "Order_Date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "Sub_Total", nullable = false, precision = 15, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "Tax_Amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "Discount_Amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "Shipping_Fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    // Total_Amount là computed column trong DB => chỉ đọc
    @Column(name = "Total_Amount", insertable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "Order_Type", nullable = false, columnDefinition = "NVARCHAR(20)")
    private String orderType;

    @Column(name = "Order_Status", nullable = false, columnDefinition = "NVARCHAR(30)")
    private String orderStatus;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Payment> payments;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<InventoryTransaction> inventoryTransactions;

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Invoice invoice;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<OrderProcessing> orderProcessings;

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private PrescriptionOrder prescriptionOrder;

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private ShippingInfo shippingInfo; // NEW

    @PrePersist
    public void prePersist() {
        if (orderDate == null) orderDate = LocalDateTime.now();
        if (taxAmount == null) taxAmount = BigDecimal.ZERO;
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
        if (shippingFee == null) shippingFee = BigDecimal.ZERO;
    }
}
