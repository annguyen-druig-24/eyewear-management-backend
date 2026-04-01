package com.swp391.eyewear_management_backend.service;

/*
    - Interface nghiệp vụ callback và enum kết quả xử lý.
*/

public interface VnpayCallbackService {

    /**
     * Handle VNPAY IPN/return callback to confirm a payment.
     *
     * @param paymentId        id của Payment trong hệ thống
     * @param vnpAmount        số tiền VNPAY trả về (thường = amount * 100)
     * @param vnpResponseCode  mã phản hồi VNPAY (00 = success)
     * @param vnpTransactionStatus trạng thái giao dịch VNPAY (00 = thành công, 02 = thất bại/cancel)
     * @return kết quả xử lý để controller/IPN trả đúng response code cho VNPAY
     */
    //IpResult handleCallback(Long paymentId, long vnpAmount, String vnpResponseCode);
    IpResult handleCallback(Long paymentId, long vnpAmount, String vnpResponseCode, String vnpTransactionStatus);

    enum IpResult {
        CONFIRM_SUCCESS,        // RspCode 00
        ALREADY_CONFIRMED,      // RspCode 02
        INVALID_AMOUNT,         // RspCode 04
        ORDER_NOT_FOUND,        // RspCode 01
        CONFIRM_FAILED          // vẫn trả 00 cho VNPAY nhưng trong DB status FAILED
    }
}
