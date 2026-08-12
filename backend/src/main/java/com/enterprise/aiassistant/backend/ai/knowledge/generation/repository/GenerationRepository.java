package com.enterprise.aiassistant.backend.ai.knowledge.generation.repository;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.Generation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GenerationRepository extends JpaRepository<Generation, Long> {

    void deleteByAiConversationId(Long aiConversationId);

    // Hard delete needs every Generation row for the conversation (to collect their
    // generatedContentIds before deleting) - see AIConversationServiceImpl#hardDeleteConversation.
    List<Generation> findByAiConversationId(Long aiConversationId);

    Slice<Generation> findByAiConversationIdOrderByCreatedAtDesc(Long aiConversationId, Pageable pageable);

    Optional<Generation> findFirstByAiConversationIdOrderByCreatedAtDesc(Long aiConversationId);
}
