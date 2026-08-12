package com.enterprise.aiassistant.backend.admin.document.controller;

import com.enterprise.aiassistant.backend.admin.document.service.AdminDocumentService;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.response.*;
import com.enterprise.aiassistant.backend.document.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Admin nhìn toàn hệ thống; logic nghiệp vụ vẫn nằm trong AdminDocumentService.
@RestController
@RequestMapping("${api.prefix}/admin/documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDocumentController {

    private final AdminDocumentService adminDocumentService;

    private final DocumentMapper documentMapper;

    @GetMapping
    public Page<DocumentListResponse> getDocuments(
            DocumentFilterRequest filter,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return adminDocumentService.getDocuments(filter, pageable);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDetailResponse> getDocumentDetail(@PathVariable Long documentId) {
        return ResponseEntity.ok(adminDocumentService.getDocumentDetail(documentId));
    }

    @GetMapping("/{documentId}/shares")
    public ResponseEntity<List<DocumentShareResponse>> getDocumentShares(@PathVariable Long documentId) {
        return ResponseEntity.ok(adminDocumentService.getDocumentShares(documentId));
    }

    @GetMapping("/{documentId}/{versionId}/download")
    public ResponseEntity<Resource> downloadSelectedVersion(
            @PathVariable Long documentId,
            @PathVariable Long versionId
    ) {
        return documentMapper.toDownloadResponse(
                adminDocumentService.downloadSelectedVersion(documentId, versionId)
        );
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        adminDocumentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{documentId}/restore")
    public ResponseEntity<DocumentRestoreResponse> restoreDocument(@PathVariable Long documentId) {
        return ResponseEntity.ok(adminDocumentService.restoreDocument(documentId));
    }
}
