package com.swp391.eyewear_management_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.eyewear_management_backend.dto.request.ReturnExchangeRequest;
import com.swp391.eyewear_management_backend.dto.response.ApiResponse;
import com.swp391.eyewear_management_backend.dto.response.ReturnExchangeResponse;
import com.swp391.eyewear_management_backend.service.ReturnExchangeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/return-exchanges")
@CrossOrigin(origins = "http://localhost:3000")
public class ReturnExchangeController {

    @Autowired
    private ReturnExchangeService returnExchangeService;

    /**
     * Tạo yêu cầu đổi trả
     *
     * Phần 1: Nhận cục JSON (chứa cả cha lẫn list con)
     * Phần 2: Nhận danh sách File ảnh tương ứng với từng item con
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> createReturnExchange(
            @RequestPart("request") @Valid ReturnExchangeRequest request,
            @RequestPart(value = "itemImages", required = true) List<MultipartFile> itemImages,
            @RequestPart(value = "customerImageQr", required = false) MultipartFile customerImageQr) {

        // Truyền thêm danh sách ảnh xuống Service v1
        returnExchangeService.createReturnExchange(request, itemImages, customerImageQr);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<String>builder()
                        .code(1000)
                        .message("Success")
                        .result("Return exchange request created successfully")
                        .build());
    }

    /**
     * Lấy chi tiết yêu cầu đổi trả theo ID
     */
    @GetMapping("/{returnExchangeId}")
    public ResponseEntity<ApiResponse<ReturnExchangeResponse>> getReturnExchange(
            @PathVariable Long returnExchangeId) {
        ReturnExchangeResponse response = returnExchangeService.getReturnExchangeById(returnExchangeId);
        return ResponseEntity.ok(ApiResponse.<ReturnExchangeResponse>builder()
                .code(1000)
                .message("Return exchange retrieved successfully")
                .result(response)
                .build());
    }

    /**
     * Lấy yêu cầu đổi trả theo return code
     */
    @GetMapping("/code/{returnCode}")
    public ResponseEntity<ApiResponse<ReturnExchangeResponse>> getReturnExchangeByCode(
            @PathVariable String returnCode) {
        ReturnExchangeResponse response = returnExchangeService.getReturnExchangeByCode(returnCode);
        return ResponseEntity.ok(ApiResponse.<ReturnExchangeResponse>builder()
                .code(1000)
                .message("Return exchange retrieved successfully")
                .result(response)
                .build());
    }

    /**
     * Lấy tất cả yêu cầu đổi trả của user hiện tại
     */
    @GetMapping("/my-requests")
    public ResponseEntity<ApiResponse<List<ReturnExchangeResponse>>> getMyReturnExchanges() {
        List<ReturnExchangeResponse> response = returnExchangeService.getMyReturnExchanges();
        return ResponseEntity.ok(ApiResponse.<List<ReturnExchangeResponse>>builder()
                .code(1000)
                .message("My return exchanges retrieved successfully")
                .result(response)
                .build());
    }

    /**
     * Lấy tất cả yêu cầu đổi trả (cho admin)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ReturnExchangeResponse>>> getAllReturnExchanges() {
        List<ReturnExchangeResponse> response = returnExchangeService.getAllReturnExchanges();
        return ResponseEntity.ok(ApiResponse.<List<ReturnExchangeResponse>>builder()
                .code(1000)
                .message("All return exchanges retrieved successfully")
                .result(response)
                .build());
    }

    /**
     * Lấy yêu cầu đổi trả theo trạng thái
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ReturnExchangeResponse>>> getReturnExchangesByStatus(
            @PathVariable String status) {
        List<ReturnExchangeResponse> response = returnExchangeService.getReturnExchangesByStatus(status);
        return ResponseEntity.ok(ApiResponse.<List<ReturnExchangeResponse>>builder()
                .code(1000)
                .message("Return exchanges retrieved successfully")
                .result(response)
                .build());
    }

    /**
     * Duyệt yêu cầu đổi trả
     */
    @PutMapping("/{returnExchangeId}/approve")
    public ResponseEntity<ApiResponse<ReturnExchangeResponse>> approveReturnExchange(
            @PathVariable Long returnExchangeId) {
        ReturnExchangeResponse response = returnExchangeService.approveReturnExchange(returnExchangeId);
        return ResponseEntity.ok(ApiResponse.<ReturnExchangeResponse>builder()
                .code(1000)
                .message("Return exchange approved successfully")
                .result(response)
                .build());
    }

    /**
     * Từ chối yêu cầu đổi trả
     */
    @PutMapping("/{returnExchangeId}/reject")
    public ResponseEntity<ApiResponse<ReturnExchangeResponse>> rejectReturnExchange(
            @PathVariable Long returnExchangeId,
            @RequestParam String rejectReason) {
        ReturnExchangeResponse response = returnExchangeService.rejectReturnExchange(returnExchangeId, rejectReason);
        return ResponseEntity.ok(ApiResponse.<ReturnExchangeResponse>builder()
                .code(1000)
                .message("Return exchange rejected successfully")
                .result(response)
                .build());
    }

//    /**
//     * Cập nhật yêu cầu đổi trả
//     */
//    @PutMapping("/{returnExchangeId}")
//    public ResponseEntity<ApiResponse<ReturnExchangeResponse>> updateReturnExchange(
//            @PathVariable Long returnExchangeId,
//            @RequestBody ReturnExchangeRequest request) {
//        ReturnExchangeResponse response = returnExchangeService.updateReturnExchange(returnExchangeId, request);
//        return ResponseEntity.ok(ApiResponse.<ReturnExchangeResponse>builder()
//                .code(1000)
//                .message("Return exchange updated successfully")
//                .result(response)
//                .build());
//    }

    /**
     * Xóa yêu cầu đổi trả
     */
    @DeleteMapping("/{returnExchangeId}")
    public ResponseEntity<ApiResponse<Void>> deleteReturnExchange(
            @PathVariable Long returnExchangeId) {
        returnExchangeService.deleteReturnExchange(returnExchangeId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(1000)
                .message("Return exchange deleted successfully")
                .build());
    }
}
