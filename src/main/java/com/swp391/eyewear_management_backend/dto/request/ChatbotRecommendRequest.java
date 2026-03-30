package com.swp391.eyewear_management_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatbotRecommendRequest {

    @NotBlank(message = "CHATBOT_MESSAGE_REQUIRED")
    @Size(max = 1000, message = "CHATBOT_MESSAGE_TOO_LONG")
    String message;
}
