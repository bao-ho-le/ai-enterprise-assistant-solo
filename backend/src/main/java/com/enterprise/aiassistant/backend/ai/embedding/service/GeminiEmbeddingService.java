package com.enterprise.aiassistant.backend.ai.embedding.service;

import com.enterprise.aiassistant.backend.ai.embedding.config.GeminiProperties;
import com.enterprise.aiassistant.backend.ai.embedding.dto.EmbeddingResult;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.EmbeddingException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiEmbeddingService implements EmbeddingService {

    // Ước lượng token: ~4 ký tự/token (cùng quy ước với ChunkingMapper.CHARS_PER_TOKEN) —
    // dùng khi Gemini không trả token usage cho embedding call (xem estimateTokens()).
    private static final int CHARS_PER_TOKEN = 4;

    private final EmbeddingModel embeddingModel;
    private final GeminiProperties properties;

    @Override
    public EmbeddingResult embed(String text) {

        validateText(text);

        try {
            Response<Embedding> response = embeddingModel.embed(text);
            return toEmbeddingResult(text, response, properties);

        } catch (Exception ex) {
            log.error(ErrorCode.EMBEDDING_FAILED.getMessage(), ex);
            throw new EmbeddingException(ErrorCode.EMBEDDING_FAILED, ex);
        }
    }

    @Override
    public String getModelName() {
        return properties.getEmbeddingModel();
    }


    // Helper

    private void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new EmbeddingException(ErrorCode.EMBEDDING_TEXT_REQUIRED);
        }
    }

    // Mapper

    public EmbeddingResult toEmbeddingResult(String text, Response<Embedding> response, GeminiProperties properties) {
        Embedding embedding = response.content();

        // GoogleAiEmbeddingModel (langchain4j) never attaches a TokenUsage to the
        // Response for embedding calls — it always builds Response.from(embedding)
        // with no usage info. Fall back to a char-count estimate so usage logs
        // still get a non-zero input token count.
        Integer inputTokens = response.tokenUsage() != null
                ? response.tokenUsage().inputTokenCount()
                : estimateTokens(text);

        return EmbeddingResult.builder()
                .vector(embedding.vector())
                .dimension(embedding.dimension())
                .model(properties.getEmbeddingModel())
                .inputTokens(inputTokens)
                .build();
    }

    private int estimateTokens(String text) {
        return Math.max(1, text.length() / CHARS_PER_TOKEN);
    }

}
