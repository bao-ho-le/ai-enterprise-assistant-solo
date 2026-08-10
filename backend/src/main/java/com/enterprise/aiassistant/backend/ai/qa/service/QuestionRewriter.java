package com.enterprise.aiassistant.backend.ai.qa.service;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;

public interface QuestionRewriter {

    // Dùng ConversationMemory để biến userMessage phụ thuộc ngữ cảnh (vd: "nói rõ về nó")
    // thành standalone question cho Document QA retrieval. Không trả lời câu hỏi; nếu rewrite
    // thất bại hoặc không cần thiết, trả về nguyên văn userMessage.
    String rewrite(
            AIConversation conversation,
            String userMessage
    );
}
