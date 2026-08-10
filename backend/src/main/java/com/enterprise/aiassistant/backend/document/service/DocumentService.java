package com.enterprise.aiassistant.backend.document.service;

import com.enterprise.aiassistant.backend.document.dto.request.*;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentMoveResponse;
import com.enterprise.aiassistant.backend.document.dto.response.*;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    List<DocumentUploadResponse> upload(
            List<MultipartFile> files,
            DocumentBatchUploadRequest request
    );

    UploadNewVersionResponse uploadNewVersion(
            Long documentId,
            MultipartFile file,
            UploadNewVersionRequest request
    );


    DocumentDownloadResource downloadSelectedVersion(
            Long documentId,
            Long versionId
    );

    Resource loadProcessingResource(
            Long versionId
    );

    void deleteDocument(Long documentId);

    DocumentRestoreResponse restoreDocument(Long documentId);


    DocumentUpdateMetadataResponse updateDocumentMetadata(Long documentId, DocumentUpdateMetadataRequest request);

    boolean existsByTitle(String title);

    Page<DocumentListResponse> getDocuments(DocumentFilterRequest filter, Pageable pageable);

    DocumentDetailResponse getDocumentDetail(Long documentId);

    DocumentMoveResponse moveDocument(Long documentId, MoveDocumentRequest request);
}
