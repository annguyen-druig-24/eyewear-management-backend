package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Return_Exchange_Item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnExchangeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Return_Exchange_Item_ID")
    private Long returnExchangeItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Return_Exchange_ID", nullable = false)
    private ReturnExchange returnExchange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Order_Detail_ID", nullable = false)
    private OrderDetail orderDetail;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "Item_Reason", columnDefinition = "NVARCHAR(500)")
    private String itemReason;

    @Column(name = "Note", columnDefinition = "NVARCHAR(500)")
    private String note;
}