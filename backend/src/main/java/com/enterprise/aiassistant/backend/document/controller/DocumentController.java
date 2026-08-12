package com.enterprise.aiassistant.backend.document.controller;

import com.enterprise.aiassistant.backend.document.dto.request.DocumentBatchUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUpdateMetadataRequest;
import com.enterprise.aiassistant.backend.document.dto.request.MoveDocumentRequest;
import com.enterprise.aiassistant.backend.document.dto.request.ShareDocumentRequest;
import com.enterprise.aiassistant.backend.document.dto.request.UploadNewVersionRequest;
import com.enterprise.aiassistant.backend.document.dto.response.*;
import com.enterprise.aiassistant.backend.document.mapper.DocumentMapper;
import com.enterprise.aiassistant.backend.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("${api.prefix}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentMapper documentMapper;


    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<List<DocumentUploadResponse>> upload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestPart("request") DocumentBatchUploadRequest request
    ) {

        return ResponseEntity.ok(
                documentService.upload(files, request)
        );
    }

    @PostMapping(
            value = "/{documentId}/versions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UploadNewVersionResponse> uploadNewVersion(
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file,
            @RequestPart("request") UploadNewVersionRequest request
    ) {
        return ResponseEntity.ok(
                documentService.uploadNewVersion(documentId, file, request)
        );
    }

    @PutMapping("/{documentId}")
    public ResponseEntity<DocumentUpdateMetadataResponse> updateDocumentMetadata(
            @PathVariable Long documentId,
            @RequestBody DocumentUpdateMetadataRequest request
    ) {

        return ResponseEntity.ok(
                documentService.updateDocumentMetadata(documentId, request)
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

    @PutMapping("/{documentId}/move")
    public ResponseEntity<com.enterprise.aiassistant.backend.document.dto.response.DocumentMoveResponse> moveDocument(
            @PathVariable Long documentId,
            @RequestBody MoveDocumentRequest request
    ) {
        return ResponseEntity.ok(documentService.moveDocument(documentId, request));
    }

    @GetMapping("/{documentId}/{versionId}/download")
    public ResponseEntity<Resource> downloadSelectedVersion(
            @PathVariable Long documentId,
            @PathVariable Long versionId
    ) {

        DocumentDownloadResource documentDownloadResource =
                documentService.downloadSelectedVersion(documentId, versionId);

        return documentMapper.toDownloadResponse(documentDownloadResource);

    }

    @GetMapping
    public Page<DocumentListResponse> getDocuments(
            DocumentFilterRequest filter,

            @PageableDefault(page = 0, size = 10)
            Pageable pageable
    ) {

        return documentService.getDocuments(
                filter,
                pageable
        );
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDetailResponse> getDocumentDetail(
            @PathVariable Long documentId
    ) {
        return ResponseEntity.ok(documentService.getDocumentDetail(documentId));
    }

    @GetMapping("/check-title")
    public boolean checkTitle(@RequestParam String title) {
        return documentService.existsByTitle(title);
    }

    // Chia sẻ document cho 1 user cụ thể — chỉ cấp quyền READ/DOWNLOAD cho người nhận.
    @PostMapping("/{documentId}/shares")
    public ResponseEntity<DocumentShareResponse> shareDocument(
            @PathVariable Long documentId,
            @RequestBody ShareDocumentRequest request
    ) {
        return ResponseEntity.ok(documentService.shareDocument(documentId, request));
    }

    @GetMapping("/{documentId}/shares")
    public ResponseEntity<List<DocumentShareResponse>> getDocumentShares(
            @PathVariable Long documentId
    ) {
        return ResponseEntity.ok(documentService.getDocumentShares(documentId));
    }

    @DeleteMapping("/{documentId}/shares/{targetUserId}")
    public ResponseEntity<Void> revokeDocumentAccess(
            @PathVariable Long documentId,
            @PathVariable Long targetUserId
    ) {
        documentService.revokeDocumentAccess(documentId, targetUserId);
        return ResponseEntity.noContent().build();
    }
}
