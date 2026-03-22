package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.request.InventoryReceiptRequest;
import com.swp391.eyewear_management_backend.dto.request.ReceiptDetailRequest;
import com.swp391.eyewear_management_backend.dto.response.InventoryReceiptConformResponse;
import com.swp391.eyewear_management_backend.dto.response.InventoryReceiptResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductOfSupplierResponse;
import com.swp391.eyewear_management_backend.entity.*;
import com.swp391.eyewear_management_backend.exception.AppException;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import com.swp391.eyewear_management_backend.mapper.InventoryReceiptMapper;
import com.swp391.eyewear_management_backend.mapper.ProductMapper;
import com.swp391.eyewear_management_backend.repository.*;
import com.swp391.eyewear_management_backend.service.InventoryReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryReceiptServiceImpl implements InventoryReceiptService {

    private final InventoryReceiptRepository receiptRepo;
    private final InventoryReceiptDetailRepository detailRepo;
    private final ProductRepo productRepo;
    private final SupplierRepository supplierRepo;
    private final UserRepo userRepo;

    private final InventoryReceiptMapper receiptMapper;
    private final ProductMapper productMapper;
    @Override
    @Transactional(readOnly = true)
    public List<ProductOfSupplierResponse> getProductsBySupplierId(Long supplierId) {
        return productRepo.findProductsBySupplierId(supplierId)
                .stream()
                .map(productMapper::toProductOfSupplierResponse) // Đã sửa lại thành receiptMapper
                .collect(Collectors.toList());
    }

    /**
     * Lấy user hiện tại từ security context
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String username = auth.getName();
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    /**
     * Hàm hỗ trợ tạo mã Receipt Code tự động, duy nhất 100%
     * Format: IR-yyMMdd-UUID (VD: IR-260322-A1B2)
     */
    private String generateUniqueReceiptCode() {
        String code;
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));

        do {
            String randomPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            code = "IR-" + datePart + "-" + randomPart;
        } while (receiptRepo.existsByReceiptCode(code));

        return code;
    }

    @Override
    @Transactional
    public InventoryReceiptResponse createInventoryReceipt(InventoryReceiptRequest request) {
        Supplier supplier = supplierRepo.findById(request.getSupplierId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_SUPPLIER));

        User creator = getCurrentUser();

        // 1. Tạo Entity Phiếu Nhập (Master)
        InventoryReceipt receipt = new InventoryReceipt();
        receipt.setReceiptCode(generateUniqueReceiptCode());
        receipt.setSupplier(supplier);
        receipt.setCreatedBy(creator);

        receipt.setOrderDate(LocalDateTime.now());
        receipt.setReceivedDate(null);
        receipt.setStatus("ORDERED");
        receipt.setNote(request.getNote());

        receipt.setTotalAmount(BigDecimal.ZERO); // Fix lỗi NULL Total_Amount

        InventoryReceipt savedReceipt = receiptRepo.save(receipt);

        BigDecimal grandTotal = BigDecimal.ZERO;
        List<InventoryReceiptDetail> detailList = new ArrayList<>();

        // ---> TỐI ƯU HÓA: Lấy trước toàn bộ Product ID của Supplier này đưa vào Set
        Set<Long> validProductIdsForSupplier = productRepo.findProductsBySupplierId(supplier.getSupplierID())
                .stream()
                .map(Product::getProductID) // Giả sử entity Product của bạn lấy id bằng getId()
                .collect(Collectors.toSet());

        // 2. Vòng lặp xử lý chi tiết
        for (ReceiptDetailRequest item : request.getDetails()) {

            Product product = productRepo.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + item.getProductId()));

            // ================================================================
            // VALIDATE 1: SẢN PHẨM CÓ THUỘC NHÀ CUNG CẤP KHÔNG?
            // ================================================================
            if (!validProductIdsForSupplier.contains(item.getProductId())) {
                throw new RuntimeException("Lỗi: Sản phẩm [" + product.getProductName() + "] không do nhà cung cấp này phân phối!");
            }

            // ================================================================
            // VALIDATE 2: GIÁ NHẬP (UNIT COST) CÓ KHỚP VỚI DATABASE (COST PRICE)?
            // ================================================================
            // (Lưu ý: Dùng getCostPrice() dựa theo cột Cost_Price trong DB của bạn)
            if (item.getUnitCost().compareTo(product.getCostPrice()) != 0) {
                throw new RuntimeException("Lỗi: Giá nhập của sản phẩm [" + product.getProductName() + "] gửi lên (" + item.getUnitCost() + ") bị sai lệch so với hệ thống (" + product.getCostPrice() + ")!");
            }

            // --- VALIDATE 3 (Của bạn): KIỂM TRA THÀNH TIỀN ---
            BigDecimal totalBeforeTax = item.getUnitCost().multiply(new BigDecimal(item.getQuantity()));
            BigDecimal vatPercentage = item.getVatRate() != null ? item.getVatRate() : BigDecimal.ZERO;
            BigDecimal vatAmount = totalBeforeTax.multiply(vatPercentage).divide(new BigDecimal("100"));
            BigDecimal expectedDetailTotal = totalBeforeTax.add(vatAmount);

            if (expectedDetailTotal.compareTo(item.getTotalPrice()) != 0) {
                throw new RuntimeException("Lỗi dữ liệu: Thành tiền của sản phẩm ID "
                        + item.getProductId() + " bị tính sai!");
            }

            // Tạo Entity Detail để lưu vào DB
            InventoryReceiptDetail detail = new InventoryReceiptDetail();
            detail.setInventoryReceipt(savedReceipt);
            detail.setProduct(product);
            detail.setOrderedQuantity(item.getQuantity());
            detail.setReceivedQuantity(0);
            detail.setRejectedQuantity(0);
            detail.setUnitCost(item.getUnitCost());
            detail.setNote(item.getNote());
            detail.setTotalPrice(item.getTotalPrice());

            detailList.add(detail);
            grandTotal = grandTotal.add(item.getTotalPrice());
        }

        savedReceipt.setTotalAmount(grandTotal);
        receiptRepo.save(savedReceipt);

        detailRepo.saveAll(detailList);

        return receiptMapper.toResponse(savedReceipt);
    }

    @Override
    public List<InventoryReceiptResponse> getAllReceipts() {
        return receiptRepo.findAll().stream()
                .map(receiptMapper::toResponse)
                .collect(Collectors.toList());
    }

    // Thêm hàm này vào trong class InventoryReceiptServiceImpl
    @Override
    @Transactional(readOnly = true)
    public InventoryReceiptConformResponse getReceiptById(Long id) {
        // Tìm phiếu nhập theo ID
        InventoryReceipt receipt = receiptRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập kho có ID: " + id));

        // Dùng mapper chuyển đổi từ Entity sang ConformResponse và trả về
        return receiptMapper.toConformResponse(receipt);
    }
}