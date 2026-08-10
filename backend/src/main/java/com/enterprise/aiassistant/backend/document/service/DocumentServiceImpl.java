package com.enterprise.aiassistant.backend.document.service;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.document.dto.request.*;
import com.enterprise.aiassistant.backend.document.dto.response.*;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.helper.DocumentHelper;
import com.enterprise.aiassistant.backend.document.mapper.DocumentMapper;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.folder.service.FolderService;
import com.enterprise.aiassistant.backend.processing.orchestration.event.DocumentVersionCreatedBatchEvent;
import com.enterprise.aiassistant.backend.processing.orchestration.event.DocumentVersionCreatedEvent;
import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.storage.mapper.FileMapper;
import com.enterprise.aiassistant.backend.storage.repository.FileRepository;
import com.enterprise.aiassistant.backend.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static com.enterprise.aiassistant.backend.common.exception.ErrorCode.DOCUMENT_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final FileStorageService fileStorageService;

    private final FileRepository fileRepository;

    private final DocumentRepository documentRepository;

    private final DocumentVersionRepository versionRepository;

    private final DocumentMapper documentMapper;

    private final FileMapper fileMapper;

    private final DocumentHelper documentHelper;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final FolderService folderService;


    @Override
    @Transactional
    public List<DocumentUploadResponse> upload(
            List<MultipartFile> files,
            DocumentBatchUploadRequest request
    ) {

        documentHelper.validateFiles(files);
        documentHelper.validateBatchRequest(files, request.getDocuments());

        List<DocumentUploadResponse> responses =
                new ArrayList<>();

        List<Long> versionIds =
                new ArrayList<>();


        for (int i = 0; i < files.size(); i++) {

            MultipartFile file = files.get(i);

            DocumentUploadRequest item =
                    request.getDocuments().get(i);


            // 1. Upload MinIO
            StoredFileDto storedFile =
                    fileStorageService.store(file);


            // 2. FileEntity
            FileEntity newFile =
                    fileMapper.toFileEntity(storedFile);

            fileRepository.save(newFile);


            // 3. Document — không truyền folderId thì tự vào thư mục gốc, document luôn thuộc 1 folder
            Folder folder = folderService.resolveTargetFolder(item.getFolderId());

            Document document =
                    documentMapper.toDocument(item, folder);

            documentRepository.save(document);


            // 4. Version
            DocumentVersion version =
                    documentMapper.toDocumentVersion(
                            document,
                            newFile,
                            1,
                            ""
                    );

            versionRepository.save(version);


            // 5. Current version
            document.setCurrentVersion(version);

            documentRepository.save(document);


            versionIds.add(version.getId());


            responses.add(
                    documentMapper.toUploadResponse(
                            document,
                            newFile
                    )
            );
        }


        applicationEventPublisher.publishEvent(
                new DocumentVersionCreatedBatchEvent(versionIds)
        );


        return responses;
    }

    @Override
    @Transactional
    public UploadNewVersionResponse uploadNewVersion(
            Long documentId,
            MultipartFile file,
            UploadNewVersionRequest request
    ) {

        documentHelper.validateDocumentId(documentId);
        documentHelper.validateFile(file);
        documentHelper.validateVersionRequest(request);


        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(DOCUMENT_NOT_FOUND));

        documentHelper.validateDocumentStatus(document);

        // Upload file mới vào storage
        StoredFileDto storedFile = fileStorageService.store(file);


        // Lưu metadata file
        FileEntity fileEntity = fileMapper.toFileEntity(storedFile);
        fileRepository.save(fileEntity);


        // Tạo version mới
        DocumentVersion newVersion = documentHelper.createNewVersion(
                document,
                fileEntity,
                request.getChangeNote()
        );

        versionRepository.save(newVersion);


        // Update current version + optional metadata carried over from the form
        document.setCurrentVersion(newVersion);

        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            document.setDescription(request.getDescription());
        }

        if (request.getDocumentType() != null) {
            document.setDocumentType(request.getDocumentType());
        }

        documentRepository.save(document);

        // Kích hoạt xử lý bất đồng bộ sau khi transaction này commit
        applicationEventPublisher.publishEvent(new DocumentVersionCreatedEvent(newVersion.getId()));


        return documentMapper.toUploadNewVersionResponse(
                document,
                newVersion,
                fileEntity
        );
    }

    @Override
    @Transactional
    public DocumentUpdateMetadataResponse updateDocumentMetadata(
            Long documentId,
            DocumentUpdateMetadataRequest request
    ) {

        documentHelper.validateDocumentId(documentId);
        documentHelper.validateUpdateMetadataRequest(request);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(DOCUMENT_NOT_FOUND));

        // Nếu document đã bị delete thì không cho cập nhật
        documentHelper.validateDocumentStatus(document);

        documentHelper.validateMetadata(request);


        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            document.setDescription(request.getDescription());
        }

        if (request.getDocumentType() != null) {
            document.setDocumentType(request.getDocumentType());
        }

        documentRepository.save(document);

        return documentMapper.toUpdateMetadataReponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDownloadResource downloadSelectedVersion(Long documentId, Long versionId) {

        documentHelper.validateDocumentId(documentId);

        documentHelper.validateDocumentVersion(versionId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(ErrorCode.DOCUMENT_NOT_FOUND));

        documentHelper.validateDocumentStatus(document);

        DocumentVersion selectedVersion = versionRepository.findById(versionId)
                .orElseThrow(() -> new DocumentException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND));


        FileEntity file = selectedVersion.getFile();

        documentHelper.validateFileStorageMetadata(file);


        Resource resource = fileStorageService.loadAsResource(
                file.getBucketName(),
                file.getObjectKey()
        );

        return documentMapper.toDocumentDownloadResource(resource, file);

    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadProcessingResource(Long versionId) {

        documentHelper.validateDocumentVersion(versionId);

        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new DocumentException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND));

        Document document = version.getDocument();

        if (document != null) {
            documentHelper.validateDocumentStatus(document);
        }

        FileEntity fileEntity = version.getFile();

        if (fileEntity == null) {
            throw new DocumentException(ErrorCode.FILE_NOT_FOUND);
        }

        return fileStorageService.loadAsResource(
                fileEntity.getBucketName(),
                fileEntity.getObjectKey()
        );
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {

        documentHelper.validateDocumentId(documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(ErrorCode.DOCUMENT_NOT_FOUND));

        documentHelper.validateDocumentStatus(document);

        document.setStatus(DocumentStatus.DELETED);
        document.setDeletedAt(java.time.LocalDateTime.now());
        documentRepository.save(document);
    }

    @Override
    @Transactional
    public DocumentRestoreResponse restoreDocument(Long documentId) {

        documentHelper.validateDocumentId(documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(DOCUMENT_NOT_FOUND));

        documentHelper.validateDocumentIsDeleted(document);

        document.setStatus(DocumentStatus.ACTIVE);
        document.setDeletedAt(null);
        documentRepository.save(document);

        return documentMapper.toRestoreResponse(document);
    }

    @Override
    @Transactional
    public Page<DocumentListResponse> getDocuments(DocumentFilterRequest filter, Pageable pageable) {

        documentHelper.validateFilter(filter);

        return documentRepository.filterDocuments(filter, pageable);
    }

    @Override
    public DocumentDetailResponse getDocumentDetail(Long documentId) {

        documentHelper.validateDocumentId(documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(DOCUMENT_NOT_FOUND));

        DocumentVersion currentVersion = document.getCurrentVersion();
        FileEntity currentFile = currentVersion.getFile();

        DocumentDetailResponse.DocumentInfo documentInfo =
                documentMapper.toDocumentInfo(document);

        DocumentDetailResponse.CurrentVersionInfo currentVersionInfo =
                documentMapper.toCurrentVersionInfo(currentVersion, currentFile);

        List<DocumentDetailResponse.VersionHistoryItem> versionHistory =
                documentMapper.toVersionHistory(document.getVersions());

        DocumentDetailResponse.AdvancedInfo advancedInfo =
                documentMapper.toAdvancedInfo(currentFile);

        return new DocumentDetailResponse(documentInfo, currentVersionInfo, versionHistory, advancedInfo);
    }

    @Override
    public boolean existsByTitle(String title) {
        return documentRepository.existsByTitle(title);
    }

    @Override
    @Transactional
    public DocumentMoveResponse moveDocument(
            Long documentId,
            MoveDocumentRequest request
    ) {

        documentHelper.validateDocumentId(documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(DOCUMENT_NOT_FOUND));

        documentHelper.validateDocumentStatus(document);

        // folderId = null nghĩa là chuyển về thư mục gốc, không phải bỏ ra ngoài mọi folder.
        Folder folder = folderService.resolveTargetFolder(request.getFolderId());

        document.setFolder(folder);

        documentRepository.save(document);

        return documentMapper.toDocumentMoveResponse(document, folder);
    }

}