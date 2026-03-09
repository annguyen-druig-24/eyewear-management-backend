package com.swp391.eyewear_management_backend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItemRequest {

    Long contactLensId;
    Long frameId;
    Long lensId;

    @NotNull(message = "QUANTITY_REQUIRED")
    @Min(value = 1, message = "QUANTITY_MUST_BE_GREATER_THAN_0")
    Integer quantity;

    @DecimalMin(value = "0.0", inclusive = false, message = "FRAME_PRICE_MUST_BE_GREATER_THAN_0")
    Double framePrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "LENS_PRICE_MUST_BE_GREATER_THAN_0")
    Double lensPrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "PRICE_MUST_BE_GREATER_THAN_0")
    Double price;

    // Prescription fields (optional) - liên kết với bảng Cart_Item_Prescription
    Double rightEyeSph;
    Double rightEyeCyl;
    Integer rightEyeAxis;
    @JsonAlias({"rightADD", "rightAdd", "RIGHT_ADD", "right_add"})
    Double rightEyeAdd;

    Double leftEyeSph;
    Double leftEyeCyl;
    Integer leftEyeAxis;
    @JsonAlias({"leftADD", "leftAdd", "LEFT_ADD", "left_add"})
    Double leftEyeAdd;

    // Pupillary Distance
    @JsonAlias({"PD", "Pd", "pD"})
    Double pd;
    @JsonAlias({"PD_Right", "PDRight", "pd_right", "rightPD", "rightPd", "pdright", "PD_RIGHT"})
    Double pdRight;
    @JsonAlias({"PD_Left", "PDLeft", "pd_left", "leftPD", "leftPd", "pdleft", "PD_LEFT"})
    Double pdLeft;
}
