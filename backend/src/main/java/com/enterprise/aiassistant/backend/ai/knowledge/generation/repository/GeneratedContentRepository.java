package com.enterprise.aiassistant.backend.ai.knowledge.generation.repository;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedDocumentType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GeneratedContentRepository extends JpaRepository<GeneratedContent, Long> {

    // Generated content thuộc về conversation sinh ra nó, nên chỉ chủ conversation mới thấy.
    @Query("""
            SELECT c FROM GeneratedContent c
            WHERE EXISTS (
                SELECT 1 FROM Generation g
                WHERE g.generatedContent = c AND g.aiConversation.user.id = :userId
            )
            AND (CAST(:generatedType AS string) IS NULL OR c.generatedType = :generatedType)
            ORDER BY c.createdAt DESC
            """)
    Slice<GeneratedContent> findOwnedByUser(
            @Param("userId") Long userId,
            @Param("generatedType") GeneratedDocumentType generatedType,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(g) > 0 FROM Generation g
            WHERE g.generatedContent.id = :generatedContentId
              AND g.aiConversation.user.id = :userId
            """)
    boolean isOwnedByUser(
            @Param("generatedContentId") Long generatedContentId,
            @Param("userId") Long userId
    );
}
