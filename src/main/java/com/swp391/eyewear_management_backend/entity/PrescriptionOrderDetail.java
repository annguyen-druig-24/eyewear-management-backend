package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "Prescription_Order_Detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Prescription_Order_Detail_ID")
    private Long prescriptionOrderDetailID;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Prescription_Order_ID", nullable = false)
    private PrescriptionOrder prescriptionOrder;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Frame_ID")
    private Frame frame;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
    })
    @JoinColumn(name = "Lens_ID")
    private Lens lens;

    // Right Eye Prescription
    @Column(name = "Right_Eye_Sph", columnDefinition = "DECIMAL(5,2)")
    private Double rightEyeSph;

    @Column(name = "Right_Eye_Cyl", columnDefinition = "DECIMAL(5,2)")
    private Double rightEyeCyl;

    @Column(name = "Right_Eye_Axis")
    private Integer rightEyeAxis;

    @Column(name = "Right_Eye_Add", columnDefinition = "DECIMAL(5,2)")
    private Double rightEyeAdd;

    // Left Eye Prescription
    @Column(name = "Left_Eye_Sph", columnDefinition = "DECIMAL(5,2)")
    private Double leftEyeSph;

    @Column(name = "Left_Eye_Cyl", columnDefinition = "DECIMAL(5,2)")
    private Double leftEyeCyl;

    @Column(name = "Left_Eye_Axis")
    private Integer leftEyeAxis;

    @Column(name = "Left_Eye_Add", columnDefinition = "DECIMAL(5,2)")
    private Double leftEyeAdd;

    @Column(name = "PD", columnDefinition = "DECIMAL(4,1)")
    private Double pd;

    @Column(name = "PD_Right", columnDefinition = "DECIMAL(4,1)")
    private Double pdRight;

    @Column(name = "PD_Left", columnDefinition = "DECIMAL(4,1)")
    private Double pdLeft;

    @Column(name = "Sub_Total", nullable = false, precision = 15, scale = 2)
    private BigDecimal subTotal;
}
