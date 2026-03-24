package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.request.InventoryReceiptRequest;
import com.swp391.eyewear_management_backend.dto.request.InventoryReceiptReceiveDetailRequest;
import com.swp391.eyewear_management_backend.dto.request.InventoryReceiptReceiveRequest;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    // Thêm 2 repository mới để ghi log kho
    private final InventoryRepo inventoryRepo;
    private final InventoryTransactionRepo transactionRepo;

    private final InventoryReceiptMapper receiptMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProductOfSupplierResponse> getProductsBySupplierId(Long supplierId) {
        return productRepo.findProductsBySupplierId(supplierId)
                .stream()
                .map(productMapper::toProductOfSupplierResponse)
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
        // Cập nhật trạng thái mới
        receipt.setStatus("Pending Verification");
        receipt.setNote(request.getNote());
        receipt.setTotalAmount(BigDecimal.ZERO);

        InventoryReceipt savedReceipt = receiptRepo.save(receipt);

        BigDecimal grandTotal = BigDecimal.ZERO;
        List<InventoryReceiptDetail> detailList = new ArrayList<>();

        // TỐI ƯU HÓA: Lấy trước toàn bộ Product ID của Supplier này đưa vào Set
        Set<Long> validProductIdsForSupplier = productRepo.findProductsBySupplierId(supplier.getSupplierID())
                .stream()
                .map(Product::getProductID)
                .collect(Collectors.toSet());

        // TỐI ƯU HÓA: preload toàn bộ Product theo danh sách request để tránh N+1 (findById trong vòng lặp)
        Set<Long> requestedProductIds = request.getDetails().stream()
                .map(ReceiptDetailRequest::getProductId)
                .collect(Collectors.toSet());

        Map<Long, Product> requestedProductMap = productRepo.findAllById(requestedProductIds).stream()
                .collect(Collectors.toMap(Product::getProductID, p -> p));

        // 2. Vòng lặp xử lý chi tiết
        for (ReceiptDetailRequest item : request.getDetails()) {

//            Product product = productRepo.findById(item.getProductId())
//                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + item.getProductId()));
            Product product = requestedProductMap.get(item.getProductId());
            if (product == null) {
                throw new RuntimeException("Không tìm thấy sản phẩm ID: " + item.getProductId());
            }

            // VALIDATE 1: SẢN PHẨM CÓ THUỘC NHÀ CUNG CẤP KHÔNG?
            if (!validProductIdsForSupplier.contains(item.getProductId())) {
                throw new RuntimeException("Lỗi: Sản phẩm [" + product.getProductName() + "] không do nhà cung cấp này phân phối!");
            }

            // VALIDATE 2: GIÁ NHẬP (UNIT COST) CÓ KHỚP VỚI DATABASE (COST PRICE)?
            if (item.getUnitCost().compareTo(product.getCostPrice()) != 0) {
                throw new RuntimeException("Lỗi: Giá nhập của sản phẩm [" + product.getProductName() + "] gửi lên (" + item.getUnitCost() + ") bị sai lệch so với hệ thống (" + product.getCostPrice() + ")!");
            }

            // VALIDATE 3: KIỂM TRA THÀNH TIỀN
            BigDecimal totalBeforeTax = item.getUnitCost().multiply(new BigDecimal(item.getQuantity()));
            BigDecimal vatPercentage = item.getVatRate() != null ? item.getVatRate() : BigDecimal.ZERO;
            BigDecimal vatAmount = totalBeforeTax.multiply(vatPercentage).divide(new BigDecimal("100"));
            BigDecimal expectedDetailTotal = totalBeforeTax.add(vatAmount);

            if (expectedDetailTotal.compareTo(item.getTotalPrice()) != 0) {
                throw new RuntimeException("Lỗi dữ liệu: Thành tiền của sản phẩm ID " + item.getProductId() + " bị tính sai!");
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

    @Override
    @Transactional(readOnly = true)
    public InventoryReceiptConformResponse getReceiptById(Long id) {
        InventoryReceipt receipt = receiptRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập kho có ID: " + id));
        return receiptMapper.toConformResponse(receipt);
    }

    @Override
    @Transactional
    public InventoryReceiptConformResponse receiveReceipt(Long id, InventoryReceiptReceiveRequest request) {
        if (request == null || request.getInventoryReceiptId() == null || !request.getInventoryReceiptId().equals(id)) {
            throw new RuntimeException("Dữ liệu yêu cầu không hợp lệ hoặc ID phiếu nhập không khớp.");
        }

        InventoryReceipt receipt = receiptRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập kho: " + id));

        // Cập nhật trạng thái so sánh
        if (!"Pending Verification".equals(receipt.getStatus()) && !"Partially Entered".equals(receipt.getStatus())) {
            throw new RuntimeException("Chỉ có thể xác nhận phiếu nhập ở trạng thái Pending Verification hoặc Partially Entered.");
        }

        User currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        List<Inventory> inventoryLogs = new ArrayList<>();
        List<InventoryTransaction> transactions = new ArrayList<>();

        BigDecimal calculatedActualTotal = BigDecimal.ZERO;
        boolean isFullyReceived = true;

        // Map chi tiết phiếu nhập hiện tại để dễ dàng tìm kiếm
        Map<Long, InventoryReceiptDetail> detailMap = receipt.getDetails().stream()
                .collect(Collectors.toMap(InventoryReceiptDetail::getInventoryReceiptDetailID, d -> d));

        for (InventoryReceiptReceiveDetailRequest item : request.getDetails()) {
            InventoryReceiptDetail detail = detailMap.get(item.getReceiptDetailId());
            if (detail == null) {
                throw new RuntimeException("Chi tiết phiếu nhập không tồn tại: " + item.getReceiptDetailId());
            }

            Product product = detail.getProduct();

            int receivedQty = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
            if (receivedQty < 0 || receivedQty > detail.getOrderedQuantity()) {
                throw new RuntimeException("Số lượng thực nhận không hợp lệ cho sản phẩm: " + product.getProductName());
            }

            // --- KIỂM TRA TÍNH TOÁN ACTUAL TOTAL PRICE TỪ CLIENT ---
            BigDecimal actualTotalPrice = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal unitCost = detail.getUnitCost();
            if (item.getUnitCost() != null && item.getUnitCost().compareTo(unitCost) != 0) {
                throw new RuntimeException("Đơn giá thực nhận không khớp dữ liệu đã lưu cho sản phẩm: " + product.getProductName());
            }

            // Tính VAT từ dữ liệu đã lưu của chi tiết phiếu nhập trong DB.
            BigDecimal vatPercentage = BigDecimal.ZERO;
            BigDecimal orderedSubtotal = detail.getUnitCost().multiply(BigDecimal.valueOf(detail.getOrderedQuantity()));
            if (orderedSubtotal.compareTo(BigDecimal.ZERO) > 0 && detail.getTotalPrice() != null) {
                BigDecimal orderedVatAmount = detail.getTotalPrice().subtract(orderedSubtotal);
                if (orderedVatAmount.compareTo(BigDecimal.ZERO) > 0) {
                    vatPercentage = orderedVatAmount
                            .multiply(BigDecimal.valueOf(100))
                            .divide(orderedSubtotal, 6, RoundingMode.HALF_UP);
                }
            }

            BigDecimal expectedTotalBeforeTax = unitCost.multiply(BigDecimal.valueOf(receivedQty));
            BigDecimal expectedVatAmount = expectedTotalBeforeTax.multiply(vatPercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal expectedActualTotalPrice = expectedTotalBeforeTax.add(expectedVatAmount);

            // Cho phép sai số nhỏ do làm tròn (VD: 0.01)
            if (expectedActualTotalPrice.subtract(actualTotalPrice).abs().compareTo(new BigDecimal("0.01")) > 0) {
                throw new RuntimeException("Lỗi dữ liệu: Thành tiền thực tế của sản phẩm ID " + product.getProductID() + " tính toán sai lệch!");
            }
            // --------------------------------------------------------

            // 1. Cập nhật số liệu thực tế cho Detail
            detail.setReceivedQuantity(receivedQty);
            detail.setRejectedQuantity(detail.getOrderedQuantity() - receivedQty);
            detail.setActualTotalPrice(actualTotalPrice);
            if (item.getNote() != null) {
                detail.setNote(item.getNote());
            }

            // Chỉ ghi nhận log và transaction nếu có số lượng nhập > 0
            if (receivedQty > 0) {
                int qtyBefore = product.getOnHandQuantity() == null ? 0 : product.getOnHandQuantity();
                int qtyAfter = qtyBefore + receivedQty;

                // 2. Lưu log Inventory
                Inventory inventory = new Inventory();
                inventory.setProduct(product);
                inventory.setQuantityBefore(qtyBefore);
                inventory.setQuantityAfter(qtyAfter);
                inventory.setUser(currentUser);
                inventory.setSupplier(receipt.getSupplier());
                inventory.setOrderDate(receipt.getOrderDate());
                inventory.setReceivedDate(now);
                inventoryLogs.add(inventory);

                // 3. Lưu log InventoryTransaction
                InventoryTransaction transaction = new InventoryTransaction();
                transaction.setProduct(product);
                transaction.setTransactionType("RECEIPT_IN");
                transaction.setQuantityChange(receivedQty);
                transaction.setQuantityBefore(qtyBefore);
                transaction.setQuantityAfter(qtyAfter);
                transaction.setReferenceType("INVENTORY_RECEIPT");
                transaction.setReferenceID(receipt.getInventoryReceiptID());
                transaction.setInventoryReceipt(receipt);
                transaction.setPerformedBy(currentUser);
                transaction.setPerformedAt(now);
                if (item.getNote() != null && !item.getNote().isBlank()) {
                    transaction.setNote("Xác nhận nhập kho từ phiếu: " + receipt.getReceiptCode() + " - " + item.getNote());
                } else {
                    transaction.setNote("Xác nhận nhập kho từ phiếu: " + receipt.getReceiptCode());
                }
                transactions.add(transaction);

                // 4. Cập nhật số lượng Product (Available Quantity là computed column)
                product.setOnHandQuantity(qtyAfter);
            }

            if (receivedQty < detail.getOrderedQuantity()) {
                isFullyReceived = false;
            }

            calculatedActualTotal = calculatedActualTotal.add(actualTotalPrice);
        }

        // Validate tổng tiền thực tế client gửi lên khớp với tổng cộng các chi tiết
        // Cho phép sai số nhỏ
        if (request.getTotalAmount() != null && request.getTotalAmount().subtract(calculatedActualTotal).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new RuntimeException("Tổng tiền thực tế không khớp với tổng các chi tiết thực tế cộng lại!");
        }

        // 5. Cập nhật Receipt Master
        receipt.setActualTotalAmount(calculatedActualTotal);
        // Cập nhật trạng thái mới
        receipt.setStatus(isFullyReceived ? "Fully Entered" : "Partially Entered");
        receipt.setReceivedBy(currentUser);
        receipt.setReceivedDate(now);

        // Lưu vào Database
        inventoryRepo.saveAll(inventoryLogs);
        transactionRepo.saveAll(transactions);
        InventoryReceipt savedReceipt = receiptRepo.save(receipt);

        return receiptMapper.toConformResponse(savedReceipt);
    }
}