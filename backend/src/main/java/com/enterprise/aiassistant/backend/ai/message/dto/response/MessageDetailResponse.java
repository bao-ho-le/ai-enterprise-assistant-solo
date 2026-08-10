package com.enterprise.aiassistant.backend.ai.message.dto.response;

import com.enterprise.aiassistant.backend.ai.message.enums.AIMessageRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MessageDetailResponse {

    private Long id;

    private AIMessageRole role;

    private String content;

    private LocalDateTime createdAt;

    private List<MessageSourceResponse> sources;
}
