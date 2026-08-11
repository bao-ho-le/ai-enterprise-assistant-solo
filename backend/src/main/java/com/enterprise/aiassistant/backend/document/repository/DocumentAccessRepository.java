package com.enterprise.aiassistant.backend.document.repository;

import com.enterprise.aiassistant.backend.document.entity.DocumentAccess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentAccessRepository extends JpaRepository<DocumentAccess, Long> {

    boolean existsByDocumentIdAndUserId(Long documentId, Long userId);

    Optional<DocumentAccess> findByDocumentIdAndUserId(Long documentId, Long userId);

    List<DocumentAccess> findByDocumentIdOrderByCreatedAtAsc(Long documentId);

    void deleteByDocumentId(Long documentId);

    @Query("SELECT a.document.id FROM DocumentAccess a WHERE a.user.id = :userId")
    List<Long> findDocumentIdsSharedWithUser(@Param("userId") Long userId);

    // Admin shared-documents: 1 dòng = 1 lượt chia sẻ, lọc theo owner/department/người được chia sẻ.
    // Fetch sẵn các quan hệ to-one để mapper không phải bắn thêm query cho từng dòng.
    @Query(
            value = """
                    SELECT a FROM DocumentAccess a
                    JOIN FETCH a.document d
                    LEFT JOIN FETCH d.owner
                    LEFT JOIN FETCH d.department
                    JOIN FETCH a.user
                    LEFT JOIN FETCH a.grantedBy
                    WHERE (CAST(:keyword AS string) IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
                      AND (CAST(:ownerId AS string) IS NULL OR d.owner.id = :ownerId)
                      AND (CAST(:departmentId AS string) IS NULL OR d.department.id = :departmentId)
                      AND (CAST(:sharedUserId AS string) IS NULL OR a.user.id = :sharedUserId)
                    ORDER BY a.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(a) FROM DocumentAccess a
                    JOIN a.document d
                    WHERE (CAST(:keyword AS string) IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
                      AND (CAST(:ownerId AS string) IS NULL OR d.owner.id = :ownerId)
                      AND (CAST(:departmentId AS string) IS NULL OR d.department.id = :departmentId)
                      AND (CAST(:sharedUserId AS string) IS NULL OR a.user.id = :sharedUserId)
                    """
    )
    Page<DocumentAccess> searchSharedDocuments(
            @Param("keyword") String keyword,
            @Param("ownerId") Long ownerId,
            @Param("departmentId") Long departmentId,
            @Param("sharedUserId") Long sharedUserId,
            Pageable pageable
    );
}
