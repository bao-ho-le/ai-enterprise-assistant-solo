package com.enterprise.aiassistant.backend.document.mapper;

import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.entity.DocumentAccess;
import com.enterprise.aiassistant.backend.document.dto.response.*;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.helper.DocumentHelper;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.user.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;


@Component
public class DocumentMapper {

    public DocumentMoveResponse toDocumentMoveResponse(Document document, Folder folder) {
        return DocumentMoveResponse.builder()
                .documentId(document.getId())
                .folderId(folder.getId())
                .build();
    }

    public Document toDocument(
            DocumentUploadRequest request,
            Folder folder,
            User owner,
            Department department
    ) {

        return Document.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .documentType(request.getDocumentType())
                .folder(folder)
                .owner(owner)
                .department(department)
                .build();
    }

    public DocumentAccess toDocumentAccess(Document document, User targetUser, User grantedBy) {

        return DocumentAccess.builder()
                .document(document)
                .user(targetUser)
                .grantedBy(grantedBy)
                .build();
    }

    public DocumentShareResponse toShareResponse(DocumentAccess access) {

        Document document = access.getDocument();
        User owner = document.getOwner();
        Department department = document.getDepartment();
        User sharedUser = access.getUser();

        return DocumentShareResponse.builder()
                .documentAccessId(access.getId())
                .documentId(document.getId())
                .documentTitle(document.getTitle())
                .ownerId(owner != null ? owner.getId() : null)
                .ownerName(owner != null ? owner.getFullName() : null)
                .departmentId(department != null ? department.getId() : null)
                .departmentName(department != null ? department.getName() : null)
                .sharedUserId(sharedUser.getId())
                .sharedUserName(sharedUser.getFullName())
                .sharedUserEmail(sharedUser.getEmail())
                .grantedByName(access.getGrantedBy() != null ? access.getGrantedBy().getFullName() : null)
                .sharedAt(access.getCreatedAt())
                .build();
    }


    public DocumentVersion toDocumentVersion(
            Document document,
            FileEntity fileEntity,
            int versionNumber,
            String changeNote
    ) {

        return DocumentVersion.builder()
                .document(document)
                .file(fileEntity)
                .versionNumber(versionNumber)
                .changeNote(changeNote)
                .build();
    }


    public DocumentUploadResponse toUploadResponse(
            Document document,
            FileEntity fileEntity
    ) {

        return DocumentUploadResponse.builder()
                .documentId(document.getId())
                .currentVersionId(document.getCurrentVersion().getId())
                .title(document.getTitle())
                .filename(fileEntity.getOriginalFilename())
                .versionNumber(document.getCurrentVersion().getVersionNumber())
                .status(document.getStatus())
                .build();
    }

    public DocumentRestoreResponse toRestoreResponse(Document document) {

        return DocumentRestoreResponse.builder()
                .documentId(document.getId())
                .title(document.getTitle())
                .documentType(document.getDocumentType())
                .status(document.getStatus())
                .deletedAt(document.getDeletedAt())
                .build();
    }

    public DocumentUpdateMetadataResponse toUpdateMetadataReponse(Document document) {

        return DocumentUpdateMetadataResponse.builder()
                .documentId(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .documentType(document.getDocumentType())
                .build();
    }

    public UploadNewVersionResponse toUploadNewVersionResponse(
            Document document,
            DocumentVersion version,
            FileEntity fileEntity) {

        return UploadNewVersionResponse.builder()
                .documentId(document.getId())
                .currentVersionId(version.getId())
                .filename(fileEntity.getOriginalFilename())
                .versionNumber(version.getVersionNumber())
                .changeNote(version.getChangeNote())
                .status(version.getStatus().name())
                .build();
    }


    public DocumentDownloadResource toDocumentDownloadResource(
            Resource resource,
            FileEntity fileEntity) {

        return DocumentDownloadResource.builder()
                .resource(resource)
                .originalFilename(fileEntity.getOriginalFilename())
                .mimeType(fileEntity.getMimeType())
                .fileSize(fileEntity.getFileSize())
                .build();
    }

    public DocumentDetailResponse.DocumentInfo toDocumentInfo(Document document) {
        return new DocumentDetailResponse.DocumentInfo(
                document.getTitle(),
                document.getDescription(),
                document.getStatus(),
                document.getDocumentType(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    public DocumentDetailResponse.CurrentVersionInfo toCurrentVersionInfo(
            DocumentVersion version,
            FileEntity file
    ) {
        return new DocumentDetailResponse.CurrentVersionInfo(
                version.getId(),
                version.getVersionNumber(),
                version.getStatus(),
                file.getOriginalFilename(),
                file.getExtension(),
                file.getFileSize(),
                version.getCreatedAt()
        );
    }

    public List<DocumentDetailResponse.VersionHistoryItem> toVersionHistory(
            List<DocumentVersion> versions
    ) {
        return versions.stream()
                .sorted(Comparator.comparing(DocumentVersion::getVersionNumber).reversed())
                .map(v -> new DocumentDetailResponse.VersionHistoryItem(
                        v.getId(),
                        v.getVersionNumber(),
                        v.getFile().getOriginalFilename(),
                        v.getChangeNote(),
                        v.getStatus(),
                        v.getCreatedAt()
                ))
                .toList();
    }

    public DocumentDetailResponse.AdvancedInfo toAdvancedInfo(
            FileEntity file
    ) {
        return new DocumentDetailResponse.AdvancedInfo(
                file.getStorageProvider(),
                file.getBucketName(),
                file.getObjectKey(),
                file.getChecksum()
        );
    }


    public ResponseEntity<Resource> toDownloadResponse(
            DocumentDownloadResource downloadResource
    ) {
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                .contentType(DocumentHelper.resolveMediaType(downloadResource.getMimeType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(
                                        downloadResource.getOriginalFilename(),
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                );

        if (downloadResource.getFileSize() != null) {
            responseBuilder.contentLength(downloadResource.getFileSize());
        }

        return responseBuilder.body(downloadResource.getResource());
    }
}

