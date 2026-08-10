package com.enterprise.aiassistant.backend.ai.chat.conversation.repository;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface AIConversationDocumentRepository extends JpaRepository<AIConversationDocument, Long> {

    @Query("SELECT acd.documentVersion.id FROM AIConversationDocument acd WHERE acd.conversation.id = :conversationId")
    List<Long> findDocumentVersionIdsByConversationId(@Param("conversationId") Long conversationId);


    @Query("SELECT acd FROM AIConversationDocument acd " +
            "JOIN FETCH acd.documentVersion dv " +
            "JOIN FETCH dv.document " +
            "WHERE acd.conversation.id = :conversationId " +
            "ORDER BY acd.createdAt ASC")
    List<AIConversationDocument> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") Long conversationId);

    // No cascade from AIConversation anymore (unidirectional, child-owned) - hard delete needs this explicitly.
    void deleteByConversationId(Long conversationId);

    @Query("SELECT acd FROM AIConversationDocument acd " +
            "JOIN FETCH acd.documentVersion dv " +
            "JOIN FETCH dv.document " +
            "WHERE acd.conversation.id = :conversationId")
    List<AIConversationDocument> findByAiConversationIdWithDocument(@Param("conversationId") Long conversationId);

    Optional<AIConversationDocument>
    findByConversationIdAndDocumentVersionId(
            Long conversationId,
            Long documentVersionId);

    @Query("SELECT COUNT(acd) > 0 FROM AIConversationDocument acd " +
            "WHERE acd.conversation.id = :conversationId " +
            "AND acd.documentVersion.document.status = :status")
    boolean existsByConversationIdAndDocumentVersionDocumentStatus(
            @Param("conversationId") Long conversationId,
            @Param("status") DocumentStatus status);

}
