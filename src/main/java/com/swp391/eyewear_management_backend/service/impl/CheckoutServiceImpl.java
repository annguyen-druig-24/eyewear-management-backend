package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.request.CheckoutPreviewRequest;
import com.swp391.eyewear_management_backend.dto.request.ShippingAddressRequest;
import com.swp391.eyewear_management_backend.dto.response.CheckoutLineItemResponse;
import com.swp391.eyewear_management_backend.dto.response.CheckoutPreviewResponse;
import com.swp391.eyewear_management_backend.dto.response.PrescriptionInfoResponse;
import com.swp391.eyewear_management_backend.entity.CartItem;
import com.swp391.eyewear_management_backend.entity.CartItemPrescription;
import com.swp391.eyewear_management_backend.entity.Product;
import com.swp391.eyewear_management_backend.exception.AppException;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import com.swp391.eyewear_management_backend.repository.CartItemPrescriptionRepo;
import com.swp391.eyewear_management_backend.repository.CartItemRepo;
import com.swp391.eyewear_management_backend.repository.UserRepo;
import com.swp391.eyewear_management_backend.service.CheckoutService;
import com.swp391.eyewear_management_backend.service.GhnShippingService;
import com.swp391.eyewear_management_backend.service.PromotionCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final CartItemRepo cartItemRepo;
    private final UserRepo userRepo;

    private final PromotionCalculatorService promotionCalculatorService;
    private final GhnShippingService ghnShippingService;

    private final CartItemPrescriptionRepo cartItemPrescriptionRepo;

    @Override
    public CheckoutPreviewResponse preview(CheckoutPreviewRequest request) {

        // 1) user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long userId = userRepo.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED))
                .getUserId();

        // 2) normalize ids: validate request --> Loại bỏ các id = null, hoặc id trùng
        List<Long> ids = request.getCartItemIds().stream()
                .filter(Objects::nonNull)   //Kiểm tra xem id có bị null hay ko
                .distinct()     //Kiểm tra các id trùng
                .toList();
        if (ids.isEmpty()) throw new AppException(ErrorCode.CART_ITEM_ID_REQUIRED);

        // 3) load cart items of this user: (A) Load dữ liệu cart item chuẩn từ DB, (B) Validate quyền sở hữu + tính hợp lệ
        List<CartItem> cartItems = cartItemRepo.findByUserIdAndIdsFetchAll(userId, ids);
        if (cartItems.size() != ids.size()) {
            throw new AppException(ErrorCode.CART_ITEM_INVALID_FOR_CHECKOUT);
        }

        // 3.1) load prescription info (Cart_Item_Prescription) theo cartItemIds
        //  Mục tiêu: a) Load toàn bộ prescription info liên quan đến các CartItemIds đang checkout bằng 1 query repo
        //            b) Chuyển List -> Map để tra cứu nhanh theo CartItemIds trong bước build line item
        List<CartItemPrescription> rxList = cartItemPrescriptionRepo.findByCartItem_CartItemIdIn(ids);
        Map<Long, CartItemPrescription> rxMap = new HashMap<>();
        for (CartItemPrescription rx : rxList) {
            if (rx.getCartItem() != null && rx.getCartItem().getCartItemId() != null) {
                rxMap.put(rx.getCartItem().getCartItemId(), rx);
            }
        }

        // 4) build line items + subTotal + detect types + prescriptionAmount (before discount)
        List<CheckoutLineItemResponse> lineResponses = new ArrayList<>();
        BigDecimal subTotal = BigDecimal.ZERO;

        boolean hasDirect = false;
        boolean hasPre = false;
        boolean hasPrescription = false;

        BigDecimal prescriptionAmountBeforeDiscount = BigDecimal.ZERO;

        for (CartItem ci : cartItems) {
            int qty = ci.getQuantity() == null || ci.getQuantity() <= 0 ? 1 : ci.getQuantity();

            BigDecimal unitPrice = ci.getPrice() == null ? BigDecimal.ZERO : ci.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            subTotal = subTotal.add(lineTotal);

            //determine itemType: ưu tiên có rx record OR frame+lens
            String itemType = determineItemType(ci, rxMap.containsKey(ci.getCartItemId()));

            if ("PRESCRIPTION".equals(itemType)) {
                hasPrescription = true;
                prescriptionAmountBeforeDiscount = prescriptionAmountBeforeDiscount.add(lineTotal);
            } else if ("PRE_ORDER".equals(itemType)) {
                hasPre = true;
            } else {
                hasDirect = true;
            }

            //build prescription response if exists
            PrescriptionInfoResponse rxResp = null;
            CartItemPrescription rx = rxMap.get(ci.getCartItemId());
            if (rx != null) {
                rxResp = PrescriptionInfoResponse.builder()
                        .rightEyeSph(rx.getRightEyeSph())
                        .rightEyeCyl(rx.getRightEyeCyl())
                        .rightEyeAxis(rx.getRightEyeAxis())
                        .rightEyeAdd(rx.getRightEyeAdd())
                        .leftEyeSph(rx.getLeftEyeSph())
                        .leftEyeCyl(rx.getLeftEyeCyl())
                        .leftEyeAxis(rx.getLeftEyeAxis())
                        .leftEyeAdd(rx.getLeftEyeAdd())
                        .pd(rx.getPd())
                        .pdRight(rx.getPdRight())
                        .pdLeft(rx.getPdLeft())
                        .build();
            }

            lineResponses.add(CheckoutLineItemResponse.builder()
                    .cartItemId(ci.getCartItemId())
                    .itemType(itemType)
                    .name(buildDisplayName(ci))
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .lineDiscount(BigDecimal.ZERO) // sẽ set sau nếu có promo
                    .contactLensId(ci.getContactLens() != null ? ci.getContactLens().getContactLensID() : null)
                    .frameId(ci.getFrame() != null ? ci.getFrame().getFrameID() : null)
                    .lensId(ci.getLens() != null ? ci.getLens().getLensID() : null)
                    .prescription(rxResp)
                    .build());
        }

        String orderType = determineOrderType(hasDirect, hasPre, hasPrescription);

        // 5) Promotions (available + recommended + apply selected)
        var promoResult = promotionCalculatorService.evaluate(cartItems, subTotal, request.getPromotionId());
        BigDecimal discountAmount = promoResult.discountAmount();
        Map<Long, BigDecimal> itemDiscountMap = promoResult.itemDiscountMap();

        // attach lineDiscount for FE display + compute prescriptionAmount after discount
        BigDecimal prescriptionAmountAfterDiscount = BigDecimal.ZERO;
        for (CheckoutLineItemResponse li : lineResponses) {
            BigDecimal itemDisc = itemDiscountMap.getOrDefault(li.getCartItemId(), BigDecimal.ZERO);
            li.setLineDiscount(itemDisc);

            if ("PRESCRIPTION".equals(li.getItemType())) {
                BigDecimal net = li.getLineTotal().subtract(itemDisc);
                prescriptionAmountAfterDiscount = prescriptionAmountAfterDiscount.add(net);
            }
        }

        // 6) Shipping (GHN) - chỉ tính nếu có đủ address codes
        ShippingAddressRequest shipAddr = request.getAddress();

        boolean missing = (shipAddr == null
                || shipAddr.getDistrictCode() == null
                || shipAddr.getWardCode() == null);

        if (missing) {
            var user = userRepo.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            // chỉ build address khi user có đủ code
            if (user.getDistrictCode() != null && user.getWardCode() != null) {
                shipAddr = new ShippingAddressRequest();
                shipAddr.setStreet(user.getAddress());          // địa chỉ chi tiết
                shipAddr.setProvinceCode(user.getProvinceCode());
                shipAddr.setProvinceName(user.getProvinceName());
                shipAddr.setDistrictCode(user.getDistrictCode());
                shipAddr.setDistrictName(user.getDistrictName());
                shipAddr.setWardCode(user.getWardCode());
                shipAddr.setWardName(user.getWardName());
            }
        }

        var ship = ghnShippingService.calculate(cartItems, shipAddr);
        BigDecimal shippingFee = ship.shippingFee();
        var expectedDeliveryAt = ship.expectedDeliveryAt();

        // 7) total
        BigDecimal totalAmount = subTotal.subtract(discountAmount).add(shippingFee);

        // 8) deposit
        DepositResult deposit = calculateDeposit(orderType, hasPrescription, totalAmount, prescriptionAmountAfterDiscount);

        return CheckoutPreviewResponse.builder()
                .items(lineResponses)
                .orderType(orderType)
                .subTotal(subTotal.setScale(0, RoundingMode.HALF_UP))
                .discountAmount(discountAmount.setScale(0, RoundingMode.HALF_UP))
                .shippingFee(shippingFee.setScale(0, RoundingMode.HALF_UP))
                .expectedDeliveryAt(expectedDeliveryAt)
                .totalAmount(totalAmount.setScale(0, RoundingMode.HALF_UP))
                .depositRequired(deposit.depositRequired)
                .depositAmount(deposit.depositAmount)
                .remainingAmount(deposit.remainingAmount)
                .availablePromotions(promoResult.availablePromotions())
                .recommendedPromotion(promoResult.recommendedPromotion())
                .appliedPromotionId(promoResult.appliedPromotionId())
                .build();
    }

    /**
     * itemType logic:
     * - nếu có prescription record (Cart_Item_Prescription) => PRESCRIPTION
     * - hoặc frame + lens => PRESCRIPTION
     * - còn lại: PRE_ORDER nếu product.allowPreorder = true, else DIRECT
     */
    private String determineItemType(CartItem ci, boolean hasPrescriptionRecord) {
        if (hasPrescriptionRecord) return "PRESCRIPTION";
        if (ci.getFrame() != null && ci.getLens() != null) return "PRESCRIPTION";

        Product p = resolveSingleProduct(ci);
        boolean allowPre = p != null && Boolean.TRUE.equals(p.getAllowPreorder());
        return allowPre ? "PRE_ORDER" : "DIRECT";
    }

    private Product resolveSingleProduct(CartItem ci) {
        if (ci.getContactLens() != null) return ci.getContactLens().getProduct();
        if (ci.getFrame() != null) return ci.getFrame().getProduct();
        if (ci.getLens() != null) return ci.getLens().getProduct();
        return null;
    }

    private String buildDisplayName(CartItem ci) {
        if (ci.getFrame() != null && ci.getLens() != null) {
            String fn = ci.getFrame().getProduct() != null ? ci.getFrame().getProduct().getProductName() : "Frame";
            String ln = ci.getLens().getProduct() != null ? ci.getLens().getProduct().getProductName() : "Lens";
            return fn + " + " + ln;
        }
        Product p = resolveSingleProduct(ci);
        return p != null ? p.getProductName() : "Cart Item";
    }

    private String determineOrderType(boolean hasDirect, boolean hasPre, boolean hasPrescription) {
        int types = 0;
        if (hasDirect) types++;
        if (hasPre) types++;
        if (hasPrescription) types++;

        if (types > 1) return "MIX_ORDER";
        if (hasPrescription) return "PRESCRIPTION_ORDER";
        if (hasPre) return "PRE_ORDER";
        return "DIRECT_ORDER";
    }

    private DepositResult calculateDeposit(String orderType, boolean hasPrescription,
                                           BigDecimal totalAmount, BigDecimal prescriptionAmountAfterDiscount) {

        BigDecimal threshold = new BigDecimal("5000000");
        BigDecimal rate = new BigDecimal("0.20");

        boolean isMixed = "MIX_ORDER".equals(orderType);

        BigDecimal depositAmount = BigDecimal.ZERO;

        if (!hasPrescription) {
            if (totalAmount.compareTo(threshold) >= 0) {
                depositAmount = totalAmount.multiply(rate);
            }
        } else {
            if (isMixed) {
                if (totalAmount.compareTo(threshold) >= 0) {
                    depositAmount = totalAmount.multiply(rate);
                } else {
                    depositAmount = prescriptionAmountAfterDiscount.multiply(rate);
                }
            } else {
                if (totalAmount.compareTo(threshold) >= 0) {
                    depositAmount = totalAmount.multiply(rate);
                } else {
                    depositAmount = prescriptionAmountAfterDiscount.multiply(rate);
                }
            }
        }

        depositAmount = depositAmount.setScale(0, RoundingMode.HALF_UP);
        BigDecimal remaining = totalAmount.subtract(depositAmount).setScale(0, RoundingMode.HALF_UP);

        return new DepositResult(depositAmount.compareTo(BigDecimal.ZERO) > 0, depositAmount, remaining);
    }

    private record DepositResult(boolean depositRequired, BigDecimal depositAmount, BigDecimal remainingAmount) {}
}
