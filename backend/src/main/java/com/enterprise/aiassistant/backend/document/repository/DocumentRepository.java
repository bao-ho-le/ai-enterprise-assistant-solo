package com.enterprise.aiassistant.backend.document.repository;

import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface DocumentRepository extends JpaRepository<Document, Long>,
        DocumentRepositoryCustom {
    boolean existsByTitle(String title);

    List<Document> findByFolderIdAndStatus(Long folderId, DocumentStatus status);

    // Dùng cho hard delete folder: lấy tất cả document trong folder bất kể status.
    List<Document> findByFolderId(Long folderId);

    // Backfill lúc khởi động: document cũ tạo trước khi có thư mục gốc bắt buộc.
    List<Document> findByFolderIsNull();

    long countByStatus(DocumentStatus status);

    long countByDepartmentIdAndStatus(Long departmentId, DocumentStatus status);

    List<Document> findByIdIn(List<Long> documentIds);

    // Thùng rác Admin: mọi document đã xoá mềm, mới xoá gần nhất lên trước.
    @Query("""
            SELECT d FROM Document d
            WHERE d.status = com.enterprise.aiassistant.backend.document.enums.DocumentStatus.DELETED
              AND (CAST(:keyword AS string) IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            ORDER BY d.deletedAt DESC
            """)
    Page<Document> findDeletedDocuments(@Param("keyword") String keyword, Pageable pageable);
}
