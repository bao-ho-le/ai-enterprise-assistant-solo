package com.enterprise.aiassistant.backend.admin.document.controller;

import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.response.*;
import com.enterprise.aiassistant.backend.document.mapper.DocumentMapper;
import com.enterprise.aiassistant.backend.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Admin nhìn toàn hệ thống; nghiệp vụ vẫn dùng lại DocumentService (không copy logic).
@RestController
@RequestMapping("${api.prefix}/admin/documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDocumentController {

    private final DocumentService documentService;

    private final DocumentMapper documentMapper;

    @GetMapping
    public Page<DocumentListResponse> getDocuments(
            DocumentFilterRequest filter,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return documentService.getDocumentsForAdmin(filter, pageable);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDetailResponse> getDocumentDetail(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentService.getDocumentDetail(documentId));
    }

    @GetMapping("/{documentId}/shares")
    public ResponseEntity<List<DocumentShareResponse>> getDocumentShares(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentService.getDocumentShares(documentId));
    }

    @GetMapping("/{documentId}/{versionId}/download")
    public ResponseEntity<Resource> downloadSelectedVersion(
            @PathVariable Long documentId,
            @PathVariable Long versionId
    ) {
        return documentMapper.toDownloadResponse(
                documentService.downloadSelectedVersion(documentId, versionId)
        );
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{documentId}/restore")
    public ResponseEntity<DocumentRestoreResponse> restoreDocument(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentService.restoreDocument(documentId));
    }
}
