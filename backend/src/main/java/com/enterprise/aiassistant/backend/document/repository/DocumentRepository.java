package com.enterprise.aiassistant.backend.document.repository;

import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface DocumentRepository extends JpaRepository<Document, Long>,
        DocumentRepositoryCustom {
    boolean existsByTitle(String title);

    List<Document> findByFolderIdAndStatus(Long folderId, DocumentStatus status);

    // Dùng cho hard delete folder: lấy tất cả document trong folder bất kể status.
    List<Document> findByFolderId(Long folderId);

    // Backfill lúc khởi động: document cũ tạo trước khi có thư mục gốc bắt buộc.
    List<Document> findByFolderIsNull();
}
