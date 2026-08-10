package com.enterprise.aiassistant.backend.ai.knowledge.generation.repository;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedDocumentType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedContentRepository extends JpaRepository<GeneratedContent, Long> {

    Slice<GeneratedContent> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );

    Slice<GeneratedContent> findByGeneratedTypeOrderByCreatedAtDesc(
            GeneratedDocumentType generatedType,
            Pageable pageable
    );
}
