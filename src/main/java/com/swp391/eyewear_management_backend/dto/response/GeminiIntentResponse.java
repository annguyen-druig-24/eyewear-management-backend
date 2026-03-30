package com.swp391.eyewear_management_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiIntentResponse {

    String productType;
    String brand;
    Double minPrice;
    Double maxPrice;

    @Builder.Default
    List<String> keywords = new ArrayList<>();

    Boolean premium;
    Boolean needsClarification;
    String clarificationQuestion;
}
