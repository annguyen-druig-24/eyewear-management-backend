package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.response.GeminiIntentResponse;
import com.swp391.eyewear_management_backend.integration.gemini.GeminiClient;
import com.swp391.eyewear_management_backend.service.GeminiIntentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiIntentServiceImpl implements GeminiIntentService {

    private final GeminiClient geminiClient;

    @Override
    public GeminiIntentResponse parseCustomerIntent(String userMessage) {
        // The service stays thin on purpose so prompt logic and HTTP concerns
        // remain isolated inside GeminiClient.
        return geminiClient.extractProductIntent(userMessage);
    }
}
