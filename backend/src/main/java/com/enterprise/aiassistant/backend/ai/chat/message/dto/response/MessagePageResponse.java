package com.enterprise.aiassistant.backend.ai.chat.message.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// Plain DTO instead of returning Slice<AIMessageResponse> directly: Slice.hasNext()
// does not serialize to JSON in this project's Jackson setup (Spring Boot's Jackson 3
// ObjectMapper has no Spring Data Commons Page/Slice module registered for it, since
// that module targets Jackson 2), so it would silently come back as a missing field
// and infinite scroll would think every page was the last one.
@Getter
@Builder
public class MessagePageResponse {

    private List<AIMessageResponse> content;

    private boolean hasMore;
}
