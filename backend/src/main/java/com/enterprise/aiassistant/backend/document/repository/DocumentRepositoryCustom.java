package com.enterprise.aiassistant.backend.document.repository;

import com.enterprise.aiassistant.backend.document.dto.DocumentAccessScope;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DocumentRepositoryCustom {

    // scope luôn phải khác null. Chỉ scope.unrestricted() = true (Admin/Supervisor,
    // xem DocumentAuthorizationService#currentAccessScope) mới bỏ qua lọc ABAC.
    Page<DocumentListResponse> filterDocuments(
            DocumentFilterRequest filter,
            DocumentAccessScope scope,
            Pageable pageable
    );

}
