package com.enterprise.aiassistant.backend.ai.memory.helper;

import org.springframework.stereotype.Component;

@Component
public class ConversationMemoryHelper {

    // Ngưỡng nén tính bằng ký tự của summarizedContext + pendingContext, không phải số
    // message: đạt hoặc vượt ngưỡng (>=) là nén ngay ở cuối lượt chat đó.
    private static final int CONTEXT_CHARACTER_LIMIT = 16_000;

    private static final String EMPTY_CONTEXT = "No previous conversation context.";

    // Memory phải giữ cả câu hỏi lẫn câu trả lời, nếu không các lượt sau không hiểu
    // assistant đã trả lời gì.
    public String appendTurn(
            String pendingContext,
            String userMessage,
            String assistantResponse
    ) {

        return nullToEmpty(pendingContext)
                + "USER:\n" + userMessage.trim()
                + "\n\nASSISTANT:\n" + assistantResponse.trim()
                + "\n\n";
    }

    public int calculateCharacterCount(
            String summarizedContext,
            String pendingContext
    ) {

        return nullToEmpty(summarizedContext).length() + nullToEmpty(pendingContext).length();
    }

    public boolean needsSummarization(int contextCharacterCount) {

        return contextCharacterCount >= CONTEXT_CHARACTER_LIMIT;
    }

    public String buildContext(
            String summarizedContext,
            String pendingContext
    ) {

        String summarized = nullToEmpty(summarizedContext).trim();
        String pending = nullToEmpty(pendingContext).trim();

        if (summarized.isEmpty() && pending.isEmpty()) {
            return EMPTY_CONTEXT;
        }

        StringBuilder context = new StringBuilder();

        if (!summarized.isEmpty()) {
            context.append("Summary of earlier turns:\n").append(summarized).append("\n\n");
        }

        if (!pending.isEmpty()) {
            context.append("Recent turns:\n").append(pending);
        }

        return context.toString().trim();
    }

    public String emptyContext() {

        return EMPTY_CONTEXT;
    }

    private String nullToEmpty(String value) {

        return value == null ? "" : value;
    }
}
