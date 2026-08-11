package com.enterprise.aiassistant.backend.admin.shared.controller;

import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentShareResponse;
import com.enterprise.aiassistant.backend.document.mapper.DocumentMapper;
import com.enterprise.aiassistant.backend.document.repository.DocumentAccessRepository;
import com.enterprise.aiassistant.backend.document.service.DocumentService;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Toàn bộ lượt chia sẻ trong hệ thống. Thu hồi vẫn đi qua DocumentService để dùng chung
// validate + authorization, không viết lại logic ở đây.
@RestController
@RequestMapping("${api.prefix}/admin/shared-documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSharedDocumentController {

    private final DocumentAccessRepository documentAccessRepository;

    private final DocumentMapper documentMapper;

    private final DocumentService documentService;

    private final CurrentUserService currentUserService;

    @GetMapping
    public Page<DocumentShareResponse> getSharedDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long sharedUserId,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {

        currentUserService.requirePermission(Permission.DOCUMENT_MANAGE_ACCESS);

        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        return documentAccessRepository
                .searchSharedDocuments(safeKeyword, ownerId, departmentId, sharedUserId, pageable)
                .map(documentMapper::toShareResponse);
    }

    @DeleteMapping("/{documentId}/users/{targetUserId}")
    public ResponseEntity<Void> revokeAccess(
            @PathVariable Long documentId,
            @PathVariable Long targetUserId
    ) {
        documentService.revokeDocumentAccess(documentId, targetUserId);
        return ResponseEntity.noContent().build();
    }
}
