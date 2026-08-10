package com.enterprise.aiassistant.backend.document.repository;

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

@Repository
public class DocumentRepositoryCustomImpl implements DocumentRepositoryCustom {


    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public Page<DocumentListResponse> filterDocuments(
            DocumentFilterRequest filter,
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
                    d.deletedAt
                )
                
                FROM Document d
                
                JOIN d.currentVersion v
                
                JOIN v.file f
                
                WHERE 1=1
                
            """);


        Map<String, Object> params = new HashMap<>();


        appendFilters(jpql, params, filter);


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
                countDocuments(filter)
        );
    }


    private long countDocuments(DocumentFilterRequest filter) {

        StringBuilder jpql = new StringBuilder("""
                
                SELECT COUNT(d)

                FROM Document d

                JOIN d.currentVersion v

                JOIN v.file f

                WHERE 1=1
                
                """);


        Map<String, Object> params = new HashMap<>();
        appendFilters(jpql, params, filter);

        TypedQuery<Long> query =
                entityManager.createQuery(jpql.toString(), Long.class);

        params.forEach(query::setParameter);


        return query.getSingleResult();
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
