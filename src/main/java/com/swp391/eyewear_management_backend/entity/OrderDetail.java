package com.swp391.eyewear_management_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "Order_Detail")
@Getter
@Setter
@NoArgsConstructor
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Order_Detail_ID")
    private Long orderDetailID;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JsonIgnore
    @JoinColumn(name = "Order_ID", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Product_ID", nullable = false)
    private Product product;

    @Column(name = "Unit_Price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "Note", columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @OneToMany(mappedBy = "orderDetail", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ReturnExchangeItem> returnExchangeItems;

    @OneToMany(mappedBy = "orderDetail", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<InventoryTransaction> inventoryTransactions;

    public OrderDetail(Order order, Product product, BigDecimal unitPrice, String note, Integer quantity) {
        this.order = order;
        this.product = product;
        this.unitPrice = unitPrice;
        this.note = note;
        this.quantity = quantity;
    }
}
