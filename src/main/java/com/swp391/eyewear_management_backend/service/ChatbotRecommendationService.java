package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.request.ChatbotRecommendRequest;
import com.swp391.eyewear_management_backend.dto.response.ChatbotRecommendResponse;

public interface ChatbotRecommendationService {

    /**
     * Converts a free-text shopping request into a frontend-ready chatbot response.
     * The later implementation will parse customer intent, search real products,
     * and return only data that exists inside the system.
     */
    ChatbotRecommendResponse recommendProducts(ChatbotRecommendRequest request);
}
