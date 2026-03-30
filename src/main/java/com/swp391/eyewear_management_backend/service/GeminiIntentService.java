package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.response.GeminiIntentResponse;

public interface GeminiIntentService {

    /**
     * Parses a natural-language customer request into structured shopping filters
     * that backend services can safely map to real product queries.
     */
    GeminiIntentResponse parseCustomerIntent(String userMessage);
}
