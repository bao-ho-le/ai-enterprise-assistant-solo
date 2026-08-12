package com.enterprise.aiassistant.backend.ai.infrastructure.llm.service;

import com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto.LLMRequest;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.dto.LLMResponse;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.mapper.GeminiResponseMapper;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.InvalidRequestException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GeminiLLMService implements LLMService {

    private final ChatModel chatModel;
    private final GeminiResponseMapper geminiResponseMapper;
    private final LLMHelper llmHelper;

    @Override
    public LLMResponse generate(LLMRequest request) {
        llmHelper.validateGenerateRequest(request);

        try {
            ChatResponse response = chatModel.chat(UserMessage.from(request.getPrompt()));
            return geminiResponseMapper.mapToLLMResponse(response, getModelName());
        } catch (InvalidRequestException e) {
            if (isInputTooLarge(e)) {
                throw new LLMException(ErrorCode.LLM_INPUT_TOO_LARGE, e);
            }
            throw new LLMException(ErrorCode.LLM_GENERATION_FAILED, ErrorCode.LLM_GENERATION_FAILED.getMessage(), e);
        } catch (Exception e) {
            // Bắt mọi lỗi khác từ langchain4j (network, quota, timeout...), không phân loại chi tiết
            throw new LLMException(ErrorCode.LLM_GENERATION_FAILED, ErrorCode.LLM_GENERATION_FAILED.getMessage(), e);
        }
    }

    @Override
    public String getModelName() {
        return "gemini-3.1-flash-lite";
    }

    // Gemini không có mã lỗi riêng cho "vượt token" - mọi request không hợp lệ đều trả
    // HTTP 400 (InvalidRequestException), nên phải soi message mới phân biệt được.
    // "token count" khớp cả 2 dạng message thật của Gemini:
    // - "The input token count (N) exceeds the maximum number of tokens allowed (M)."
    // - "...the input token count is N but model only supports up to M..."
    private boolean isInputTooLarge(InvalidRequestException e) {
        String message = e.getMessage();

        if (message == null) {
            return false;
        }

        message = message.toLowerCase(Locale.ROOT);

        return message.contains("token count");
    }
}
