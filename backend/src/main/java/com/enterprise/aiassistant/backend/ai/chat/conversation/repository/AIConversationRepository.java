package com.enterprise.aiassistant.backend.ai.chat.conversation.repository;


import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;

public interface AIConversationRepository extends JpaRepository<AIConversation, Long> {

    Optional<AIConversation> findByIdAndStatus(Long conversationId, ConversationStatus status);

    boolean existsByIdAndStatus(Long conversationId, ConversationStatus status);

    @Query(
            value = """
                    SELECT new com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.ConversationResponse(
                        c.id,
                        c.title,
                        c.conversationType,
                        c.status,
                        (SELECT COUNT(m) FROM AIMessage m WHERE m.conversation = c),
                        (SELECT MAX(m2.createdAt) FROM AIMessage m2 WHERE m2.conversation = c),
                        c.createdAt,
                        c.updatedAt,
                        c.deletedAt
                    )
                    FROM AIConversation c
                    WHERE c.status = :status
                    AND (:conversationType IS NULL OR c.conversationType = :conversationType)
                    ORDER BY c.createdAt DESC
                    """
    )
    Slice<ConversationResponse> filterConversations(
            @Param("conversationType") ConversationType conversationType,
            @Param("status") ConversationStatus status,
            Pageable pageable
    );

    // Soft-deleted only: status is pinned to DELETED here so no client-supplied filter
    // can widen this query back to active conversations. Ordered by most recently deleted.
    @Query(
            value = """
                    SELECT new com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.ConversationResponse(
                        c.id,
                        c.title,
                        c.conversationType,
                        c.status,
                        (SELECT COUNT(m) FROM AIMessage m WHERE m.conversation = c),
                        (SELECT MAX(m2.createdAt) FROM AIMessage m2 WHERE m2.conversation = c),
                        c.createdAt,
                        c.updatedAt,
                        c.deletedAt
                    )
                    FROM AIConversation c
                    WHERE c.status = com.enterprise.aiassistant.backend.ai.chat.conversation.enums.ConversationStatus.DELETED
                    AND (:conversationType IS NULL OR c.conversationType = :conversationType)
                    ORDER BY c.deletedAt DESC
                    """
    )
    Slice<ConversationResponse> filterDeletedConversations(
            @Param("conversationType") ConversationType conversationType,
            Pageable pageable
    );

}
