package com.enterprise.aiassistant.backend.ai.intent.service;

import com.enterprise.aiassistant.backend.ai.intent.enums.Intent;
import com.enterprise.aiassistant.backend.ai.intent.helper.IntentHelper;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.llm.service.LLMService;
import com.enterprise.aiassistant.backend.ai.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class IntentClassifierImpl implements IntentClassifier {

    // Phân loại intent chỉ là bước hỗ trợ routing: LLM lỗi hoặc trả giá trị lạ thì giữ
    // nguyên behavior cũ của conversation DOCUMENT_QA thay vì làm hỏng cả lượt chat.
    public static final Intent FALLBACK_INTENT = Intent.GENERAL_CHAT;

    private final LLMService llmService;
    private final PromptBuilderService promptBuilderService;
    private final AIMessageHelper messageHelper;
    private final IntentHelper intentHelper;

    @Override
    public Intent classify(String message) {

        messageHelper.validateContent(message);

        try {
            LLMResponse llmResponse = llmService.generate(
                    LLMRequest.builder()
                            .prompt(promptBuilderService.buildIntentClassificationPrompt(message))
                            .build()
            );

            return intentHelper.parseIntent(llmResponse.getContent());

        } catch (LLMException e) {
            return FALLBACK_INTENT;
        }
    }


}
