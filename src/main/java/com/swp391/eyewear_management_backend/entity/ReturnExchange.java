package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Return_Exchange")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"orderDetail", "user"})
public class ReturnExchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Return_Exchange_ID")
    private Long returnExchangeID;

    @OneToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Order_Detail_ID", nullable = false, unique = true)
    private OrderDetail orderDetail;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "User_ID", nullable = false)
    private User user;

    @Column(name = "Return_Code", unique = true, nullable = false, columnDefinition = "NVARCHAR(50)")
    private String returnCode;

    @Column(name = "Request_Date", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "Return_Reason", columnDefinition = "NVARCHAR(500)")
    private String returnReason;

    @Column(name = "Return_Type", nullable = false, columnDefinition = "NVARCHAR(20)")
    private String returnType;

    @Column(name = "Product_Condition", columnDefinition = "NVARCHAR(50)")
    private String productCondition;

    @Column(name = "Refund_Amount", precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "Refund_Method", columnDefinition = "NVARCHAR(50)")
    private String refundMethod;

    @Column(name = "Refund_Account_Number", columnDefinition = "NVARCHAR(50)")
    private String refundAccountNumber;

    @Column(name = "Status", nullable = false, columnDefinition = "NVARCHAR(30)")
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Approved_By")
    private User approvedBy;

    @Column(name = "Approved_Date")
    private LocalDateTime approvedDate;

    @Column(name = "Reject_Reason", columnDefinition = "NVARCHAR(500)")
    private String rejectReason;

    @Column(name = "Image_URL", columnDefinition = "NVARCHAR(500)")
    private String imageUrl;

    public ReturnExchange(OrderDetail orderDetail, User user, String returnCode, LocalDateTime requestDate,
                          Integer quantity, String returnReason, String returnType, String productCondition, 
                          BigDecimal refundAmount, String refundMethod, String refundAccountNumber, String status, 
                          User approvedBy, LocalDateTime approvedDate, String rejectReason, String imageUrl) {
        this.orderDetail = orderDetail;
        this.user = user;
        this.returnCode = returnCode;
        this.requestDate = requestDate;
        this.quantity = quantity;
        this.returnReason = returnReason;
        this.returnType = returnType;
        this.productCondition = productCondition;
        this.refundAmount = refundAmount;
        this.refundMethod = refundMethod;
        this.refundAccountNumber = refundAccountNumber;
        this.status = status;
        this.approvedBy = approvedBy;
        this.approvedDate = approvedDate;
        this.rejectReason = rejectReason;
        this.imageUrl = imageUrl;
    }
}
