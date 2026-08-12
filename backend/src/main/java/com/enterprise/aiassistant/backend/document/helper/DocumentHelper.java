package com.enterprise.aiassistant.backend.document.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.FileStorageException;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUpdateMetadataRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.ShareDocumentRequest;
import com.enterprise.aiassistant.backend.document.dto.request.UploadNewVersionRequest;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.mapper.DocumentMapper;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.storage.config.FileUploadProperties;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentHelper {

    private final FileUploadProperties fileUploadProperties;

    private final DocumentVersionRepository versionRepository;

    private final DocumentMapper documentMapper;

    private static final int MAX_FILES = 10;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB


    private static final Set<String> ALLOWED_TYPES =
            Set.of(
                    // PDF
                    "application/pdf",

                    // Microsoft Word
                    "application/msword", // .doc
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx

                    // Microsoft Excel
                    "application/vnd.ms-excel", // .xls
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx

                    // Text
                    "text/plain"
            );

    private static final Set<String> ALLOWED_SORTS = Set.of("newest", "oldest");


    public void validateFiles(List<MultipartFile> files) {

        if (files == null) {
            throw new DocumentException(
                    ErrorCode.FILE_REQUIRED
            );
        }

        if (files.isEmpty()) {
            throw new DocumentException(
                    ErrorCode.EMPTY_FILE
            );
        }


        if (files.size() > MAX_FILES) {
            throw new DocumentException(
                    ErrorCode.TOO_MANY_FILES
            );
        }


        files.forEach(this::validateFile);
    }

    public void validateFile(MultipartFile file) {

        if (file == null) {
            throw new DocumentException(
                    ErrorCode.FILE_REQUIRED
            );
        }

        if (file.isEmpty()) {
            throw new DocumentException(
                    ErrorCode.EMPTY_FILE
            );
        }


        if (file.getSize() > MAX_FILE_SIZE) {
            throw new DocumentException(
                    ErrorCode.FILE_TOO_LARGE
            );
        }


        validateContentType(file);
    }


    private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();

        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(
                    ErrorCode.UNSUPPORTED_FILE_TYPE
            );
        }
    }

    public void validateBatchRequest(
            List<MultipartFile> files,
            List<DocumentUploadRequest> documents
    ) {

        if (documents == null || documents.isEmpty()) {
            throw new DocumentException(
                    ErrorCode.DOCUMENTS_METADATA_REQUIRED
            );
        }

        if (files.size() != documents.size()) {
            throw new DocumentException(
                    ErrorCode.FILE_METADATA_MISMATCH
            );
        }


        if (documents.stream()
                .anyMatch(item ->
                        item.getTitle() == null
                                || item.getTitle().isBlank()
                )) {

            throw new DocumentException(
                    ErrorCode.DOCUMENT_TITLE_REQUIRED
            );
        }


        if (documents.stream()
                .anyMatch(item ->
                        item.getDocumentType() == null
                )) {

            throw new DocumentException(
                    ErrorCode.DOCUMENT_TYPE_REQUIRED
            );
        }
    }

    public void validateRequest(DocumentUploadRequest request) {
        if (request == null) {
            throw new DocumentException(
                    ErrorCode.REQUEST_REQUIRED
            );
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new DocumentException(
                    ErrorCode.DOCUMENT_TITLE_REQUIRED
            );
        }

        if (request.getDocumentType() == null) {
            throw new DocumentException(
                    ErrorCode.INVALID_DOCUMENT_TYPE
            );
        }

    }

    private int getNextVersionNumber(Document document) {
        return versionRepository
                .findTopByDocumentIdOrderByVersionNumberDesc(document.getId())
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
    }

    public DocumentVersion createNewVersion(Document document, FileEntity newFile, String changeNote) {
        int nextVersion = getNextVersionNumber(document);

        return documentMapper.toDocumentVersion(
                document,
                newFile,
                nextVersion,
                changeNote
        );
    }

    public void validateDocumentId(Long documentId) {

        if (documentId == null) {
            throw new DocumentException(
                    ErrorCode.DOCUMENT_ID_REQUIRED
            );
        }

        if (documentId <= 0) {
            throw new DocumentException(ErrorCode.DOCUMENT_ID_INVALID);
        }
    }

    public void validateFilter(DocumentFilterRequest request) {

        if (request == null) {
            return;
        }

        if (request.getSort() != null
                && !ALLOWED_SORTS.contains(request.getSort().toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.INVALID_SORT_OPTION);
        }

        if (request.getFromDate() != null
                && request.getToDate() != null
                && request.getFromDate().isAfter(request.getToDate())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        if (request.getMinSize() != null && request.getMinSize() < 0) {
            throw new BusinessException(ErrorCode.INVALID_FILE_SIZE);
        }

        if (request.getMaxSize() != null && request.getMaxSize() < 0) {
            throw new BusinessException(ErrorCode.INVALID_FILE_SIZE);
        }

        if (request.getMinSize() != null
                && request.getMaxSize() != null
                && request.getMinSize() > request.getMaxSize()) {
            throw new BusinessException(ErrorCode.INVALID_FILE_SIZE_RANGE);
        }

        if (request.getKeyword() != null
                && request.getKeyword().length() > 255) {
            throw new BusinessException(ErrorCode.KEYWORD_TOO_LONG);
        }
    }

    public void validateVersionRequest(
            UploadNewVersionRequest request
    ) {

        if (request == null) {

            throw new BusinessException(
                    ErrorCode.REQUEST_REQUIRED
            );
        }

        if (request.getChangeNote() != null &&
                request.getChangeNote().length() > 255) {

            throw new BusinessException(
                    ErrorCode.CHANGE_NOTE_TOO_LONG
            );
        }

        validateTitleIfPresent(request.getTitle());
        validateDescriptionIfPresent(request.getDescription());
    }

    public void validateDocumentStatus(Document document) {

        if (document.getStatus() == DocumentStatus.DELETED) {

            throw new DocumentException(
                    ErrorCode.DOCUMENT_DELETED
            );
        }
    }

    // Đối lập với validateDocumentStatus: chỉ document đang DELETED mới được restore
    public void validateDocumentIsDeleted(Document document){

        if(document.getStatus() != DocumentStatus.DELETED){

            throw new DocumentException(
                    ErrorCode.DOCUMENT_NOT_DELETED
            );
        }
    }

    public void validateUpdateMetadataRequest(
            DocumentUpdateMetadataRequest request
    ) {

        if (request == null) {

            throw new BusinessException(
                    ErrorCode.REQUEST_REQUIRED
            );
        }
    }

    public void validateMetadata(
            DocumentUpdateMetadataRequest request
    ) {
        validateTitleIfPresent(request.getTitle());
        validateDescriptionIfPresent(request.getDescription());
    }

    private void validateTitleIfPresent(String title) {

        if (title == null) {
            return;
        }

        if (title.isBlank()) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_TITLE_REQUIRED
            );
        }

        if (title.length() > 255) {
            throw new BusinessException(
                    ErrorCode.TITLE_TOO_LONG
            );
        }
    }

    private void validateDescriptionIfPresent(String description) {

        if (description != null && description.length() > 1000) {
            throw new BusinessException(
                    ErrorCode.DESCRIPTION_TOO_LONG
            );
        }
    }

    public static MediaType resolveMediaType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(mimeType);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public void validateDocumentVersion(Long versionId) {
        if (versionId == null) {
            throw new DocumentException(
                    ErrorCode.DOCUMENT_VERSION_NOT_FOUND
            );
        }
    }

    public void validateFileStorageMetadata(FileEntity file) {

        if (file == null) {
            throw new FileStorageException(ErrorCode.FILE_NOT_FOUND);
        }

        if (file.getBucketName() == null
                || file.getBucketName().isBlank()
                || file.getObjectKey() == null
                || file.getObjectKey().isBlank()) {
            throw new DocumentException(
                    ErrorCode.FILE_STORAGE_METADATA_INVALID
            );
        }
    }

    // ===================== Shared documents =====================

    public void validateShareRequest(ShareDocumentRequest request) {

        if (request == null) {
            throw new DocumentException(ErrorCode.REQUEST_REQUIRED);
        }

        validateTargetUserId(request.getTargetUserId());
    }

    public void validateTargetUserId(Long targetUserId) {

        if (targetUserId == null) {
            throw new DocumentException(ErrorCode.DOCUMENT_ACCESS_USER_REQUIRED);
        }

        if (targetUserId <= 0) {
            throw new DocumentException(ErrorCode.USER_ID_INVALID);
        }
    }

    // Owner đã có toàn quyền sẵn, chia sẻ lại cho chính họ chỉ tạo bản ghi rác.
    public void validateShareTarget(Document document, User targetUser) {

        if (document.getOwner() != null
                && document.getOwner().getId().equals(targetUser.getId())) {

            throw new DocumentException(ErrorCode.DOCUMENT_ACCESS_OWNER_CANNOT_BE_TARGET);
        }
    }
}
