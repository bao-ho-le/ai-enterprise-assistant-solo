package com.enterprise.aiassistant.backend.ai.knowledge.generation.helper;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.request.TriggerGenerationRequest;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class GenerationHelper {

    // Đây là bộ chuyển đổi dữ liệu giữa Java Object và
    // FAIL_ON_UNKNOWN_PROPERTIES dùng để không báo lỗi khi Json có field mà Object không có
    private final ObjectMapper objectMapper =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public void validateTriggerRequest(TriggerGenerationRequest request) {
        if (request == null || request.getInputData() == null) {
            throw new AIConversationException(ErrorCode.GENERATION_INPUT_DATA_REQUIRED);
        }
    }

    public void validateGenerationId(Long generationId) {
        if (generationId == null) {
            throw new AIConversationException(ErrorCode.GENERATION_ID_REQUIRED);
        }

        if (generationId <= 0) {
            throw new AIConversationException(ErrorCode.GENERATION_ID_INVALID);
        }
    }

    // Cần dùng kiểu T vì nhận vào Json Node trả về
    public <T> T parseInput(JsonNode inputData, Class<T> type) {
        try {
            return objectMapper.treeToValue(inputData, type);
        } catch (JsonProcessingException ex) {
            throw new AIConversationException(ErrorCode.GENERATION_INPUT_DATA_INVALID);
        }
    }

    public void validateSourceDocumentsRequired(String documentContext) {
        if (documentContext == null || documentContext.isBlank()) {
            throw new AIConversationException(ErrorCode.GENERATION_SOURCE_DOCUMENTS_REQUIRED);
        }
    }

    public String truncateTitle(String raw, int maxLength) {
        String trimmed = raw.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}
