package com.swp391.eyewear_management_backend.integration.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swp391.eyewear_management_backend.config.gemini.GeminiProperties;
import com.swp391.eyewear_management_backend.dto.response.GeminiIntentResponse;
import com.swp391.eyewear_management_backend.exception.AppException;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class GeminiClient {

    private static final String PRODUCT_TYPE_FRAME = "FRAME";
    private static final String PRODUCT_TYPE_LENS = "LENS";
    private static final String PRODUCT_TYPE_CONTACT_LENS = "CONTACT_LENS";
    private static final String PRODUCT_TYPE_UNKNOWN = "UNKNOWN";

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiIntentResponse extractProductIntent(String userMessage) {
        validateConfiguration();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(geminiProperties.getTimeoutSeconds()))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildGenerateContentUri())
                .timeout(Duration.ofSeconds(geminiProperties.getTimeoutSeconds()))
                .header("x-goog-api-key", geminiProperties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(userMessage), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new AppException(ErrorCode.GEMINI_REQUEST_FAILED, "Failed to call Gemini API");
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AppException(ErrorCode.GEMINI_REQUEST_FAILED, "Gemini API returned status " + response.statusCode());
        }

        return parseIntentResponse(response.body());
    }

    private void validateConfiguration() {
        // This guard fails fast so the system never attempts an outbound AI call
        // with a missing key or disabled Gemini integration.
        if (!geminiProperties.isEnabled() || !StringUtils.hasText(geminiProperties.getApiKey())) {
            throw new AppException(ErrorCode.GEMINI_NOT_CONFIGURED);
        }
    }

    private URI buildGenerateContentUri() {
        String encodedModel = URLEncoder.encode(geminiProperties.getModel(), StandardCharsets.UTF_8);
        String baseUrl = geminiProperties.getBaseUrl().endsWith("/")
                ? geminiProperties.getBaseUrl().substring(0, geminiProperties.getBaseUrl().length() - 1)
                : geminiProperties.getBaseUrl();
        return URI.create(baseUrl + "/v1beta/models/" + encodedModel + ":generateContent");
    }

    private String buildRequestBody(String userMessage) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", buildIntentPrompt(userMessage));

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("temperature", 0.1);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseJsonSchema", buildIntentSchema());

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new AppException(ErrorCode.GEMINI_INVALID_RESPONSE, "Failed to build Gemini request body");
        }
    }

    private ObjectNode buildIntentSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        addStringProperty(properties, "productType");
        addNullableStringProperty(properties, "brand");
        addNullableNumberProperty(properties, "minPrice");
        addNullableNumberProperty(properties, "maxPrice");

        ObjectNode keywords = properties.putObject("keywords");
        keywords.put("type", "array");
        ObjectNode keywordItems = keywords.putObject("items");
        keywordItems.put("type", "string");

        ObjectNode premium = properties.putObject("premium");
        premium.put("type", "boolean");

        ObjectNode needsClarification = properties.putObject("needsClarification");
        needsClarification.put("type", "boolean");

        addNullableStringProperty(properties, "clarificationQuestion");

        ArrayNode required = schema.putArray("required");
        required.add("productType");
        required.add("keywords");
        required.add("premium");
        required.add("needsClarification");

        return schema;
    }

    private void addStringProperty(ObjectNode properties, String fieldName) {
        ObjectNode node = properties.putObject(fieldName);
        node.put("type", "string");
    }

    private void addNullableStringProperty(ObjectNode properties, String fieldName) {
        ObjectNode node = properties.putObject(fieldName);
        ArrayNode type = node.putArray("type");
        type.add("string");
        type.add("null");
    }

    private void addNullableNumberProperty(ObjectNode properties, String fieldName) {
        ObjectNode node = properties.putObject(fieldName);
        ArrayNode type = node.putArray("type");
        type.add("number");
        type.add("null");
    }

    private String buildIntentPrompt(String userMessage) {
        return """
                You are an intent parser for an eyewear e-commerce chatbot.
                Read the customer message and return JSON only.

                Rules:
                - productType must be one of FRAME, LENS, CONTACT_LENS, UNKNOWN
                - "gong kinh", "gong", "frame" => FRAME
                - "trong kinh", "trong", "lens" => LENS
                - "kinh ap trong", "contact lens" => CONTACT_LENS
                - Generic words like "kinh", "mat kinh", "eyewear" => productType UNKNOWN and needsClarification=false
                - Convert Vietnamese currency phrases into numeric VND values
                - "1 trieu" => 1000000
                - "1.5 trieu" => 1500000
                - "cao cap", "premium", "high-end" => premium=true
                - brand must be null when the message does not explicitly mention one
                - keywords should contain short useful search hints, not full sentences
                - For style questions, include useful keywords such as tron, oval, mat meo, titan, nhe when relevant
                - Do not ask for clarification if the message is still clearly about eyewear shopping or common eyewear advice
                - Only set needsClarification=true when the message is completely unrelated to eyewear
                - If needsClarification=true, provide a short Vietnamese clarificationQuestion
                - Do not invent products, links, or unavailable data

                Customer message:
                """ + userMessage;
    }

    private GeminiIntentResponse parseIntentResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (!StringUtils.hasText(textNode.asText())) {
                throw new AppException(ErrorCode.GEMINI_INVALID_RESPONSE, "Gemini response did not include JSON text");
            }

            GeminiIntentResponse intent = objectMapper.readValue(textNode.asText(), GeminiIntentResponse.class);
            return normalizeIntent(intent);
        } catch (IOException exception) {
            throw new AppException(ErrorCode.GEMINI_INVALID_RESPONSE, "Failed to parse Gemini intent response");
        }
    }

    private GeminiIntentResponse normalizeIntent(GeminiIntentResponse intent) {
        // This normalization step keeps the downstream search layer simple by
        // forcing nullable strings, known product types, and safe boolean defaults.
        if (intent == null) {
            throw new AppException(ErrorCode.GEMINI_INVALID_RESPONSE, "Gemini intent payload is empty");
        }

        intent.setProductType(normalizeProductType(intent.getProductType()));
        intent.setBrand(normalizeNullableText(intent.getBrand()));
        intent.setClarificationQuestion(normalizeNullableText(intent.getClarificationQuestion()));
        intent.setPremium(Boolean.TRUE.equals(intent.getPremium()));
        intent.setNeedsClarification(Boolean.TRUE.equals(intent.getNeedsClarification()));

        if (intent.getKeywords() == null) {
            intent.setKeywords(new java.util.ArrayList<>());
        } else {
            intent.setKeywords(intent.getKeywords().stream()
                    .map(this::normalizeNullableText)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList());
        }

        if (intent.getMinPrice() != null && intent.getMinPrice() < 0) {
            intent.setMinPrice(null);
        }
        if (intent.getMaxPrice() != null && intent.getMaxPrice() < 0) {
            intent.setMaxPrice(null);
        }
        if (intent.getMinPrice() != null && intent.getMaxPrice() != null && intent.getMinPrice() > intent.getMaxPrice()) {
            double originalMin = intent.getMinPrice();
            intent.setMinPrice(intent.getMaxPrice());
            intent.setMaxPrice(originalMin);
        }

        return intent;
    }

    private String normalizeProductType(String productType) {
        if (!StringUtils.hasText(productType)) {
            return PRODUCT_TYPE_UNKNOWN;
        }

        String normalized = productType.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case PRODUCT_TYPE_FRAME, "GONG_KINH", "FRAMES" -> PRODUCT_TYPE_FRAME;
            case PRODUCT_TYPE_LENS, "TRONG_KINH" -> PRODUCT_TYPE_LENS;
            case PRODUCT_TYPE_CONTACT_LENS, "KINH_AP_TRONG", "CONTACTLENS" -> PRODUCT_TYPE_CONTACT_LENS;
            default -> PRODUCT_TYPE_UNKNOWN;
        };
    }

    private String normalizeNullableText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
