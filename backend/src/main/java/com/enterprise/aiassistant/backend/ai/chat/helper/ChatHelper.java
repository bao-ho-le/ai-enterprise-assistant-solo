package com.enterprise.aiassistant.backend.ai.chat.helper;


import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.SummaryGenerationInput;

import org.springframework.stereotype.Component;

@Component
public class ChatHelper {

    private static final String DEFAULT_STYLE = "PARAGRAPH";
    private static final String DEFAULT_LENGTH = "Medium";
    private static final String DEFAULT_AUDIENCE = "General audience";
    private static final String DEFAULT_LANGUAGE = "the same language as the user's message";

    // Chat không có form Summary nên dùng default; câu hỏi của user đi vào instructions
    // để prompt hiện tại vẫn phản ánh được yêu cầu cụ thể mà không cần parse gì thêm.
    public SummaryGenerationInput toSummaryInput(String message) {

        SummaryGenerationInput input = new SummaryGenerationInput();
        input.setStyle(DEFAULT_STYLE);
        input.setLength(DEFAULT_LENGTH);
        input.setAudience(DEFAULT_AUDIENCE);
        input.setLanguage(DEFAULT_LANGUAGE);
        input.setInstructions(message);
        input.setIncludeActionItems(false);

        return input;
    }
}
