package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Shipping_Info")
@Data
@NoArgsConstructor
public class ShippingInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Shipping_Info_ID")
    private Long shippingInfoID;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Order_ID", nullable = false, unique = true)
    private Order order;

    @Column(name = "Recipient_Name", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String recipientName;

    @Column(name = "Recipient_Phone", nullable = false)
    private String recipientPhone;

    @Column(name = "Recipient_Email")
    private String recipientEmail;

    @Column(name = "Recipient_Address", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String recipientAddress;

    @Column(name = "Note", columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @Column(name = "Province_Code")
    private Integer provinceCode;

    @Column(name = "Province_Name", columnDefinition = "NVARCHAR(100)")
    private String provinceName;

    @Column(name = "District_Code")
    private Integer districtCode;

    @Column(name = "District_Name", columnDefinition = "NVARCHAR(100)")
    private String districtName;

    @Column(name = "Ward_Code", columnDefinition = "NVARCHAR(20)")
    private String wardCode;

    @Column(name = "Ward_Name", columnDefinition = "NVARCHAR(100)")
    private String wardName;

    @Column(name = "Shipping_Method", nullable = false, columnDefinition = "NVARCHAR(30)")
    private String shippingMethod;

    @Column(name = "Shipping_Fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "Shipping_Status", nullable = false, columnDefinition = "NVARCHAR(30)")
    private String shippingStatus = "PENDING";

    @Column(name = "Expected_Delivery_At")
    private LocalDateTime expectedDeliveryAt;
}
