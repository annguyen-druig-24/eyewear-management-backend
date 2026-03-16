package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Return_Exchange")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "order", "items", "approvedBy", "processedBy"})
public class ReturnExchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Return_Exchange_ID")
    private Long returnExchangeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Order_ID", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "User_ID", nullable = false)
    private User user;

    @Column(name = "Return_Code", unique = true, nullable = false, columnDefinition = "NVARCHAR(50)")
    private String returnCode;

    @Column(name = "Request_Date", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "Request_Note", columnDefinition = "NVARCHAR(1000)")
    private String requestNote;

    @Column(name = "Return_Reason", columnDefinition = "NVARCHAR(500)")
    private String returnReason;

    @Column(name = "Customer_Evidence_URL", columnDefinition = "NVARCHAR(500)")
    private String customerEvidenceUrl;

    @Column(name = "Return_Type", nullable = false, columnDefinition = "NVARCHAR(20)")
    private String returnType;

    @Column(name = "Request_Scope", nullable = false, columnDefinition = "NVARCHAR(10)")
    private String requestScope;

    @Column(name = "Refund_Amount", precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "Refund_Method", columnDefinition = "NVARCHAR(30)")
    private String refundMethod;

    @Column(name = "Refund_Account_Number", columnDefinition = "NVARCHAR(100)")
    private String refundAccountNumber;

    @Column(name = "Refund_Account_Name", columnDefinition = "NVARCHAR(100)")
    private String refundAccountName;

    @Column(name = "Refund_Reference_Code", columnDefinition = "NVARCHAR(100)")
    private String refundReferenceCode;

    @Column(name = "Staff_Refund_Evidence_URL", columnDefinition = "NVARCHAR(500)")
    private String staffRefundEvidenceUrl;

    @Column(name = "Status", nullable = false, columnDefinition = "NVARCHAR(30)")
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Approved_By")
    private User approvedBy;

    @Column(name = "Approved_Date")
    private LocalDateTime approvedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Processed_By")
    private User processedBy;

    @Column(name = "Processed_Date")
    private LocalDateTime processedDate;

    @Column(name = "Reject_Reason", columnDefinition = "NVARCHAR(500)")
    private String rejectReason;

    @OneToMany(mappedBy = "returnExchange", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnExchangeItem> items = new ArrayList<>();
}