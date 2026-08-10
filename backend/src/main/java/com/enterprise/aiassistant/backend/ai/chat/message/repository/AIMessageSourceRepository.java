package com.enterprise.aiassistant.backend.ai.chat.message.repository;

import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessageSource;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AIMessageSourceRepository extends JpaRepository<AIMessageSource, Long> {

    @Query("SELECT s FROM AIMessageSource s " +
            "JOIN FETCH s.documentChunk dc " +
            "JOIN FETCH dc.documentVersion dv " +
            "JOIN FETCH dv.document " +
            "WHERE s.aiMessage.id IN :messageIds")
    List<AIMessageSource> findByMessageIdIn(@Param("messageIds") Collection<Long> messageIds);

    // No cascade from AIMessage anymore (unidirectional, child-owned) - hard delete needs this explicitly.
    void deleteByAiMessage_ConversationId(Long conversationId);

    List<AIMessageSource> findByAiMessageIdOrderByIdAsc(
            Long messageId
    );
}


