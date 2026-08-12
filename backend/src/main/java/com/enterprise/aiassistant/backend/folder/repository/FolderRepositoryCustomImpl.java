package com.enterprise.aiassistant.backend.folder.repository;

import com.enterprise.aiassistant.backend.document.dto.DocumentAccessScope;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
public class FolderRepositoryCustomImpl implements FolderRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<DocumentListResponse> getDocumentsInFolder(
            Long folderId,
            DocumentAccessScope scope,
            Pageable pageable
    ) {

        // Mọi document đều thuộc 1 folder (root là mặc định), nên không còn nhánh "folder IS NULL".
        StringBuilder jpql = new StringBuilder(
                "SELECT new com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse(" +
                        "d.id, d.title, v.createdAt, f.extension, d.documentType, f.fileSize, v.status, " +
                        "d.status, d.folder.id, d.deletedAt, owner.id, owner.fullName, dept.id, dept.name, deleter.fullName) " +
                        "FROM Document d " +
                        "JOIN d.currentVersion v " +
                        "JOIN v.file f " +
                        "LEFT JOIN d.owner owner " +
                        "LEFT JOIN d.department dept " +
                        "LEFT JOIN d.deletedBy deleter " +
                        "WHERE d.status = com.enterprise.aiassistant.backend.document.enums.DocumentStatus.ACTIVE " +
                        "AND d.folder.id = :folderId");

        Map<String, Object> params = new HashMap<>();
        params.put("folderId", folderId);

        appendAccessScope(jpql, params, scope);

        jpql.append(" ORDER BY v.createdAt DESC");

        TypedQuery<DocumentListResponse> query =
                entityManager.createQuery(jpql.toString(), DocumentListResponse.class);

        params.forEach(query::setParameter);

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<DocumentListResponse> results = query.getResultList();

        StringBuilder countJpql = new StringBuilder(
                "SELECT COUNT(d) FROM Document d " +
                        "WHERE d.status = com.enterprise.aiassistant.backend.document.enums.DocumentStatus.ACTIVE " +
                        "AND d.folder.id = :folderId");

        Map<String, Object> countParams = new HashMap<>();
        countParams.put("folderId", folderId);

        appendAccessScope(countJpql, countParams, scope);

        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql.toString(), Long.class);

        countParams.forEach(countQuery::setParameter);

        long total = countQuery.getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    // scope = null là lỗi lập trình, không phải "bỏ qua ABAC" — fail fast thay vì âm thầm
    // trả về toàn bộ document. Chỉ scope.unrestricted() = true (Admin/Supervisor) mới bypass.
    void appendAccessScope(
            StringBuilder jpql,
            Map<String, Object> params,
            DocumentAccessScope scope
    ) {

        Objects.requireNonNull(scope, "DocumentAccessScope must not be null");

        if (scope.unrestricted()) {
            return;
        }

        jpql.append(" AND (d.department IS NULL");

        jpql.append(" OR d.owner.id = :scopeUserId");
        params.put("scopeUserId", scope.userId());

        if (scope.departmentId() != null) {
            jpql.append(" OR d.department.id = :scopeDepartmentId");
            params.put("scopeDepartmentId", scope.departmentId());
        }

        if (scope.sharedDocumentIds() != null && !scope.sharedDocumentIds().isEmpty()) {
            jpql.append(" OR d.id IN :scopeSharedDocumentIds");
            params.put("scopeSharedDocumentIds", scope.sharedDocumentIds());
        }

        jpql.append(")");
    }
}
