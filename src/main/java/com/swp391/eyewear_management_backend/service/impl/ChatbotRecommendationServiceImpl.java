package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.config.FrontendProperties;
import com.swp391.eyewear_management_backend.dto.request.ChatbotRecommendRequest;
import com.swp391.eyewear_management_backend.dto.response.BrandResponse;
import com.swp391.eyewear_management_backend.dto.response.ChatbotFiltersResponse;
import com.swp391.eyewear_management_backend.dto.response.ChatbotProductItemResponse;
import com.swp391.eyewear_management_backend.dto.response.ChatbotRecommendResponse;
import com.swp391.eyewear_management_backend.dto.response.GeminiIntentResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductDetailResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductResponse;
import com.swp391.eyewear_management_backend.dto.response.TopProductResponse;
import com.swp391.eyewear_management_backend.dto.response.extend.ContactLensResponse;
import com.swp391.eyewear_management_backend.dto.response.extend.FrameResponse;
import com.swp391.eyewear_management_backend.dto.response.extend.LensResponse;
import com.swp391.eyewear_management_backend.service.ChatbotRecommendationService;
import com.swp391.eyewear_management_backend.service.DashboardService;
import com.swp391.eyewear_management_backend.service.GeminiIntentService;
import com.swp391.eyewear_management_backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotRecommendationServiceImpl implements ChatbotRecommendationService {

    private static final String PRODUCT_TYPE_FRAME = "FRAME";
    private static final String PRODUCT_TYPE_LENS = "LENS";
    private static final String PRODUCT_TYPE_CONTACT_LENS = "CONTACT_LENS";
    private static final String PRODUCT_TYPE_UNKNOWN = "UNKNOWN";
    private static final double STUDENT_DEFAULT_MAX_PRICE = 1_500_000D;

    private static final Pattern PRICE_RANGE_PATTERN = Pattern.compile(
            "(\\d+(?:[\\.,]\\d+)?)\\s*(trieu|tr|nghin|ngan|k)?\\s*(?:-|den|toi)\\s*(\\d+(?:[\\.,]\\d+)?)\\s*(trieu|tr|nghin|ngan|k)?"
    );
    private static final Pattern PRICE_MAX_PATTERN = Pattern.compile(
            "(?:duoi|toi da|khong qua|under)\\s*(\\d+(?:[\\.,]\\d+)?)\\s*(trieu|tr|nghin|ngan|k)?"
    );
    private static final Pattern PRICE_APPROX_PATTERN = Pattern.compile(
            "(?:khoang|tam|around)\\s*(\\d+(?:[\\.,]\\d+)?)\\s*(trieu|tr|nghin|ngan|k)?"
    );

    private final GeminiIntentService geminiIntentService;
    private final ProductService productService;
    private final DashboardService dashboardService;
    private final FrontendProperties frontendProperties;

    @Override
    public ChatbotRecommendResponse recommendProducts(ChatbotRecommendRequest request) {
        // The chatbot now uses a hybrid strategy: try Gemini first, then fall back
        // to local heuristics so common questions still receive useful answers.
        String normalizedMessage = normalizeText(request.getMessage());

        ChatbotRecommendResponse policyResponse = handlePolicyQuestion(normalizedMessage);
        if (policyResponse != null) {
            return policyResponse;
        }

        ChatbotRecommendResponse specificationResponse = handleSpecificationQuestion(normalizedMessage);
        if (specificationResponse != null) {
            return specificationResponse;
        }

        ChatbotRecommendResponse bestSellerResponse = handleBestSellerQuestion(normalizedMessage);
        if (bestSellerResponse != null) {
            return bestSellerResponse;
        }

        GeminiIntentResponse intent = resolveIntent(request.getMessage(), normalizedMessage);
        List<String> requestedTypes = resolveRequestedTypes(normalizedMessage, intent);

        intent.setKeywords(mergeHeuristicKeywords(intent.getKeywords(), normalizedMessage));
        ChatbotFiltersResponse filters = toFilters(intent, requestedTypes);
        List<ChatbotProductItemResponse> recommendations = findRecommendations(intent, requestedTypes, normalizedMessage);

        return ChatbotRecommendResponse.builder()
                .reply(buildReply(normalizedMessage, intent, recommendations, requestedTypes))
                .needsClarification(false)
                .filters(filters)
                .products(recommendations)
                .build();
    }

    private ChatbotRecommendResponse handlePolicyQuestion(String normalizedMessage) {
        if (!isPolicyQuestion(normalizedMessage)) {
            return null;
        }

        return ChatbotRecommendResponse.builder()
                .reply(buildPolicyReply(normalizedMessage))
                .needsClarification(false)
                .products(List.of())
                .build();
    }

    private ChatbotRecommendResponse handleSpecificationQuestion(String normalizedMessage) {
        if (!isSpecificationQuestion(normalizedMessage)) {
            return null;
        }

        GeminiIntentResponse heuristicIntent = buildHeuristicIntent(normalizedMessage);
        List<String> requestedTypes = resolveRequestedTypes(normalizedMessage, heuristicIntent);
        ProductResponse matchedProduct = findMentionedProduct(normalizedMessage, requestedTypes);
        if (matchedProduct == null) {
            return ChatbotRecommendResponse.builder()
                    .reply("Mình chưa xác định chính xác sản phẩm bạn đang hỏi thông số. Bạn có thể gửi lại tên sản phẩm rõ hơn để mình đọc đúng số đo chi tiết cho bạn.")
                    .needsClarification(false)
                    .filters(toFilters(heuristicIntent, requestedTypes))
                    .products(List.of())
                    .build();
        }

        ProductDetailResponse detail = productService.getProductById(matchedProduct.getId());
        return ChatbotRecommendResponse.builder()
                .reply(buildSpecificationReply(detail))
                .needsClarification(false)
                .filters(toFilters(heuristicIntent, requestedTypes))
                .products(List.of(toChatbotProductItem(matchedProduct, "Sản phẩm bạn đang hỏi thông số chi tiết")))
                .build();
    }

    private ChatbotRecommendResponse handleBestSellerQuestion(String normalizedMessage) {
        if (!isBestSellerQuestion(normalizedMessage)) {
            return null;
        }

        GeminiIntentResponse heuristicIntent = buildHeuristicIntent(normalizedMessage);
        List<String> requestedTypes = resolveRequestedTypes(normalizedMessage, heuristicIntent);
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        List<TopProductResponse> topProducts = resolveBestSellerProducts(requestedTypes, firstDayOfMonth, today);

        if (topProducts.isEmpty()) {
            return ChatbotRecommendResponse.builder()
                    .reply("Hiện mình chưa có đủ dữ liệu bán hàng để gợi ý nhóm best seller đúng với tiêu chí bạn vừa hỏi.")
                    .needsClarification(false)
                    .filters(toFilters(heuristicIntent, requestedTypes))
                    .products(List.of())
                    .build();
        }

        Map<Long, ProductResponse> productsById = new HashMap<>();
        for (ProductResponse product : productService.searchProducts(null, null, null, null)) {
            productsById.put(product.getId(), product);
        }

        List<ChatbotProductItemResponse> recommendations = new ArrayList<>();
        for (TopProductResponse topProduct : topProducts) {
            if (recommendations.size() >= 3) {
                break;
            }

            ProductResponse fullProduct = productsById.get((long) topProduct.getId());
            if (fullProduct != null) {
                recommendations.add(toChatbotProductItem(fullProduct, "Đang thuộc nhóm bán chạy của tháng này"));
                continue;
            }

            recommendations.add(ChatbotProductItemResponse.builder()
                    .id((long) topProduct.getId())
                    .name(topProduct.getName())
                    .price((double) topProduct.getPrice())
                    .imageUrl(topProduct.getImage())
                    .productUrl(buildProductUrl((long) topProduct.getId()))
                    .reason("Đang thuộc nhóm bán chạy của tháng này")
                    .build());
        }

        return ChatbotRecommendResponse.builder()
                .reply(buildBestSellerReply(requestedTypes))
                .needsClarification(false)
                .filters(toFilters(heuristicIntent, requestedTypes))
                .products(recommendations)
                .build();
    }

    private List<TopProductResponse> resolveBestSellerProducts(List<String> requestedTypes, LocalDate startDate, LocalDate endDate) {
        if (requestedTypes.size() == 1) {
            String dashboardTypeName = toDashboardProductTypeName(requestedTypes.get(0));
            if (StringUtils.hasText(dashboardTypeName)) {
                return dashboardService.getTopSellingProductsByType(startDate, endDate, dashboardTypeName);
            }
        }
        return dashboardService.getTopSellingProducts(startDate, endDate);
    }

    private GeminiIntentResponse resolveIntent(String rawMessage, String normalizedMessage) {
        GeminiIntentResponse heuristicIntent = buildHeuristicIntent(normalizedMessage);

        try {
            GeminiIntentResponse geminiIntent = geminiIntentService.parseCustomerIntent(rawMessage);
            return mergeIntent(geminiIntent, heuristicIntent, normalizedMessage);
        } catch (Exception exception) {
            log.warn("Falling back to heuristic chatbot parsing: {}", exception.getMessage());
            return heuristicIntent;
        }
    }

    private GeminiIntentResponse buildHeuristicIntent(String normalizedMessage) {
        PriceRange priceRange = extractPriceRange(normalizedMessage);
        Double heuristicMinPrice = priceRange.minPrice();
        Double heuristicMaxPrice = priceRange.maxPrice();

        // Student-oriented advice should automatically favor affordable products
        // unless the user has already provided a concrete budget.
        if (heuristicMinPrice == null && heuristicMaxPrice == null && isStudentQuestion(normalizedMessage)) {
            heuristicMaxPrice = STUDENT_DEFAULT_MAX_PRICE;
        }

        return GeminiIntentResponse.builder()
                .productType(resolvePrimaryProductType(normalizedMessage))
                .brand(detectBrand(normalizedMessage))
                .minPrice(heuristicMinPrice)
                .maxPrice(heuristicMaxPrice)
                .keywords(buildHeuristicKeywords(normalizedMessage))
                .premium(containsPremiumKeyword(normalizedMessage))
                .needsClarification(false)
                .clarificationQuestion(null)
                .build();
    }

    private GeminiIntentResponse mergeIntent(
            GeminiIntentResponse geminiIntent,
            GeminiIntentResponse heuristicIntent,
            String normalizedMessage
    ) {
        if (geminiIntent == null) {
            return heuristicIntent;
        }

        List<String> mergedKeywords = mergeKeywordLists(geminiIntent.getKeywords(), heuristicIntent.getKeywords());
        String mergedProductType = normalizeProductType(geminiIntent.getProductType());
        if (PRODUCT_TYPE_UNKNOWN.equals(mergedProductType)) {
            mergedProductType = heuristicIntent.getProductType();
        }

        Double mergedMinPrice = geminiIntent.getMinPrice() != null ? geminiIntent.getMinPrice() : heuristicIntent.getMinPrice();
        Double mergedMaxPrice = geminiIntent.getMaxPrice() != null ? geminiIntent.getMaxPrice() : heuristicIntent.getMaxPrice();
        String mergedBrand = StringUtils.hasText(geminiIntent.getBrand()) ? geminiIntent.getBrand() : heuristicIntent.getBrand();

        // If the user is asking student-age advice without naming a budget,
        // keep the local affordability rule instead of trusting accidental
        // Gemini parsing from phrases like "20 tuoi".
        if (isStudentQuestion(normalizedMessage) && !containsExplicitBudgetHint(normalizedMessage)) {
            mergedMinPrice = heuristicIntent.getMinPrice();
            mergedMaxPrice = heuristicIntent.getMaxPrice();
        }

        // For explicit budget queries, the local regex parser is more reliable
        // than free-form model interpretation, so keep the extracted range.
        if (containsExplicitBudgetHint(normalizedMessage)
                && (heuristicIntent.getMinPrice() != null || heuristicIntent.getMaxPrice() != null)) {
            mergedMinPrice = heuristicIntent.getMinPrice();
            mergedMaxPrice = heuristicIntent.getMaxPrice();
        }

        mergedBrand = canonicalizeBrandName(mergedBrand);

        return GeminiIntentResponse.builder()
                .productType(mergedProductType)
                .brand(mergedBrand)
                .minPrice(mergedMinPrice)
                .maxPrice(mergedMaxPrice)
                .keywords(mergedKeywords)
                .premium(Boolean.TRUE.equals(geminiIntent.getPremium()) || Boolean.TRUE.equals(heuristicIntent.getPremium()))
                .needsClarification(false)
                .clarificationQuestion(shouldAskClarification(normalizedMessage) ? geminiIntent.getClarificationQuestion() : null)
                .build();
    }

    private boolean shouldAskClarification(String normalizedMessage) {
        return false;
    }

    private List<String> mergeHeuristicKeywords(List<String> existingKeywords, String normalizedMessage) {
        return mergeKeywordLists(existingKeywords, buildHeuristicKeywords(normalizedMessage));
    }

    private List<String> mergeKeywordLists(List<String> leftKeywords, List<String> rightKeywords) {
        List<String> merged = new ArrayList<>();
        if (leftKeywords != null) {
            merged.addAll(leftKeywords);
        }
        if (rightKeywords != null) {
            merged.addAll(rightKeywords);
        }

        return merged.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeText)
                .distinct()
                .toList();
    }

    private List<ChatbotProductItemResponse> findRecommendations(
            GeminiIntentResponse intent,
            List<String> requestedTypes,
            String normalizedMessage
    ) {
        List<ProductResponse> firstPass = productService.searchProducts(
                null,
                intent.getMinPrice(),
                intent.getMaxPrice(),
                intent.getBrand()
        );

        List<ChatbotProductItemResponse> primaryRecommendations = selectProducts(intent, requestedTypes, normalizedMessage, firstPass);
        if (!primaryRecommendations.isEmpty()) {
            return primaryRecommendations;
        }

        // The fallback search uses compact keywords for cases where users ask
        // about style, lifestyle, or demographic fit rather than naming a product.
        String fallbackKeywordQuery = buildFallbackKeywordQuery(intent);
        if (!StringUtils.hasText(fallbackKeywordQuery)) {
            return primaryRecommendations;
        }

        List<ProductResponse> secondPass = productService.searchProducts(
                fallbackKeywordQuery,
                intent.getMinPrice(),
                intent.getMaxPrice(),
                intent.getBrand()
        );

        return selectProducts(intent, requestedTypes, normalizedMessage, secondPass);
    }

    private List<ChatbotProductItemResponse> selectProducts(
            GeminiIntentResponse intent,
            List<String> requestedTypes,
            String normalizedMessage,
            List<ProductResponse> products
    ) {
        List<ProductResponse> rankedProducts = products.stream()
                .filter(product -> matchesRequestedTypes(requestedTypes, product))
                .filter(product -> matchesIntentConstraints(intent, product))
                .sorted(Comparator
                        .comparingDouble((ProductResponse product) -> scoreProduct(intent, product, requestedTypes, normalizedMessage))
                        .reversed()
                        .thenComparing(ProductResponse::getPrice, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        if (rankedProducts.isEmpty()) {
            return List.of();
        }

        if (isFrameLensComboRequest(requestedTypes, intent)) {
            List<ProductResponse> comboProducts = selectBestFrameLensCombo(intent, rankedProducts, normalizedMessage);
            if (!comboProducts.isEmpty()) {
                return comboProducts.stream()
                        .map(product -> toChatbotProductItem(intent, product))
                        .toList();
            }
            return List.of();
        }

        // When users ask for multiple categories, ensure the result includes at
        // least one product from each requested type before filling remaining slots.
        List<ProductResponse> selectedProducts = new ArrayList<>();
        Set<Long> selectedIds = new HashSet<>();

        if (requestedTypes.size() > 1) {
            for (String requestedType : requestedTypes) {
                rankedProducts.stream()
                        .filter(product -> requestedType.equals(normalizeProductType(product.getProduct_Type())))
                        .filter(product -> selectedIds.add(product.getId()))
                        .findFirst()
                        .ifPresent(selectedProducts::add);

                if (selectedProducts.size() >= 3) {
                    break;
                }
            }
        }

        for (ProductResponse product : rankedProducts) {
            if (selectedProducts.size() >= 3) {
                break;
            }
            if (selectedIds.add(product.getId())) {
                selectedProducts.add(product);
            }
        }

        return selectedProducts.stream()
                .map(product -> toChatbotProductItem(intent, product))
                .toList();
    }

    private List<ProductResponse> selectBestFrameLensCombo(
            GeminiIntentResponse intent,
            List<ProductResponse> rankedProducts,
            String normalizedMessage
    ) {
        if (intent.getMaxPrice() == null) {
            return List.of();
        }

        List<ProductResponse> frames = rankedProducts.stream()
                .filter(product -> PRODUCT_TYPE_FRAME.equals(normalizeProductType(product.getProduct_Type())))
                .limit(12)
                .toList();

        List<ProductResponse> lenses = rankedProducts.stream()
                .filter(product -> PRODUCT_TYPE_LENS.equals(normalizeProductType(product.getProduct_Type())))
                .limit(12)
                .toList();

        ProductResponse bestFrame = null;
        ProductResponse bestLens = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double bestTotalPrice = Double.MAX_VALUE;

        for (ProductResponse frame : frames) {
            for (ProductResponse lens : lenses) {
                double totalPrice = safePrice(frame) + safePrice(lens);
                if (totalPrice > intent.getMaxPrice()) {
                    continue;
                }

                double pairScore = scoreProduct(intent, frame, List.of(PRODUCT_TYPE_FRAME), normalizedMessage)
                        + scoreProduct(intent, lens, List.of(PRODUCT_TYPE_LENS), normalizedMessage);

                if (pairScore > bestScore || (Double.compare(pairScore, bestScore) == 0 && totalPrice < bestTotalPrice)) {
                    bestScore = pairScore;
                    bestTotalPrice = totalPrice;
                    bestFrame = frame;
                    bestLens = lens;
                }
            }
        }

        if (bestFrame == null || bestLens == null) {
            return List.of();
        }

        return List.of(bestFrame, bestLens);
    }

    private boolean matchesRequestedTypes(List<String> requestedTypes, ProductResponse product) {
        if (requestedTypes == null || requestedTypes.isEmpty()) {
            return true;
        }
        return requestedTypes.contains(normalizeProductType(product.getProduct_Type()));
    }

    private double scoreProduct(
            GeminiIntentResponse intent,
            ProductResponse product,
            List<String> requestedTypes,
            String normalizedMessage
    ) {
        double score = 0;
        String productType = normalizeProductType(product.getProduct_Type());
        int requestedTypeIndex = requestedTypes.indexOf(productType);
        if (requestedTypeIndex >= 0) {
            score += 45 - (requestedTypeIndex * 10L);
        }

        if (StringUtils.hasText(intent.getBrand()) && isSameBrand(intent.getBrand(), product.getBrand())) {
            score += 35;
        }

        if (product.getAvailableQuantity() != null && product.getAvailableQuantity() > 0) {
            score += 18;
        } else if (Boolean.TRUE.equals(product.getAllowPreorder())) {
            score += 8;
        } else {
            score -= 20;
        }

        score += scoreKeywordMatches(intent, product) * 11.0;

        if (Boolean.TRUE.equals(intent.getPremium()) && product.getPrice() != null) {
            score += Math.min(product.getPrice() / 250_000.0, 20);
        }

        if (isStyleAdviceQuestion(normalizedMessage) && PRODUCT_TYPE_FRAME.equals(productType)) {
            score += 18;
        }

        return score;
    }

    private int scoreKeywordMatches(GeminiIntentResponse intent, ProductResponse product) {
        String haystack = normalizeText(
                String.join(" ",
                        safe(product.getName()),
                        safe(product.getDescription()),
                        safe(product.getBrand()),
                        safe(product.getFrameShapeName()),
                        safe(product.getFrameMaterialName()),
                        safe(product.getColor()),
                        safe(product.getTypeName()),
                        safe(product.getUsageType()),
                        safe(product.getLensMaterial())
                )
        );

        int matches = 0;
        for (String keyword : intent.getKeywords()) {
            String normalizedKeyword = normalizeText(keyword);
            if (StringUtils.hasText(normalizedKeyword) && haystack.contains(normalizedKeyword)) {
                matches++;
            }
        }
        return matches;
    }

    private ChatbotProductItemResponse toChatbotProductItem(GeminiIntentResponse intent, ProductResponse product) {
        return ChatbotProductItemResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .brand(product.getBrand())
                .imageUrl(product.getImage_URL())
                .productUrl(buildProductUrl(product.getId()))
                .productType(product.getProduct_Type())
                .availableQuantity(product.getAvailableQuantity())
                .reason(buildRecommendationReason(intent, product))
                .build();
    }

    private ChatbotProductItemResponse toChatbotProductItem(ProductResponse product, String reason) {
        return ChatbotProductItemResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .brand(product.getBrand())
                .imageUrl(product.getImage_URL())
                .productUrl(buildProductUrl(product.getId()))
                .productType(product.getProduct_Type())
                .availableQuantity(product.getAvailableQuantity())
                .reason(reason)
                .build();
    }

    private ChatbotFiltersResponse toFilters(GeminiIntentResponse intent, List<String> requestedTypes) {
        return ChatbotFiltersResponse.builder()
                .productType(String.join(", ", requestedTypes))
                .brand(intent.getBrand())
                .minPrice(intent.getMinPrice())
                .maxPrice(intent.getMaxPrice())
                .keywords(intent.getKeywords())
                .premium(intent.getPremium())
                .build();
    }

    private String buildReply(
            String normalizedMessage,
            GeminiIntentResponse intent,
            List<ChatbotProductItemResponse> recommendations,
            List<String> requestedTypes
    ) {
        double comboTotalPrice = recommendations.stream()
                .limit(2)
                .mapToDouble(product -> product.getPrice() != null ? product.getPrice() : 0D)
                .sum();

        if (isFrameLensComboRequest(requestedTypes, intent)
                && recommendations.size() >= 2
                && intent.getMaxPrice() != null
                && comboTotalPrice <= intent.getMaxPrice()) {
            return "Mình đã chọn cho bạn 1 gọng kính và 1 tròng kính có tổng giá khoảng "
                    + formatCurrency(comboTotalPrice)
                    + ", vẫn nằm dưới ngân sách "
                    + formatCurrency(intent.getMaxPrice())
                    + ".";
        }

        String adviceIntro = buildAdviceIntro(normalizedMessage);
        if (StringUtils.hasText(adviceIntro)) {
            if (recommendations.isEmpty()) {
                return adviceIntro + " Hiện mình chưa tìm được sản phẩm thật phù hợp ngay trong kho, nhưng bạn có thể thử nới thêm ngân sách hoặc thương hiệu.";
            }
            return adviceIntro + " Mình gợi ý vài sản phẩm phù hợp ở bên dưới để bạn tham khảo.";
        }

        if (recommendations.isEmpty()) {
            return buildNoProductReply(intent, requestedTypes);
        }

        if (requestedTypes.size() >= 3 && containsGenericEyewearWord(normalizedMessage)) {
            return "Bạn đang tìm sản phẩm mắt kính theo nhu cầu chung nên mình ưu tiên gọng kính trước, sau đó tới tròng kính và kính áp tròng để bạn dễ so sánh.";
        }

        if (requestedTypes.contains(PRODUCT_TYPE_FRAME) && requestedTypes.contains(PRODUCT_TYPE_LENS)) {
            return "Mình đã ưu tiên gợi ý cả gọng kính và tròng kính để bạn dễ phối trong cùng ngân sách.";
        }

        return buildSuccessReply(intent, recommendations.size());
    }

    private String buildAdviceIntro(String normalizedMessage) {
        if (isSquareFaceQuestion(normalizedMessage)) {
            return "Với khuôn mặt vuông, bạn nên ưu tiên gọng tròn, oval hoặc dáng mắt mèo bo góc nhẹ để tổng thể gương mặt trông mềm hơn.";
        }
        if (isFemaleQuestion(normalizedMessage)) {
            return "Nếu bạn là nữ, các dáng gọng mắt mèo, oval, tròn mảnh hoặc kim loại thanh thoát thường rất dễ đeo và dễ phối trang phục.";
        }
        if (isStudentQuestion(normalizedMessage)) {
            return "Nếu bạn là sinh viên khoảng 20 tuổi, mình ưu tiên các mẫu dễ đeo hằng ngày, bền, nhẹ và có mức giá dưới 1 triệu 500 ngàn để hợp túi tiền hơn.";
        }
        return null;
    }

    private String buildSuccessReply(GeminiIntentResponse intent, int recommendationCount) {
        StringBuilder reply = new StringBuilder("Mình tìm thấy ")
                .append(recommendationCount)
                .append(" sản phẩm phù hợp");

        if (StringUtils.hasText(intent.getBrand())) {
            reply.append(" thuộc thương hiệu ").append(intent.getBrand());
        }
        if (intent.getMinPrice() != null || intent.getMaxPrice() != null) {
            reply.append(" theo tầm giá bạn đang quan tâm");
        }
        reply.append(". Bạn có thể xem chi tiết từng sản phẩm ở bên dưới.");
        return reply.toString();
    }

    private String buildNoProductReply(GeminiIntentResponse intent, List<String> requestedTypes) {
        StringBuilder reply = new StringBuilder("Mình chưa tìm thấy sản phẩm phù hợp");
        if (StringUtils.hasText(intent.getBrand())) {
            reply.append(" cho thương hiệu ").append(intent.getBrand());
        }
        if (intent.getMinPrice() != null || intent.getMaxPrice() != null) {
            reply.append(" trong tầm giá bạn đã chọn");
        }
        if (!requestedTypes.isEmpty()) {
            reply.append(". Bạn có thể thử nới thêm ngân sách hoặc đổi sang nhóm sản phẩm gần tương tự");
        }
        reply.append(".");
        return reply.toString();
    }

    private String buildFallbackKeywordQuery(GeminiIntentResponse intent) {
        if (intent.getKeywords() == null || intent.getKeywords().isEmpty()) {
            return null;
        }
        return intent.getKeywords().stream()
                .filter(StringUtils::hasText)
                .limit(3)
                .reduce((first, second) -> first + " " + second)
                .orElse(null);
    }

    private String buildProductUrl(Long productId) {
        String baseUrl = frontendProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return "/product/" + productId;
        }
        return (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl) + "/product/" + productId;
    }

    private String buildRecommendationReason(GeminiIntentResponse intent, ProductResponse product) {
        StringBuilder reason = new StringBuilder("Phù hợp");
        if (StringUtils.hasText(intent.getBrand()) && isSameBrand(intent.getBrand(), product.getBrand())) {
            reason.append(" thương hiệu ").append(product.getBrand());
        }
        if (intent.getMinPrice() != null || intent.getMaxPrice() != null) {
            reason.append(", nằm trong tầm giá bạn tìm");
        }
        if (Boolean.TRUE.equals(intent.getPremium()) && product.getPrice() != null) {
            reason.append(", thuộc nhóm giá cao");
        }
        return reason.toString();
    }

    private boolean matchesIntentConstraints(GeminiIntentResponse intent, ProductResponse product) {
        if (intent.getMinPrice() != null && (product.getPrice() == null || product.getPrice() < intent.getMinPrice())) {
            return false;
        }
        if (intent.getMaxPrice() != null && (product.getPrice() == null || product.getPrice() > intent.getMaxPrice())) {
            return false;
        }
        return !StringUtils.hasText(intent.getBrand()) || isSameBrand(intent.getBrand(), product.getBrand());
    }

    private boolean isPolicyQuestion(String normalizedMessage) {
        return normalizedMessage.contains("bao hanh")
                || normalizedMessage.contains("doi tra")
                || normalizedMessage.contains("chinh sach")
                || normalizedMessage.contains("hoan tien")
                || normalizedMessage.contains("hotline")
                || normalizedMessage.contains("email")
                || normalizedMessage.contains("giao sai");
    }

    private String buildPolicyReply(String normalizedMessage) {
        if (normalizedMessage.contains("hotline") || normalizedMessage.contains("email") || normalizedMessage.contains("lien he")) {
            return "Theo trang Bảo hành - Đổi trả hiện tại, shop hỗ trợ qua hotline 090 123 4567, email support@kinhmat.com, thời gian 8:00 - 22:00 hằng ngày.";
        }

        if (normalizedMessage.contains("gay gong")
                || normalizedMessage.contains("roi oc")
                || normalizedMessage.contains("tray xuoc")
                || normalizedMessage.contains("tray xuoc trong")) {
            return "Theo trang Bảo hành - Đổi trả hiện tại, shop bảo hành chủ yếu cho lỗi kỹ thuật do nhà sản xuất và lỗi gia công tròng đối với đơn prescription. Các trường hợp gãy gọng, rơi ốc, trầy xước tròng cần shop kiểm tra nguyên nhân trước; nếu xác định là lỗi kỹ thuật hoặc giao sai hàng thì bạn có thể gửi yêu cầu qua Hồ sơ cá nhân > Đơn hàng của tôi.";
        }

        if (normalizedMessage.contains("doi tra") || normalizedMessage.contains("hoan tien") || normalizedMessage.contains("giao sai")) {
            return "Theo trang Bảo hành - Đổi trả hiện tại, shop hỗ trợ đổi/trả khi sản phẩm lỗi kỹ thuật do nhà sản xuất hoặc giao sai mẫu so với đơn hàng. Quy trình là đăng nhập, vào Hồ sơ cá nhân > Đơn hàng của tôi, chọn đơn phù hợp rồi gửi yêu cầu kèm lý do và hình ảnh minh chứng.";
        }

        return "Theo trang Bảo hành - Đổi trả hiện tại, chính sách bảo hành áp dụng cho gọng kính và tròng kính mua trên hệ thống. Shop bảo hành cho lỗi kỹ thuật do nhà sản xuất và lỗi gia công tròng; đổi trả khi giao sai hàng hoặc sản phẩm có lỗi kỹ thuật.";
    }

    private boolean isSpecificationQuestion(String normalizedMessage) {
        return normalizedMessage.contains("kich thuoc")
                || normalizedMessage.contains("thong so")
                || normalizedMessage.contains("chi tiet")
                || normalizedMessage.contains("so do")
                || normalizedMessage.contains("chieu ngang")
                || normalizedMessage.contains("do dai cang")
                || normalizedMessage.contains("cau mui")
                || normalizedMessage.contains("diameter")
                || normalizedMessage.contains("base curve")
                || normalizedMessage.contains("water content")
                || normalizedMessage.contains("ham luong nuoc")
                || normalizedMessage.contains("chiet suat");
    }

    private ProductResponse findMentionedProduct(String normalizedMessage, List<String> requestedTypes) {
        List<ProductResponse> allProducts = productService.searchProducts(null, null, null, null);
        ProductResponse bestMatch = null;
        int bestScore = 0;

        for (ProductResponse product : allProducts) {
            if (requestedTypes.size() == 1 && !matchesRequestedTypes(requestedTypes, product)) {
                continue;
            }

            int matchScore = scoreMentionMatch(normalizedMessage, product);
            if (matchScore > bestScore) {
                bestScore = matchScore;
                bestMatch = product;
            }
        }

        return bestScore >= 24 ? bestMatch : null;
    }

    private int scoreMentionMatch(String normalizedMessage, ProductResponse product) {
        String normalizedProductName = normalizeText(product.getName());
        String compactMessage = compactNormalizedText(normalizedMessage);
        String compactProductName = compactNormalizedText(product.getName());
        int score = 0;

        if (normalizedMessage.contains(normalizedProductName)) {
            score += 200 + normalizedProductName.length();
        }
        if (compactMessage.contains(compactProductName)) {
            score += 220 + compactProductName.length();
        }

        for (String token : normalizedProductName.split(" ")) {
            if (isMeaningfulProductToken(token) && normalizedMessage.contains(token)) {
                score += 8;
            }
        }

        if (StringUtils.hasText(product.getBrand()) && normalizedMessage.contains(normalizeText(product.getBrand()))) {
            score += 15;
        }
        if (mentionsFrame(normalizedMessage) && PRODUCT_TYPE_FRAME.equals(normalizeProductType(product.getProduct_Type()))) {
            score += 10;
        }
        if (mentionsLens(normalizedMessage) && PRODUCT_TYPE_LENS.equals(normalizeProductType(product.getProduct_Type()))) {
            score += 10;
        }
        if (mentionsContactLens(normalizedMessage) && PRODUCT_TYPE_CONTACT_LENS.equals(normalizeProductType(product.getProduct_Type()))) {
            score += 10;
        }
        return score;
    }

    private boolean isMeaningfulProductToken(String token) {
        return StringUtils.hasText(token)
                && token.length() >= 3
                && !Set.of("gong", "kinh", "trong", "ap", "mat", "nam", "nu").contains(token);
    }

    private String buildSpecificationReply(ProductDetailResponse detail) {
        if (detail instanceof FrameResponse frameResponse) {
            return buildFrameSpecificationReply(frameResponse);
        }
        if (detail instanceof LensResponse lensResponse) {
            return buildLensSpecificationReply(lensResponse);
        }
        if (detail instanceof ContactLensResponse contactLensResponse) {
            return buildContactLensSpecificationReply(contactLensResponse);
        }
        return "Mình đã tìm thấy sản phẩm bạn hỏi, nhưng hiện chưa tổng hợp được thông số chi tiết để trả lời gọn cho bạn.";
    }

    private String buildFrameSpecificationReply(FrameResponse frameResponse) {
        List<String> specs = new ArrayList<>();
        if (isPositive(frameResponse.getLensWidth())) {
            specs.add("chiều ngang tròng " + formatMeasure(frameResponse.getLensWidth(), "mm"));
        }
        if (isPositive(frameResponse.getBridgeWidth())) {
            specs.add("cầu mũi " + formatMeasure(frameResponse.getBridgeWidth(), "mm"));
        }
        if (isPositive(frameResponse.getTempleLength())) {
            specs.add("độ dài càng kính " + formatMeasure(frameResponse.getTempleLength(), "mm"));
        }
        if (StringUtils.hasText(frameResponse.getFrameShape())) {
            specs.add("dáng " + frameResponse.getFrameShape());
        }
        if (StringUtils.hasText(frameResponse.getMaterial())) {
            specs.add("chất liệu " + frameResponse.getMaterial());
        }
        if (StringUtils.hasText(frameResponse.getColor())) {
            specs.add("màu " + frameResponse.getColor());
        }

        if (specs.isEmpty()) {
            return "Mình đã tìm thấy " + frameResponse.getName() + ", nhưng hiện dữ liệu chưa có đủ 3 số đo chi tiết để trả lời chính xác cho bạn.";
        }

        return frameResponse.getName() + " có " + String.join(", ", specs) + ".";
    }

    private String buildLensSpecificationReply(LensResponse lensResponse) {
        List<String> specs = new ArrayList<>();
        if (lensResponse.getIndexValue() != null) {
            specs.add("chiết suất " + lensResponse.getIndexValue().stripTrailingZeros().toPlainString());
        }
        if (isPositive(lensResponse.getDiameter())) {
            specs.add("đường kính " + formatMeasure(lensResponse.getDiameter(), "mm"));
        }
        if (StringUtils.hasText(lensResponse.getAvailablePowerRange())) {
            specs.add("dải độ " + lensResponse.getAvailablePowerRange());
        }
        if (StringUtils.hasText(lensResponse.getLensTypeName())) {
            specs.add("loại tròng " + lensResponse.getLensTypeName());
        }
        if (lensResponse.getIsBlueLightBlock() != null) {
            specs.add(Boolean.TRUE.equals(lensResponse.getIsBlueLightBlock()) ? "có lọc ánh sáng xanh" : "không lọc ánh sáng xanh");
        }
        if (lensResponse.getIsPhotochromic() != null) {
            specs.add(Boolean.TRUE.equals(lensResponse.getIsPhotochromic()) ? "có đổi màu" : "không đổi màu");
        }

        return lensResponse.getName() + " có " + String.join(", ", specs) + ".";
    }

    private String buildContactLensSpecificationReply(ContactLensResponse contactLensResponse) {
        List<String> specs = new ArrayList<>();
        if (StringUtils.hasText(contactLensResponse.getUsageType())) {
            specs.add("nhóm sử dụng " + contactLensResponse.getUsageType());
        }
        if (isPositive(contactLensResponse.getBaseCurve())) {
            specs.add("base curve " + formatMeasure(contactLensResponse.getBaseCurve(), ""));
        }
        if (isPositive(contactLensResponse.getDiameter())) {
            specs.add("đường kính " + formatMeasure(contactLensResponse.getDiameter(), "mm"));
        }
        if (isPositive(contactLensResponse.getWaterContent())) {
            specs.add("hàm lượng nước " + formatMeasure(contactLensResponse.getWaterContent(), "%"));
        }
        if (StringUtils.hasText(contactLensResponse.getAvailablePowerRange())) {
            specs.add("dải độ " + contactLensResponse.getAvailablePowerRange());
        }
        if (contactLensResponse.getQuantityPerBox() != null && contactLensResponse.getQuantityPerBox() > 0) {
            specs.add(contactLensResponse.getQuantityPerBox() + " miếng/hộp");
        }
        if (StringUtils.hasText(contactLensResponse.getLensMaterial())) {
            specs.add("chất liệu " + contactLensResponse.getLensMaterial());
        }
        if (StringUtils.hasText(contactLensResponse.getReplacementSchedule())) {
            specs.add("chu kỳ thay " + contactLensResponse.getReplacementSchedule());
        }
        if (StringUtils.hasText(contactLensResponse.getColor())) {
            specs.add("màu " + contactLensResponse.getColor());
        }

        return contactLensResponse.getName() + " có " + String.join(", ", specs) + ".";
    }

    private boolean isBestSellerQuestion(String normalizedMessage) {
        return normalizedMessage.contains("best seller")
                || normalizedMessage.contains("ban chay")
                || normalizedMessage.contains("dang hot")
                || normalizedMessage.contains("mau hot")
                || normalizedMessage.contains("hot nhat");
    }

    private String buildBestSellerReply(List<String> requestedTypes) {
        if (requestedTypes.size() == 1) {
            return switch (requestedTypes.get(0)) {
                case PRODUCT_TYPE_FRAME -> "Mình lấy đúng danh sách gọng kính bán chạy của tháng này để gợi ý cho bạn.";
                case PRODUCT_TYPE_LENS -> "Mình lấy đúng danh sách tròng kính bán chạy của tháng này để gợi ý cho bạn.";
                case PRODUCT_TYPE_CONTACT_LENS -> "Mình lấy đúng danh sách kính áp tròng bán chạy của tháng này để gợi ý cho bạn.";
                default -> "Mình lấy danh sách sản phẩm bán chạy của tháng này để gợi ý cho bạn.";
            };
        }
        return "Mình lấy danh sách sản phẩm best seller của tháng này để bạn tham khảo nhanh.";
    }

    private String toDashboardProductTypeName(String requestedType) {
        return switch (requestedType) {
            case PRODUCT_TYPE_FRAME -> "Gọng kính";
            case PRODUCT_TYPE_LENS -> "Tròng kính";
            case PRODUCT_TYPE_CONTACT_LENS -> "Kính áp tròng";
            default -> null;
        };
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private String formatMeasure(BigDecimal value, String unit) {
        if (value == null) {
            return "không xác định";
        }

        String numericValue = value.stripTrailingZeros().toPlainString();
        if (!StringUtils.hasText(unit)) {
            return numericValue;
        }
        return numericValue + " " + unit;
    }

    private boolean isSameBrand(String leftBrand, String rightBrand) {
        if (!StringUtils.hasText(leftBrand) || !StringUtils.hasText(rightBrand)) {
            return false;
        }
        return compactNormalizedText(leftBrand).equals(compactNormalizedText(rightBrand));
    }

    private String canonicalizeBrandName(String rawBrand) {
        if (!StringUtils.hasText(rawBrand)) {
            return rawBrand;
        }

        String compactBrand = compactNormalizedText(rawBrand);
        for (BrandResponse brand : productService.getAllBrands()) {
            if (compactBrand.equals(compactNormalizedText(brand.getBrandName()))) {
                return brand.getBrandName();
            }
        }
        return rawBrand;
    }

    private String compactNormalizedText(String value) {
        return normalizeText(value).replace(" ", "");
    }

    private List<String> resolveRequestedTypes(String normalizedMessage, GeminiIntentResponse intent) {
        if (isStyleAdviceQuestion(normalizedMessage)) {
            return List.of(PRODUCT_TYPE_FRAME);
        }

        List<String> requestedTypes = new ArrayList<>();
        if (mentionsFrame(normalizedMessage) || PRODUCT_TYPE_FRAME.equals(normalizeProductType(intent.getProductType()))) {
            requestedTypes.add(PRODUCT_TYPE_FRAME);
        }
        if (mentionsLens(normalizedMessage) || PRODUCT_TYPE_LENS.equals(normalizeProductType(intent.getProductType()))) {
            requestedTypes.add(PRODUCT_TYPE_LENS);
        }
        if (mentionsContactLens(normalizedMessage) || PRODUCT_TYPE_CONTACT_LENS.equals(normalizeProductType(intent.getProductType()))) {
            requestedTypes.add(PRODUCT_TYPE_CONTACT_LENS);
        }

        if (!requestedTypes.isEmpty()) {
            return requestedTypes.stream().distinct().toList();
        }

        // Generic "kinh" requests should show all categories, with frames first.
        return List.of(PRODUCT_TYPE_FRAME, PRODUCT_TYPE_LENS, PRODUCT_TYPE_CONTACT_LENS);
    }

    private String resolvePrimaryProductType(String normalizedMessage) {
        boolean frame = mentionsFrame(normalizedMessage);
        boolean lens = mentionsLens(normalizedMessage);
        boolean contactLens = mentionsContactLens(normalizedMessage);

        if (frame && !lens && !contactLens) {
            return PRODUCT_TYPE_FRAME;
        }
        if (lens && !frame && !contactLens) {
            return PRODUCT_TYPE_LENS;
        }
        if (contactLens && !frame && !lens) {
            return PRODUCT_TYPE_CONTACT_LENS;
        }
        return PRODUCT_TYPE_UNKNOWN;
    }

    private boolean mentionsFrame(String normalizedMessage) {
        return normalizedMessage.contains("gong kinh")
                || normalizedMessage.contains("gong")
                || normalizedMessage.contains("kinh mat")
                || normalizedMessage.contains("gang kinh");
    }

    private boolean mentionsLens(String normalizedMessage) {
        return normalizedMessage.contains("trong kinh")
                || normalizedMessage.contains("trong")
                || normalizedMessage.contains("can doi do")
                || normalizedMessage.contains("chong anh sang xanh");
    }

    private boolean mentionsContactLens(String normalizedMessage) {
        return normalizedMessage.contains("ap trong")
                || normalizedMessage.contains("contact lens")
                || normalizedMessage.contains("kinh ap trong");
    }

    private boolean containsGenericEyewearWord(String normalizedMessage) {
        return normalizedMessage.contains("kinh")
                || normalizedMessage.contains("mat kinh");
    }

    private boolean isStyleAdviceQuestion(String normalizedMessage) {
        return isSquareFaceQuestion(normalizedMessage)
                || isFemaleQuestion(normalizedMessage)
                || isStudentQuestion(normalizedMessage);
    }

    private boolean isSquareFaceQuestion(String normalizedMessage) {
        return normalizedMessage.contains("mat vuong")
                || normalizedMessage.contains("khuon mat vuong")
                || normalizedMessage.contains("dang vuong");
    }

    private boolean isFemaleQuestion(String normalizedMessage) {
        return normalizedMessage.contains("la nu")
                || normalizedMessage.contains("toi la nu")
                || normalizedMessage.contains("neu toi la nu")
                || normalizedMessage.contains("ban nu")
                || normalizedMessage.contains("phai nu");
    }

    private boolean isStudentQuestion(String normalizedMessage) {
        return normalizedMessage.contains("sinh vien")
                || normalizedMessage.contains("20 tuoi")
                || normalizedMessage.contains("tuoi 20");
    }

    private boolean containsExplicitBudgetHint(String normalizedMessage) {
        return normalizedMessage.contains("gia")
                || normalizedMessage.contains("ngan sach")
                || normalizedMessage.contains("trieu")
                || normalizedMessage.contains("nghin")
                || normalizedMessage.contains("ngan")
                || normalizedMessage.contains("vnd")
                || normalizedMessage.contains("dong")
                || normalizedMessage.contains("duoi")
                || normalizedMessage.contains("khong qua")
                || normalizedMessage.contains("toi da");
    }

    private boolean isFrameLensComboRequest(List<String> requestedTypes, GeminiIntentResponse intent) {
        if (requestedTypes.size() != 2) {
            return false;
        }
        return requestedTypes.contains(PRODUCT_TYPE_FRAME)
                && requestedTypes.contains(PRODUCT_TYPE_LENS)
                && intent.getMaxPrice() != null;
    }

    private boolean containsPremiumKeyword(String normalizedMessage) {
        return normalizedMessage.contains("cao cap")
                || normalizedMessage.contains("premium")
                || normalizedMessage.contains("high end");
    }

    private List<String> buildHeuristicKeywords(String normalizedMessage) {
        List<String> keywords = new ArrayList<>();

        if (isSquareFaceQuestion(normalizedMessage)) {
            keywords.add("tron");
            keywords.add("oval");
            keywords.add("mat meo");
        }
        if (isFemaleQuestion(normalizedMessage)) {
            keywords.add("mat meo");
            keywords.add("oval");
            keywords.add("kim loai");
        }
        if (isStudentQuestion(normalizedMessage)) {
            keywords.add("nhe");
            keywords.add("don gian");
            keywords.add("titan");
        }
        if (normalizedMessage.contains("kinh mat")) {
            keywords.add("ram");
        }

        return keywords.stream().map(this::normalizeText).distinct().toList();
    }

    private String detectBrand(String normalizedMessage) {
        String matchedBrand = null;
        int matchedLength = -1;
        String compactMessage = compactNormalizedText(normalizedMessage);

        for (BrandResponse brand : productService.getAllBrands()) {
            String normalizedBrand = normalizeText(brand.getBrandName());
            String compactBrand = compactNormalizedText(brand.getBrandName());
            if (StringUtils.hasText(normalizedBrand)
                    && (normalizedMessage.contains(normalizedBrand) || compactMessage.contains(compactBrand))
                    && compactBrand.length() > matchedLength) {
                matchedBrand = brand.getBrandName();
                matchedLength = compactBrand.length();
            }
        }

        return canonicalizeBrandName(matchedBrand);
    }

    private PriceRange extractPriceRange(String normalizedMessage) {
        Matcher rangeMatcher = PRICE_RANGE_PATTERN.matcher(normalizedMessage);
        if (rangeMatcher.find()) {
            if (isLikelyAgeMention(normalizedMessage, rangeMatcher.start(), rangeMatcher.end())
                    && rangeMatcher.group(2) == null
                    && rangeMatcher.group(4) == null) {
                return new PriceRange(null, null);
            }

            return new PriceRange(
                    parseMoney(rangeMatcher.group(1), rangeMatcher.group(2)),
                    parseMoney(rangeMatcher.group(3), rangeMatcher.group(4))
            );
        }

        Matcher maxMatcher = PRICE_MAX_PATTERN.matcher(normalizedMessage);
        if (maxMatcher.find()) {
            if (isLikelyAgeMention(normalizedMessage, maxMatcher.start(), maxMatcher.end())
                    && maxMatcher.group(2) == null) {
                return new PriceRange(null, null);
            }
            return new PriceRange(null, parseMoney(maxMatcher.group(1), maxMatcher.group(2)));
        }

        Matcher approxMatcher = PRICE_APPROX_PATTERN.matcher(normalizedMessage);
        if (approxMatcher.find()) {
            // Ignore phrases such as "khoang 20 tuoi" so age is not treated as money.
            if (isLikelyAgeMention(normalizedMessage, approxMatcher.start(), approxMatcher.end())
                    && approxMatcher.group(2) == null) {
                return new PriceRange(null, null);
            }
            double approxValue = parseMoney(approxMatcher.group(1), approxMatcher.group(2));
            return new PriceRange(approxValue * 0.75, approxValue);
        }

        return new PriceRange(null, null);
    }

    private boolean isLikelyAgeMention(String normalizedMessage, int matchStart, int matchEnd) {
        String trailingContext = normalizedMessage.substring(matchEnd).trim();
        return trailingContext.startsWith("tuoi")
                || trailingContext.startsWith("age")
                || trailingContext.startsWith("years old");
    }

    private double parseMoney(String amountText, String unitText) {
        double amount = Double.parseDouble(amountText.replace(",", "."));
        String unit = unitText == null ? "" : unitText.trim();

        return switch (unit) {
            case "trieu", "tr" -> amount * 1_000_000;
            case "nghin", "ngan", "k" -> amount * 1_000;
            default -> amount;
        };
    }

    private String normalizeProductType(String rawType) {
        String normalized = normalizeText(rawType);
        return switch (normalized) {
            case "frame", "gong kinh", "gong", "frames", "kinh mat", "mat kinh" -> PRODUCT_TYPE_FRAME;
            case "lens", "trong kinh" -> PRODUCT_TYPE_LENS;
            case "contact_lens", "contact lens", "kinh ap trong", "contactlens", "ap trong" -> PRODUCT_TYPE_CONTACT_LENS;
            default -> PRODUCT_TYPE_UNKNOWN;
        };
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ");
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private double safePrice(ProductResponse product) {
        return product.getPrice() != null ? product.getPrice() : Double.MAX_VALUE;
    }

    private String formatCurrency(Double value) {
        if (value == null) {
            return "không xác định";
        }
        return String.format(Locale.forLanguageTag("vi-VN"), "%,.0fđ", value);
    }

    private record PriceRange(Double minPrice, Double maxPrice) {
    }
}
