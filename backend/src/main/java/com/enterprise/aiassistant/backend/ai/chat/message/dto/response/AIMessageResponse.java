package com.enterprise.aiassistant.backend.ai.chat.message.dto.response;

import com.enterprise.aiassistant.backend.ai.chat.message.enums.AIMessageRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AIMessageResponse {

    private Long id;

    private AIMessageRole role;

    private String content;

    private LocalDateTime createdAt;
}
