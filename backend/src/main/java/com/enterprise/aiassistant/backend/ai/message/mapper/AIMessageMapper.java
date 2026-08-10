package com.enterprise.aiassistant.backend.ai.message.mapper;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.message.dto.response.*;
import com.enterprise.aiassistant.backend.ai.message.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.message.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.ai.message.enums.AIMessageRole;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class AIMessageMapper {

    public MessagePageResponse toMessagePageResponse(Slice<AIMessage> page) {

        List<AIMessageResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        List<AIMessageResponse> ascending = new ArrayList<>(content);
        Collections.reverse(ascending);

        return MessagePageResponse.builder()
                .content(ascending)
                .hasMore(page.hasNext())
                .build();
    }

    public AIMessageResponse toResponse(AIMessage message) {

        return AIMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public MessageSourceResponse toSourceResponse(
            AIMessageSource source,
            Map<Long, DocumentChunk> chunksById
    ) {

        DocumentChunk chunk = chunksById.get(source.getChunkId());

        return MessageSourceResponse.builder()
                .chunkId(source.getChunkId())
                .documentTitle(chunk == null ? null : chunk.getDocumentVersion().getDocument().getTitle())
                .pageNumber(chunk == null ? null : chunk.getPageNumber())
                .score(source.getScore())
                .content(chunk == null ? null : chunk.getContent())
                .build();
    }

    public List<MessageSourceResponse> toSourceResponses(
            List<AIMessageSource> sources,
            Map<Long, DocumentChunk> chunksById
    ) {

        return sources.stream()
                .map(source -> toSourceResponse(source, chunksById))
                .toList();
    }

    public MessageResponse toMessageResponse(
            AIMessage userMessage,
            AIMessage assistantMessage
    ) {

        return MessageResponse.builder()
                .userMessage(toResponse(userMessage))
                .assistantMessage(
                        assistantMessage == null
                                ? null
                                : toResponse(assistantMessage)
                )
                .build();
    }

    public AIMessage toMessage(
            AIConversation conversation,
            AIMessageRole role,
            String content
    ) {

        return AIMessage.builder()
                .conversation(conversation)
                .role(role)
                .content(content.trim())
                .build();
    }

    public MessageDetailResponse toDetailResponse(
            AIMessage message,
            List<AIMessageSource> sources,
            Map<Long, DocumentChunk> chunksById
    ) {

        return MessageDetailResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .sources(toSourceResponses(sources, chunksById))
                .build();
    }
}
