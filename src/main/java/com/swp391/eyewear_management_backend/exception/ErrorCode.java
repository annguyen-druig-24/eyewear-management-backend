package com.swp391.eyewear_management_backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized Exception", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(1023, "Invalid request", HttpStatus.BAD_REQUEST),

    //Promotion
    PROMOTION_NOT_FOUND(1024, "Promotion not found", HttpStatus.NOT_FOUND),
    PROMOTION_NOT_APPLICABLE(1025, "Promotion is not applicable for this checkout", HttpStatus.BAD_REQUEST),

    // Auth
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),      // 401
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN), // 403

    // User
    USER_EXISTED(1002, "User already exists", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "User not exists", HttpStatus.NOT_FOUND),

    // Order
    ORDER_NOT_FOUND(1026, "Order not found", HttpStatus.NOT_FOUND), // 404

    //Return-Exchange
    RETURN_EXCHANGE_NOT_FOUND(1032, "Return Exchange not found", HttpStatus.NOT_FOUND),
    RETURN_EXCHANGE_NOT_FIT(400, "The reason or item condition is not eligible for a return or exchange.",HttpStatus.BAD_REQUEST),
    RETURN_EXCHANGE_EXPIRED_TIME(1035, "Expired Time return exchange order/item.",HttpStatus.BAD_REQUEST),
    RETURN_EXCHANGE_REFUND_METHOD_NOT_FOUND(1035, "Refund Method is required.",HttpStatus.BAD_REQUEST),
    RETURN_EXCHANGE_REFUND_ACCOUNT_NOT_FOUND(1035, "Refund Account Number is required.",HttpStatus.BAD_REQUEST),
    RETURN_EXCHANGE_REFUND_NAME_NOT_FOUND(1035, "Refund Account Name is required.",HttpStatus.BAD_REQUEST),
    UPLOAD_IMAGE_FAILED(1035, "Upload image failed.",HttpStatus.BAD_REQUEST),
    ITEM_NOT_FOUND(1035, "Item not found.",HttpStatus.BAD_REQUEST),
    ORDER_DETAIL_MATCH_PRESCRIPTION_DETAIL(1035, "Order Detail not match Prescription Order Detail",HttpStatus.BAD_REQUEST),


    //Inventory
    NOT_FOUND_SUPPLIER(1078,"Can not found the supplier",HttpStatus.NOT_FOUND),

    //Glass Try On Config
    PRODUCT_NOT_FOUND(2012, "Product not found", HttpStatus.NOT_FOUND),
    PRODUCT_TRY_ON_CONFIG_NOT_FOUND(2013, "Product try-on config not found", HttpStatus.NOT_FOUND),
    TRY_ON_MODEL_FILE_REQUIRED(2014, "Try-on model file is required", HttpStatus.BAD_REQUEST),
    TRY_ON_MODEL_INVALID_FORMAT(2015, "Only .glb files are supported for try-on model", HttpStatus.BAD_REQUEST),
    TRY_ON_MODEL_INVALID_SCALE(2016, "Scale value must be greater than 0", HttpStatus.BAD_REQUEST),


    PAYMENT_METHOD_NOT_SUPPORTED(1027, "Payment is not applicable for this checkout", HttpStatus.BAD_REQUEST),
    DEPOSIT_PAYMENT_METHOD_REQUIRED(1033, "Deposit payment method is required when payment method is COD", HttpStatus.BAD_REQUEST),
    PAYMENT_IN_PROGRESS(1028, "These cart items already have a pending online payment", HttpStatus.CONFLICT),
    GHN_COD_OVER_LIMIT(1029, "COD amount exceeds GHN limit", HttpStatus.BAD_REQUEST),
    GHN_CREATE_ORDER_FAILED(1030, "Create GHN shipping order failed", HttpStatus.BAD_REQUEST),
    GHN_SYNC_FAILED(1031, "Sync GHN shipping status failed", HttpStatus.BAD_REQUEST),
    ORDER_NOT_REACHED_EXPECTED_DELIVERY(1032, "Order has not reached expected delivery time", HttpStatus.BAD_REQUEST),
    INVENTORY_RECORD_NOT_FOUND(1034, "Inventory record not found", HttpStatus.NOT_FOUND),
    INVENTORY_INSUFFICIENT_QUANTITY(1035, "Inventory quantity is insufficient", HttpStatus.BAD_REQUEST),

    USERNAME_REQUIRED(1012, "Username is required", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1004, "Username must be at least 8 characters!", HttpStatus.BAD_REQUEST),

    PASSWORD_REQUIRED(1013, "Password is required", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1005, "Password must be at least 8 chars and include uppercase, lowercase, digit!", HttpStatus.BAD_REQUEST),

    EMAIL_REQUIRED(1014, "Email is required", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1015, "Email is invalid", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(1009, "Email already exists", HttpStatus.BAD_REQUEST),

    PHONE_REQUIRED(1016, "Phone is required", HttpStatus.BAD_REQUEST),
    PHONE_INVALID(1017, "Phone must be 10 or 11 digits", HttpStatus.BAD_REQUEST),

    NAME_REQUIRED(1018, "Name is required", HttpStatus.BAD_REQUEST),
    NAME_INVALID(1019, "Name is invalid", HttpStatus.BAD_REQUEST),

    DOB_REQUIRED(1020, "Date of birth is required", HttpStatus.BAD_REQUEST),
    DOB_INVALID(1008, "Date of birth must be before today", HttpStatus.BAD_REQUEST),

    ADDRESS_INVALID(1021, "Address is invalid", HttpStatus.BAD_REQUEST),

    IDNUMBER_INVALID(1022, "Id number must be exactly 12 digits", HttpStatus.BAD_REQUEST),
    IDNUMBER_EXISTED(1011, "Id number already exists", HttpStatus.BAD_REQUEST),

    ROLE_NOT_FOUND(1010, "Role not found", HttpStatus.NOT_FOUND),

    // Product, Frame, Lens
    CONTACT_LENS_NOT_FOUND(2001, "Contact lens not found", HttpStatus.NOT_FOUND),
    FRAME_NOT_FOUND(2002, "Frame not found", HttpStatus.NOT_FOUND),
    LENS_NOT_FOUND(2003, "Lens not found", HttpStatus.NOT_FOUND),
    CART_ITEM_NOT_FOUND(2004, "Cart item not found", HttpStatus.NOT_FOUND),
    CART_ITEM_ID_REQUIRED(2005, "Cart item id is required", HttpStatus.BAD_REQUEST),
    CART_ITEM_INVALID_FOR_CHECKOUT(2011, "Cart items are invalid for checkout", HttpStatus.BAD_REQUEST),
    PRESCRIPTION_OCR_NOT_CONFIGURED(2006, "Prescription OCR is not configured", HttpStatus.SERVICE_UNAVAILABLE),
    PRESCRIPTION_OCR_FAILED(2007, "Prescription OCR failed", HttpStatus.BAD_GATEWAY),
    PRESCRIPTION_OCR_EMPTY_TEXT(2008, "No readable prescription text found", HttpStatus.BAD_REQUEST),
    SKU_ALREADY_EXISTS(2009, "SKU already exists", HttpStatus.CONFLICT),
    SKU_REQUIRED(2010, "SKU is required", HttpStatus.BAD_REQUEST),
    CHATBOT_MESSAGE_REQUIRED(2020, "Chatbot message is required", HttpStatus.BAD_REQUEST),
    CHATBOT_MESSAGE_TOO_LONG(2021, "Chatbot message must not exceed 1000 characters", HttpStatus.BAD_REQUEST),
    GEMINI_NOT_CONFIGURED(2017, "Gemini chatbot is not configured", HttpStatus.SERVICE_UNAVAILABLE),
    GEMINI_REQUEST_FAILED(2018, "Gemini request failed", HttpStatus.BAD_GATEWAY),
    GEMINI_INVALID_RESPONSE(2019, "Gemini returned an invalid response", HttpStatus.BAD_GATEWAY),
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode httpStatusCode;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}

