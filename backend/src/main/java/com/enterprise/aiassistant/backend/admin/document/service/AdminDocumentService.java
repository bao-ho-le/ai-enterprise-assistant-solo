package com.enterprise.aiassistant.backend.admin.document.service;

import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.response.*;
import com.enterprise.aiassistant.backend.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Admin nhìn toàn hệ thống; permission + ABAC vẫn nằm trong DocumentService của module document/,
// ở đây chỉ orchestrate cho controller (không lọc theo owner/department như user thường).
@Service
@RequiredArgsConstructor
public class AdminDocumentService {

    private final DocumentService documentService;

    @Transactional(readOnly = true)
    public Page<DocumentListResponse> getDocuments(DocumentFilterRequest filter, Pageable pageable) {
        return documentService.getDocumentsForAdmin(filter, pageable);
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse getDocumentDetail(Long documentId) {
        return documentService.getDocumentDetail(documentId);
    }

    @Transactional(readOnly = true)
    public List<DocumentShareResponse> getDocumentShares(Long documentId) {
        return documentService.getDocumentShares(documentId);
    }

    @Transactional(readOnly = true)
    public DocumentDownloadResource downloadSelectedVersion(Long documentId, Long versionId) {
        return documentService.downloadSelectedVersion(documentId, versionId);
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        documentService.deleteDocument(documentId);
    }

    @Transactional
    public DocumentRestoreResponse restoreDocument(Long documentId) {
        return documentService.restoreDocument(documentId);
    }
}
