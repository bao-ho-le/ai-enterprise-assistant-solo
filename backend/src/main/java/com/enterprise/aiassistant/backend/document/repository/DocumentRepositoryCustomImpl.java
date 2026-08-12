package com.enterprise.aiassistant.backend.document.repository;

import com.enterprise.aiassistant.backend.document.dto.DocumentAccessScope;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Repository
public class DocumentRepositoryCustomImpl implements DocumentRepositoryCustom {


    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public Page<DocumentListResponse> filterDocuments(
            DocumentFilterRequest filter,
            DocumentAccessScope scope,
            Pageable pageable
    ) {


        StringBuilder jpql = new StringBuilder("""

                SELECT new com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse(
                    d.id,
                    d.title,
                    v.createdAt,
                    f.extension,
                    d.documentType,
                    f.fileSize,
                    v.status,
                    d.status,
                    d.folder.id,
                    d.deletedAt,
                    owner.id,
                    owner.fullName,
                    dept.id,
                    dept.name,
                    deleter.fullName
                )

                    FROM Document d

                    JOIN d.currentVersion v

                    JOIN v.file f

                    LEFT JOIN d.owner owner

                    LEFT JOIN d.department dept

                    LEFT JOIN d.deletedBy deleter

                    WHERE 1=1

                """);


        Map<String, Object> params = new HashMap<>();


        appendFilters(jpql, params, filter);
        appendAccessScope(jpql, params, scope);


        // =========================
        // Sort
        // =========================

        if ("oldest".equalsIgnoreCase(filter.getSort())) {

            jpql.append(" ORDER BY v.createdAt ASC");

        } else {

            jpql.append(" ORDER BY v.createdAt DESC");
        }

        TypedQuery<DocumentListResponse> query =
                entityManager.createQuery(jpql.toString(), DocumentListResponse.class);

        params.forEach(query::setParameter);

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(
                query.getResultList(),
                pageable,
                countDocuments(filter, scope)
        );
    }


    // Helper

    private long countDocuments(DocumentFilterRequest filter, DocumentAccessScope scope) {

        StringBuilder jpql = new StringBuilder("""

                SELECT COUNT(d)

                FROM Document d

                JOIN d.currentVersion v

                JOIN v.file f

                WHERE 1=1

                """);


        Map<String, Object> params = new HashMap<>();
        appendFilters(jpql, params, filter);
        appendAccessScope(jpql, params, scope);

        TypedQuery<Long> query =
                entityManager.createQuery(jpql.toString(), Long.class);

        params.forEach(query::setParameter);


        return query.getSingleResult();
    }


    // ABAC đẩy thẳng xuống SQL: owner của mình, cùng department, tài liệu dùng chung
    // (chưa gắn department) hoặc được chia sẻ explicit.
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


    private void appendFilters(
            StringBuilder jpql,
            Map<String, Object> params,
            DocumentFilterRequest filter
    ) {


        // =========================
        // Keyword
        // =========================

        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {

            jpql.append(" AND LOWER(d.title) LIKE LOWER(:keyword)");

            params.put(
                    "keyword",
                    "%" + filter.getKeyword() + "%"
            );
        }


        // =========================
        // Document type
        // =========================

        if (filter.getDocumentType() != null) {

            jpql.append(" AND d.documentType = :documentType");

            params.put("documentType", filter.getDocumentType());
        }


        // =========================
        // Extension
        // =========================

        if (filter.getExtension() != null &&
                !filter.getExtension().isBlank()) {

            jpql.append(" AND f.extension = :extension");
            params.put("extension", filter.getExtension());
        }


        // =========================
        // Size
        // =========================

        if (filter.getMinSize() != null) {

            jpql.append(" AND f.fileSize >= :minSize");
            params.put("minSize", filter.getMinSize());
        }

        if (filter.getMaxSize() != null) {

            jpql.append(" AND f.fileSize <= :maxSize");

            params.put("maxSize", filter.getMaxSize());
        }


        // =========================
        // Version status
        // =========================

        if (filter.getStatus() != null) {

            jpql.append(" AND v.status = :status");
            params.put("status", filter.getStatus());
        }


        // =========================
        // Document status
        // =========================

        if (filter.getDocumentStatus() != null) {

            jpql.append(" AND d.status = :documentStatus");
            params.put("documentStatus", filter.getDocumentStatus());
        }


        // =========================
        // Owner / Department (Admin)
        // =========================

        if (filter.getOwnerId() != null) {

            jpql.append(" AND d.owner.id = :ownerId");
            params.put("ownerId", filter.getOwnerId());
        }

        if (filter.getDepartmentId() != null) {

            jpql.append(" AND d.department.id = :filterDepartmentId");
            params.put("filterDepartmentId", filter.getDepartmentId());
        }


        // =========================
        // Date range
        // =========================

        if (filter.getFromDate() != null) {

            jpql.append(" AND v.createdAt >= :fromDate");
            params.put("fromDate", filter.getFromDate());
        }


        if (filter.getToDate() != null) {

            jpql.append(" AND v.createdAt <= :toDate");
            params.put("toDate", filter.getToDate());
        }
    }

}
