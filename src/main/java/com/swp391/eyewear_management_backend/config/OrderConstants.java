package com.swp391.eyewear_management_backend.config;

public final class OrderConstants {
    private OrderConstants() {
    }

    public static final String ORDER_TYPE_DIRECT = "DIRECT_ORDER";
    public static final String ORDER_TYPE_PRE = "PRE_ORDER";
    public static final String ORDER_TYPE_PRESCRIPTION = "PRESCRIPTION_ORDER";
    public static final String ORDER_TYPE_MIX = "MIX_ORDER";

    public static final String ORDER_STATUS_PENDING = "PENDING";
    public static final String ORDER_STATUS_CONFIRMED = "CONFIRMED";
    public static final String ORDER_STATUS_PARTIALLY_PAID = "PARTIALLY_PAID";
    public static final String ORDER_STATUS_PAID = "PAID";
    public static final String ORDER_STATUS_PROCESSING = "PROCESSING";
    public static final String ORDER_STATUS_READY = "READY";
    public static final String ORDER_STATUS_COMPLETED = "COMPLETED";
    public static final String ORDER_STATUS_CANCELED = "CANCELED";
    public static final String ORDER_STATUS_RETURNED = "RETURNED";

    public static final String SHIPPING_STATUS_PENDING = "PENDING";
    public static final String SHIPPING_STATUS_PACKING = "PACKING";
    public static final String SHIPPING_STATUS_SHIPPING = "SHIPPING";
    public static final String SHIPPING_STATUS_DELIVERED = "DELIVERED";
    public static final String SHIPPING_STATUS_FAILED = "FAILED";
    public static final String SHIPPING_STATUS_CANCELED = "CANCELED";
    public static final String SHIPPING_STATUS_RETURNED = "RETURNED";

    public static final String OPERATION_ACTION_START_PROCESSING = "START_PROCESSING";
    public static final String OPERATION_ACTION_START_PACKING = "START_PACKING";        //TH này dành cho khi 1 đơn hàng ko phải là Prescription - Từ CONFIRMED --> PACKING
    public static final String OPERATION_ACTION_MOVE_TO_PACKING = "MOVE_TO_PACKING";    //TH này dành cho khi 1 đơn hàng này là Prescription - CONFIRMED -> PROCESSING -> PACKING
    public static final String OPERATION_ACTION_HANDOVER_TO_GHN = "HANDOVER_TO_GHN";
    public static final String OPERATION_ACTION_MARK_DELIVERED = "MARK_DELIVERED";
    public static final String OPERATION_ACTION_MARK_FAILED = "MARK_FAILED";
    public static final String OPERATION_ACTION_MARK_RETURNED = "MARK_RETURNED";
    public static final String OPERATION_ACTION_COMPLETE_ORDER = "COMPLETE_ORDER";

    public static final String INVOICE_STATUS_UNPAID = "UNPAID";
    public static final String INVOICE_STATUS_PARTIALLY_PAID = "PARTIALLY_PAID";
    public static final String INVOICE_STATUS_PAID = "PAID";
    public static final String INVOICE_STATUS_CANCELED = "CANCELED";

    public static final String PAYMENT_PURPOSE_DEPOSIT = "DEPOSIT";
    public static final String PAYMENT_PURPOSE_FULL = "FULL";
    public static final String PAYMENT_PURPOSE_REMAINING = "REMAINING";

    public static final String PAYMENT_STATUS_PENDING = "PENDING";
    public static final String PAYMENT_STATUS_SUCCESS = "SUCCESS";
    public static final String PAYMENT_STATUS_FAILED = "FAILED";
    public static final String PAYMENT_STATUS_CANCELED = "CANCELED";
    public static final String PAYMENT_STATUS_REFUNDED = "REFUNDED";

    public static final String PAYMENT_METHOD_COD = "COD";
}
