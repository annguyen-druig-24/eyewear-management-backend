package com.swp391.eyewear_management_backend.entity;

import com.swp391.eyewear_management_backend.entity.enumpackage.ItemType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "Cart_Item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Cart_Item_ID")
    private Long cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Cart_ID", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Contact_Lens_ID")
    private ContactLens contactLens;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Frame_ID")
    private Frame frame;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Lens_ID")
    private Lens lens;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "Frame_Price", columnDefinition = "DECIMAL(18,2)")
    private BigDecimal framePrice;

    @Column(name = "Lens_Price", columnDefinition = "DECIMAL(18,2)")
    private BigDecimal lensPrice;

    @Column(name = "Contact_Lens_Price", columnDefinition = "DECIMAL(18,2)")
    private BigDecimal contactLensPrice;

    @Column(name = "Price", columnDefinition = "DECIMAL(18,2)", nullable = false)
    private BigDecimal price;

    // THÊM TRƯỜNG PHÂN LOẠI VÀO ĐÂY
    @Enumerated(EnumType.STRING)
    @Column(name = "Item_Type", length = 50)
    private ItemType itemType;

    @OneToOne(mappedBy = "cartItem",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private CartItemPrescription prescription;

    public void setPrescription(CartItemPrescription rx) {
        this.prescription = rx;
        if (rx != null && rx.getCartItem() != this) {
            rx.setCartItem(this);
        }
    }
}