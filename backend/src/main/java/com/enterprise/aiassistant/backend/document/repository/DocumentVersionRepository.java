package com.enterprise.aiassistant.backend.document.repository;

import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    int countByDocumentId(Long documentId);

    Optional<DocumentVersion> findTopByDocumentIdOrderByVersionNumberDesc(Long documentId);

}
