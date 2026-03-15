package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Entity
@Table(name = "Supplier")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"brandSuppliers", "inventories", "inventoryReceipts"})
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Supplier_ID")
    private Long supplierID;

    @Column(name = "Supplier_Name", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String supplierName;

    @Column(name = "Supplier_Phone", nullable = false, columnDefinition = "VARCHAR(15)")
    private String supplierPhone;

    @Column(name = "Supplier_Address", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String supplierAddress;

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<BrandSupplier> brandSuppliers;

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Inventory> inventories;

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<InventoryReceipt> inventoryReceipts;

    public Supplier(String supplierName, String supplierPhone, String supplierAddress) {
        this.supplierName = supplierName;
        this.supplierPhone = supplierPhone;
        this.supplierAddress = supplierAddress;
    }
}
