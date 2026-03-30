package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.dto.request.ChatbotRecommendRequest;
import com.swp391.eyewear_management_backend.dto.response.ApiResponse;
import com.swp391.eyewear_management_backend.dto.response.ChatbotRecommendResponse;
import com.swp391.eyewear_management_backend.service.ChatbotRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/chatbot", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotRecommendationService chatbotRecommendationService;

    @PostMapping("/recommend")
    public ApiResponse<ChatbotRecommendResponse> recommend(@RequestBody @Valid ChatbotRecommendRequest request) {
        // Keep the endpoint thin so request validation and business logic stay separated.
        return ApiResponse.<ChatbotRecommendResponse>builder()
                .message("OK")
                .result(chatbotRecommendationService.recommendProducts(request))
                .build();
    }
}
