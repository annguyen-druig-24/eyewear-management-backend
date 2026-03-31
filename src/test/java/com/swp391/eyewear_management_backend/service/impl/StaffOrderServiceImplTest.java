package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.config.ghn.GhnProperties;
import com.swp391.eyewear_management_backend.entity.InventoryTransaction;
import com.swp391.eyewear_management_backend.entity.Frame;
import com.swp391.eyewear_management_backend.entity.Lens;
import com.swp391.eyewear_management_backend.entity.Order;
import com.swp391.eyewear_management_backend.entity.PrescriptionOrder;
import com.swp391.eyewear_management_backend.entity.PrescriptionOrderDetail;
import com.swp391.eyewear_management_backend.entity.Product;
import com.swp391.eyewear_management_backend.entity.ShippingInfo;
import com.swp391.eyewear_management_backend.entity.User;
import com.swp391.eyewear_management_backend.integration.ghn.GhnShippingClient;
import com.swp391.eyewear_management_backend.mapper.ReturnExchangeMapper;
import com.swp391.eyewear_management_backend.mapper.StaffOrderMapper;
import com.swp391.eyewear_management_backend.repository.InventoryTransactionRepo;
import com.swp391.eyewear_management_backend.repository.OrderDetailRepo;
import com.swp391.eyewear_management_backend.repository.OrderRepo;
import com.swp391.eyewear_management_backend.repository.PaymentRepo;
import com.swp391.eyewear_management_backend.repository.PrescriptionOrderRepo;
import com.swp391.eyewear_management_backend.repository.ProductRepo;
import com.swp391.eyewear_management_backend.repository.ReturnExchangeRepo;
import com.swp391.eyewear_management_backend.repository.UserRepo;
import com.swp391.eyewear_management_backend.service.EmailService;
import com.swp391.eyewear_management_backend.service.ImageUploadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffOrderServiceImplTest {
    @Mock
    OrderRepo orderRepo;

    @Mock
    OrderDetailRepo orderDetailRepo;

    @Mock
    PrescriptionOrderRepo prescriptionOrderRepo;

    @Mock
    InventoryTransactionRepo inventoryTransactionRepo;

    @Mock
    StaffOrderMapper staffOrderMapper;

    @Mock
    ReturnExchangeRepo returnExchangeRepo;

    @Mock
    ReturnExchangeMapper returnExchangeMapper;

    @Mock
    PaymentRepo paymentRepo;

    @Mock
    ProductRepo productRepo;

    @Mock
    UserRepo userRepo;

    @Mock
    ImageUploadService imageUploadService;

    @Mock
    GhnShippingClient ghnShippingClient;

    @Mock
    GhnProperties ghnProperties;

    @Mock
    EmailService emailService;

    @InjectMocks
    StaffOrderServiceImpl staffOrderService;

    @Test
    void getOrderDetailForOperationStaff_shouldAllowMarkDeliveredWhenPrescriptionInventoryIsFullyFulfillable() {
        Order order = buildOperationOrder(12L, "PRESCRIPTION_ORDER", "READY", "SHIPPING");
        PrescriptionOrder prescriptionOrder = buildPrescriptionOrder(order, 3, 0, 1);

        InventoryTransaction frameAllocated = new InventoryTransaction();
        frameAllocated.setProduct(prescriptionOrder.getPrescriptionOrderDetails().get(0).getFrame().getProduct());
        frameAllocated.setQuantityChange(-3);

        InventoryTransaction lensAllocated = new InventoryTransaction();
        lensAllocated.setProduct(prescriptionOrder.getPrescriptionOrderDetails().get(0).getLens().getProduct());
        lensAllocated.setQuantityChange(-2);

        when(orderRepo.findByIdFetchStatus(12L)).thenReturn(Optional.of(order));
        when(orderDetailRepo.findByOrderIdFetchProduct(12L)).thenReturn(List.of());
        when(prescriptionOrderRepo.findByOrder_OrderID(12L)).thenReturn(Optional.of(prescriptionOrder));
        when(inventoryTransactionRepo.findByOrderOrderIDAndTransactionTypeIgnoreCase(12L, "SALE_OUT"))
                .thenReturn(List.of(frameAllocated, lensAllocated));

        var response = staffOrderService.getOrderDetailForOperationStaff(12L);

        assertThat(response.getInventoryReadyForOperationUpdate()).isTrue();
        assertThat(response.getAvailableActions()).contains("MARK_DELIVERED");
    }

    @Test
    void getOrderDetailForOperationStaff_shouldHideMarkDeliveredWhenPrescriptionInventoryIsStillMissing() {
        Order order = buildOperationOrder(13L, "PRESCRIPTION_ORDER", "READY", "SHIPPING");
        PrescriptionOrder prescriptionOrder = buildPrescriptionOrder(order, 3, 0, 0);

        InventoryTransaction frameAllocated = new InventoryTransaction();
        frameAllocated.setProduct(prescriptionOrder.getPrescriptionOrderDetails().get(0).getFrame().getProduct());
        frameAllocated.setQuantityChange(-3);

        InventoryTransaction lensAllocated = new InventoryTransaction();
        lensAllocated.setProduct(prescriptionOrder.getPrescriptionOrderDetails().get(0).getLens().getProduct());
        lensAllocated.setQuantityChange(-2);

        when(orderRepo.findByIdFetchStatus(13L)).thenReturn(Optional.of(order));
        when(orderDetailRepo.findByOrderIdFetchProduct(13L)).thenReturn(List.of());
        when(prescriptionOrderRepo.findByOrder_OrderID(13L)).thenReturn(Optional.of(prescriptionOrder));
        when(inventoryTransactionRepo.findByOrderOrderIDAndTransactionTypeIgnoreCase(13L, "SALE_OUT"))
                .thenReturn(List.of(frameAllocated, lensAllocated));

        var response = staffOrderService.getOrderDetailForOperationStaff(13L);

        assertThat(response.getInventoryReadyForOperationUpdate()).isFalse();
        assertThat(response.getAvailableActions()).doesNotContain("MARK_DELIVERED");
    }

    private Order buildOperationOrder(Long orderId, String orderType, String orderStatus, String shippingStatus) {
        User user = new User();
        user.setName("Operations Test");

        ShippingInfo shippingInfo = new ShippingInfo();
        shippingInfo.setShippingStatus(shippingStatus);

        Order order = new Order();
        order.setOrderID(orderId);
        order.setOrderType(orderType);
        order.setOrderStatus(orderStatus);
        order.setUser(user);
        order.setShippingInfo(shippingInfo);
        order.setTotalAmount(BigDecimal.TEN);
        return order;
    }

    private PrescriptionOrder buildPrescriptionOrder(Order order, int quantity, int frameAvailableQuantity, int lensAvailableQuantity) {
        Product frameProduct = new Product();
        frameProduct.setProductID(300L + order.getOrderID());
        frameProduct.setProductName("Frame product");
        frameProduct.setAvailableQuantity(frameAvailableQuantity);
        frameProduct.setPrice(BigDecimal.ONE);

        Frame frame = new Frame();
        frame.setFrameID(400L + order.getOrderID());
        frame.setProduct(frameProduct);

        Product lensProduct = new Product();
        lensProduct.setProductID(500L + order.getOrderID());
        lensProduct.setProductName("Lens product");
        lensProduct.setAvailableQuantity(lensAvailableQuantity);
        lensProduct.setPrice(BigDecimal.ONE);

        Lens lens = new Lens();
        lens.setLensID(600L + order.getOrderID());
        lens.setProduct(lensProduct);

        PrescriptionOrder prescriptionOrder = new PrescriptionOrder();
        prescriptionOrder.setPrescriptionOrderID(700L + order.getOrderID());
        prescriptionOrder.setOrder(order);
        prescriptionOrder.setUser(order.getUser());

        List<PrescriptionOrderDetail> details = new java.util.ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            PrescriptionOrderDetail detail = new PrescriptionOrderDetail();
            detail.setPrescriptionOrder(prescriptionOrder);
            detail.setFrame(frame);
            detail.setLens(lens);
            detail.setSubTotal(BigDecimal.ONE);
            details.add(detail);
        }
        prescriptionOrder.setPrescriptionOrderDetails(details);
        return prescriptionOrder;
    }
}
