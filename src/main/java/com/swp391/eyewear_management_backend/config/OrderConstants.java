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
}
