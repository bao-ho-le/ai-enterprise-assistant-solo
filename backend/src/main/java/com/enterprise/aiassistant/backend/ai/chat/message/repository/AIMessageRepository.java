package com.enterprise.aiassistant.backend.ai.chat.message.repository;

import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AIMessageRepository extends JpaRepository<AIMessage, Long> {

    // Keyset/cursor pagination: messages older than `beforeId`, newest-first, one page
    // at a time. Id is monotonic with createdAt (identity PK, insert-ordered), so this
    // walks backwards through a conversation's history with no gaps/overlaps/duplicates
    // even if new messages are inserted concurrently — unlike offset (page-number)
    // pagination, a fresh cursor is always derived from the oldest id already loaded.
    // Passing Long.MAX_VALUE as beforeId fetches the latest page (no cursor yet).
    Slice<AIMessage> findByConversationIdAndIdLessThanOrderByIdDesc(
            Long conversationId,
            Long beforeId,
            Pageable pageable
    );

    long countByConversationId(Long conversationId);

    Optional<AIMessage> findByIdAndConversationId(
            Long messageId,
            Long conversationId
    );

    void deleteByConversationId(Long conversationId);
}

