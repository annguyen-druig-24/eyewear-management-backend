package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "[User]")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"role", "orders", "inventories", "createdInventoryReceipts", "approvedInventoryReceipts", "receivedInventoryReceipts", "performedInventoryTransactions", "orderProcessings", "returnExchanges", "approvedReturnExchanges", "prescriptionOrders"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "User_ID")
    private Long userId;

    @Column(name = "Username", unique = true, nullable = false, columnDefinition = "NVARCHAR(50)")
    private String username;

    @Column(name = "Password", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String password;

    @Column(name = "Email", unique = true, nullable = false, columnDefinition = "NVARCHAR(100)")
    private String email;

    @Column(name = "Phone", nullable = false, columnDefinition = "VARCHAR(15)")
    private String phone;

    @ManyToOne
    @JoinColumn(name = "Role_ID")
    private Role role;

    @Column(name = "Status")
    private Boolean status;

    @Column(name = "Name", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String name;

    @Column(name = "Address", columnDefinition = "NVARCHAR(255)")
    private String address;

    @Column(name = "Date_of_Birth")
    private LocalDate dateOfBirth;

    @Column(name = "ID_Number", columnDefinition = "VARCHAR(20)")
    private String idNumber;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Order> orders;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Inventory> inventories;

    @OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
    private List<InventoryReceipt> createdInventoryReceipts;

    @OneToMany(mappedBy = "approvedBy", fetch = FetchType.LAZY)
    private List<InventoryReceipt> approvedInventoryReceipts;

    @OneToMany(mappedBy = "receivedBy", fetch = FetchType.LAZY)
    private List<InventoryReceipt> receivedInventoryReceipts;

    @OneToMany(mappedBy = "performedBy", fetch = FetchType.LAZY)
    private List<InventoryTransaction> performedInventoryTransactions;

    @OneToMany(mappedBy = "changedBy", fetch = FetchType.LAZY)
    private List<OrderProcessing> orderProcessings;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ReturnExchange> returnExchanges;

    @OneToMany(mappedBy = "approvedBy", fetch = FetchType.LAZY)
    private List<ReturnExchange> approvedReturnExchanges;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<PrescriptionOrder> prescriptionOrders;

    // Province
    @Column(name = "Province_Code")
    private Integer provinceCode;

    @Column(name = "Province_Name", columnDefinition = "NVARCHAR(100)")
    private String provinceName;

    // District
    @Column(name = "District_Code")
    private Integer districtCode;

    @Column(name = "District_Name", columnDefinition = "NVARCHAR(100)")
    private String districtName;

    // Ward (GHN ward code là string)
    @Column(name = "Ward_Code", columnDefinition = "NVARCHAR(20)")
    private String wardCode;

    @Column(name = "Ward_Name", columnDefinition = "NVARCHAR(100)")
    private String wardName;

    public User(String username, String password, String email, String phone, Role role, Boolean status, String name, String address, LocalDate dateOfBirth, String idNumber, Integer provinceCode, String provinceName, Integer districtCode, String districtName, String wardCode, String wardName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.name = name;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.idNumber = idNumber;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.districtCode = districtCode;
        this.districtName = districtName;
        this.wardCode = wardCode;
        this.wardName = wardName;
    }
}
