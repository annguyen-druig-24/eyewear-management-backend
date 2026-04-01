package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.config.ghn.GhnProperties;
import com.swp391.eyewear_management_backend.dto.request.ShippingAddressRequest;
import com.swp391.eyewear_management_backend.entity.CartItem;
import com.swp391.eyewear_management_backend.integration.ghn.GhnShippingClient;
import com.swp391.eyewear_management_backend.service.GhnShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GhnShippingServiceImpl implements GhnShippingService {

    private final GhnShippingClient client;
    private final GhnProperties props;

    @Override
    public ShippingResult calculate(List<CartItem> cartItems, ShippingAddressRequest address) {
        if (address == null
                || address.getDistrictCode() == null
                || address.getWardCode() == null) {
            return new ShippingResult(BigDecimal.ZERO, null);
        }

        PackageSpec spec = buildPackage(cartItems); //tính trọng lượng đơn hàng theo các số đo đc set sẵn trong config vì khối lượng nhỏ

        // 1) get serviceId
        int serviceId = chooseServiceId(address.getDistrictCode());

        // 2) fee
        Map<String, Object> feeBody = new HashMap<>();
        feeBody.put("from_district_id", props.getFromDistrictId());
        feeBody.put("from_ward_code", props.getFromWardCode());
        feeBody.put("to_district_id", address.getDistrictCode());
        feeBody.put("to_ward_code", address.getWardCode());
        feeBody.put("service_id", serviceId);

        feeBody.put("height", spec.height);
        feeBody.put("length", spec.length);
        feeBody.put("width", spec.width);
        feeBody.put("weight", spec.weightGram);

        Map<String, Object> feeResp = client.fee(feeBody);
        Map<String, Object> feeData = (Map<String, Object>) feeResp.get("data");
        Number totalFee = (Number) feeData.get("total");
        BigDecimal shippingFee = new BigDecimal(totalFee.longValue());

        // 3) leadtime
        Map<String, Object> ltBody = new HashMap<>();
        ltBody.put("from_district_id", props.getFromDistrictId());
        ltBody.put("from_ward_code", props.getFromWardCode());
        ltBody.put("to_district_id", address.getDistrictCode());
        ltBody.put("to_ward_code", address.getWardCode());
        ltBody.put("service_id", serviceId);

        Map<String, Object> ltResp = client.leadtime(ltBody);
        Map<String, Object> ltData = (Map<String, Object>) ltResp.get("data");
        Number leadtime = (Number) ltData.get("leadtime");

        LocalDateTime expected = null;
        if (leadtime != null) {
            expected = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(leadtime.longValue()),
                    ZoneId.of("Asia/Ho_Chi_Minh")
            );
        }

        return new ShippingResult(shippingFee, expected);
    }

    private int chooseServiceId(int toDistrictId) {
        List<Map<String, Object>> services = client.availableServices(toDistrictId);
        if (services == null || services.isEmpty()) {
            throw new RuntimeException("GHN: No available services");
        }

        // ưu tiên service_type_id = props.preferredServiceTypeId (mặc định 2)
        for (Map<String, Object> s : services) {
            Number typeId = (Number) s.get("service_type_id");
            if (typeId != null && typeId.intValue() == props.getPreferredServiceTypeId()) {
                return ((Number) s.get("service_id")).intValue();
            }
        }
        return ((Number) services.get(0).get("service_id")).intValue();
    }

    /**
     * MVP: dùng box profiles đơn giản (không cần weight/size theo từng product).
     * - Contact lens: nhỏ
     * - Frame: vừa
     * - Prescription (frame+lens): lớn hơn
     * - Mix: lấy max size + cộng weight theo qty
     */
    private PackageSpec buildPackage(List<CartItem> items) {
        // Size cố định lấy từ config
        int length = props.getPkg().getLength();
        int width  = props.getPkg().getWidth();
        int height = props.getPkg().getHeight();

        int totalWeight = 0;

        for (CartItem ci : items) {
            int qty = ci.getQuantity() == null || ci.getQuantity() <= 0 ? 1 : ci.getQuantity();

            boolean hasFrame = ci.getFrame() != null;
            boolean hasLens  = ci.getLens() != null;
            boolean hasCL    = ci.getContactLens() != null;

            int weightPerUnit;
            if (hasFrame && hasLens) {
                weightPerUnit = props.getPkg().getWeight().getPrescription(); // 450
            } else if (hasFrame) {
                weightPerUnit = props.getPkg().getWeight().getFrame(); // 300
            } else if (hasLens) {
                weightPerUnit = props.getPkg().getWeight().getLens(); // 220
            } else if (hasCL) {
                weightPerUnit = props.getPkg().getWeight().getContactLens(); // 120
            } else {
                weightPerUnit = props.getPkg().getWeight().getDef(); // fallback
            }

            totalWeight += weightPerUnit * qty;
        }

        return new PackageSpec(length, width, height, totalWeight);
    }

    private static class PackageSpec {
        final int length, width, height, weightGram;
        PackageSpec(int length, int width, int height, int weightGram) {
            this.length = length;
            this.width = width;
            this.height = height;
            this.weightGram = weightGram;
        }
    }
}
