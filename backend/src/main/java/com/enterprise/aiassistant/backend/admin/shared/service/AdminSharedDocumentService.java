package com.enterprise.aiassistant.backend.admin.shared.service;

import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentShareResponse;
import com.enterprise.aiassistant.backend.document.mapper.DocumentMapper;
import com.enterprise.aiassistant.backend.document.repository.DocumentAccessRepository;
import com.enterprise.aiassistant.backend.document.service.DocumentService;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Toàn bộ lượt chia sẻ trong hệ thống. Thu hồi vẫn đi qua DocumentService để dùng chung
// validate + authorization, không viết lại logic ở đây.
@Service
@RequiredArgsConstructor
public class AdminSharedDocumentService {

    private final DocumentAccessRepository documentAccessRepository;

    private final DocumentMapper documentMapper;

    private final DocumentService documentService;

    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public Page<DocumentShareResponse> getSharedDocuments(
            String keyword,
            Long ownerId,
            Long departmentId,
            Long sharedUserId,
            Pageable pageable
    ) {

        currentUserService.requirePermission(Permission.DOCUMENT_MANAGE_ACCESS);

        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        return documentAccessRepository
                .searchSharedDocuments(safeKeyword, ownerId, departmentId, sharedUserId, pageable)
                .map(documentMapper::toShareResponse);
    }

    @Transactional
    public void revokeAccess(Long documentId, Long targetUserId) {
        documentService.revokeDocumentAccess(documentId, targetUserId);
    }
}
