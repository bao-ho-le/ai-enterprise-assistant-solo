package com.enterprise.aiassistant.backend.ai.intent.service;

import com.enterprise.aiassistant.backend.ai.intent.enums.Intent;
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
    private static final Intent FALLBACK_INTENT = Intent.GENERAL_CHAT;

    private final LLMService llmService;
    private final PromptBuilderService promptBuilderService;
    private final AIMessageHelper messageHelper;

    @Override
    public Intent classify(String message) {

        messageHelper.validateContent(message);

        try {
            LLMResponse llmResponse = llmService.generate(
                    LLMRequest.builder()
                            .prompt(promptBuilderService.buildIntentClassificationPrompt(message))
                            .build()
            );

            return parseIntent(llmResponse.getContent());

        } catch (LLMException e) {
            return FALLBACK_INTENT;
        }
    }


    // Helper

    // Model đôi khi bọc giá trị trong dấu nháy hoặc thêm xuống dòng, nên chuẩn hoá
    // trước khi parse; chỉ chấp nhận đúng tên enum, không so khớp chuỗi con.
    private Intent parseIntent(String rawIntent) {

        if (rawIntent == null) {
            return FALLBACK_INTENT;
        }

        String normalized = rawIntent
                .replace("\"", "")
                .replace("'", "")
                .replace("`", "")
                .trim()
                .toUpperCase(Locale.ROOT);

        try {
            return Intent.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return FALLBACK_INTENT;
        }
    }
}
