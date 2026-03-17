package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "Return_Exchange_Item")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"returnExchange", "orderDetail", "prescriptionOrderDetail"})
public class ReturnExchangeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Return_Exchange_Item_ID")
    private Long returnExchangeItemID;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Return_Exchange_ID", nullable = false)
    private ReturnExchange returnExchange;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Order_Detail_ID")
    private OrderDetail orderDetail;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Prescription_Order_Detail_ID")
    private PrescriptionOrderDetail prescriptionOrderDetail;

    @Column(name = "Item_Source", nullable = false, columnDefinition = "NVARCHAR(40)")
    private String itemSource;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "Item_Evidence_URL", columnDefinition = "NVARCHAR(500)")
    private String itemEvidenceUrl;

    @Column(name = "Item_Reason", columnDefinition = "NVARCHAR(500)")
    private String itemReason;

    @Column(name = "Note", columnDefinition = "NVARCHAR(500)")
    private String note;


}
