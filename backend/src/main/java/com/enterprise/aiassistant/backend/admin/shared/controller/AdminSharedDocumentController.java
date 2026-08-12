package com.enterprise.aiassistant.backend.admin.shared.controller;

import com.enterprise.aiassistant.backend.admin.shared.service.AdminSharedDocumentService;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentShareResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Toàn bộ lượt chia sẻ trong hệ thống. Logic nghiệp vụ vẫn nằm trong AdminSharedDocumentService.
@RestController
@RequestMapping("${api.prefix}/admin/shared-documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSharedDocumentController {

    private final AdminSharedDocumentService adminSharedDocumentService;

    @GetMapping
    public Page<DocumentShareResponse> getSharedDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long sharedUserId,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return adminSharedDocumentService.getSharedDocuments(
                keyword, ownerId, departmentId, sharedUserId, pageable
        );
    }

    @DeleteMapping("/{documentId}/users/{targetUserId}")
    public ResponseEntity<Void> revokeAccess(
            @PathVariable Long documentId,
            @PathVariable Long targetUserId
    ) {
        adminSharedDocumentService.revokeAccess(documentId, targetUserId);
        return ResponseEntity.noContent().build();
    }
}
