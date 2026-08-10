package com.enterprise.aiassistant.backend.folder.service;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.FolderException;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.folder.dto.request.CreateFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.MoveFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.RenameFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderContentsResponse;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderResponse;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.folder.enums.FolderStatus;
import com.enterprise.aiassistant.backend.folder.helper.FolderHelper;
import com.enterprise.aiassistant.backend.folder.mapper.FolderMapper;
import com.enterprise.aiassistant.backend.folder.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FolderServiceImpl implements FolderService {

    // Tên cố định của thư mục gốc, dùng làm khoá định danh root cùng với "không có cha".
    private static final String ROOT_FOLDER_NAME = "root";

    private final FolderRepository folderRepository;

    private final DocumentRepository documentRepository;

    private final FolderMapper folderMapper;

    private final FolderHelper folderHelper;

    @Override
    @Transactional
    public void initializeRootFolderIfNotExists() {

        Optional<Folder> existingRoot = folderRepository.findFirstByNameAndParentIsNull(ROOT_FOLDER_NAME);

        if (existingRoot.isPresent()) {
            log.info("Root folder already exists. Skipping initialization.");
            return;
        }

        Folder rootFolder = folderRepository.save(
                Folder.builder().name(ROOT_FOLDER_NAME).build()
        );

        backfillOrphanFolders(rootFolder);
        backfillOrphanDocuments(rootFolder);

        log.info("Root folder initialized successfully.");
    }

    @Override
    @Transactional(readOnly = true)
    public Folder resolveTargetFolder(Long folderId) {

        if (folderId == null) {
            return getRootFolder();
        }

        folderHelper.validateFolderId(folderId);

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_NOT_FOUND));

        folderHelper.validateFolderStatus(folder);

        return folder;
    }


    @Override
    @Transactional
    public FolderResponse createFolder(CreateFolderRequest request) {

        folderHelper.validateCreateRequest(request);

        Folder parent = folderRepository.findById(request.getParentId())
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_PARENT_NOT_FOUND));

        folderHelper.validateFolderStatus(parent);

        folderHelper.validateNameNotDuplicated(request.getName(), parent);

        Folder folder = folderMapper.toFolder(request, parent);

        folderRepository.save(folder);

        return folderMapper.toFolderResponse(folder);
    }

    @Override
    @Transactional
    public FolderResponse renameFolder(Long folderId, RenameFolderRequest request) {

        folderHelper.validateFolderId(folderId);
        folderHelper.validateRenameRequest(request);

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_NOT_FOUND));

        folderHelper.validateFolderStatus(folder);
        folderHelper.validateNotRootFolder(folder);

        folderHelper.validateNameNotDuplicated(request.getName(), folder.getParent(), folder.getId());

        folder.setName(request.getName());

        folderRepository.save(folder);

        return folderMapper.toFolderResponse(folder);
    }

    @Override
    @Transactional
    public FolderResponse moveFolder(Long folderId, MoveFolderRequest request) {

        folderHelper.validateFolderId(folderId);
        folderHelper.validateParentRequired(request.getTargetParentId());

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_NOT_FOUND));

        folderHelper.validateFolderStatus(folder);
        folderHelper.validateNotRootFolder(folder);

        if (request.getTargetParentId().equals(folderId)) {
            throw new FolderException(ErrorCode.FOLDER_CANNOT_MOVE_INTO_ITSELF);
        }

        Folder targetParent = folderRepository.findById(request.getTargetParentId())
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_PARENT_NOT_FOUND));

        folderHelper.validateFolderStatus(targetParent);

        folderHelper.validateNotDescendant(folder, targetParent);

        folderHelper.validateNameNotDuplicated(folder.getName(), targetParent, folder.getId());

        folder.setParent(targetParent);

        folderRepository.save(folder);

        return folderMapper.toFolderResponse(folder);
    }

    @Override
    @Transactional
    public void deleteFolder(Long folderId) {

        folderHelper.validateFolderId(folderId);

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_NOT_FOUND));

        folderHelper.validateFolderStatus(folder);
        folderHelper.validateNotRootFolder(folder);

        // Xóa mềm (soft delete) toàn bộ cây: folder hiện tại + tất cả folder con + tất cả document bên trong,
        // giống hành vi "Move to trash" của Google Drive.
        softDeleteRecursive(folder, LocalDateTime.now());
    }


    @Override
    public FolderResponse getFolderDetail(Long folderId) {

        folderHelper.validateFolderId(folderId);

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_NOT_FOUND));

        return folderMapper.toFolderResponse(folder);
    }

    @Override
    @Transactional(readOnly = true)
    public FolderContentsResponse getFolderContents(Long folderId, Pageable pageable) {

        // folderId = null (endpoint /root/contents) trả về nội dung của root thật, không còn là "root ngầm định".
        Folder currentFolder = resolveTargetFolder(folderId);

        // Folder con luôn trả về toàn bộ trong 1 lần gọi, không phân trang — chỉ
        // document mới dùng pageable (frontend tự cuộn để lấy thêm document).
        List<Folder> subfolders = folderRepository
                .findByParentIdAndStatusOrderByNameAsc(currentFolder.getId(), FolderStatus.ACTIVE);

        Page<DocumentListResponse> documents =
                folderRepository.getDocumentsInFolder(currentFolder.getId(), pageable);

        return FolderContentsResponse.builder()
                .currentFolder(folderMapper.toFolderResponse(currentFolder))
                .breadcrumb(folderMapper.toBreadcrumb(currentFolder))
                .subfolders(folderMapper.toFolderResponseList(subfolders))
                .documents(documents)
                .build();
    }

    @Override
    @Transactional
    public FolderResponse restoreFolder(Long folderId) {

        folderHelper.validateFolderId(folderId);

        // Không dùng findById + validateFolderStatus (dùng để chặn thao tác trên folder
        // đã xoá) vì restore chính là thao tác dành riêng cho folder đang ở trạng thái DELETED.
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_NOT_FOUND));

        if (folder.getStatus() != FolderStatus.DELETED) {
            throw new FolderException(ErrorCode.FOLDER_NOT_DELETED);
        }

        // Khôi phục tên nếu đang bị trùng với 1 folder khác đã được tạo/đổi tên
        // trong lúc folder này nằm trong thùng rác, để tránh vi phạm unique constraint.
        folderHelper.validateNameNotDuplicated(folder.getName(), folder.getParent(), folder.getId());

        restoreRecursive(folder, LocalDateTime.now());

        return folderMapper.toFolderResponse(folder);
    }


    @Override
    @Transactional
    public void hardDeleteFolder(Long folderId) {

        folderHelper.validateFolderId(folderId);

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_NOT_FOUND));

        // Bắt buộc phải qua bước soft-delete (thùng rác) trước, tránh xoá nhầm
        // vĩnh viễn 1 folder đang hoạt động bình thường.
        if (folder.getStatus() != FolderStatus.DELETED) {
            throw new FolderException(ErrorCode.FOLDER_NOT_DELETED);
        }

        hardDeleteRecursive(folder);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<FolderResponse> getDeletedFolders(String keyword, Pageable pageable) {

        if (keyword == null || keyword.isBlank()) {
            return folderRepository.findByStatusOrderByDeletedAtDesc(FolderStatus.DELETED, pageable)
                    .map(folderMapper::toFolderResponse);
        }

        return folderRepository
                .findByStatusAndNameContainingIgnoreCaseOrderByDeletedAtDesc(FolderStatus.DELETED, keyword.trim(), pageable)
                .map(folderMapper::toFolderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FolderResponse> searchFolders(String keyword, Pageable pageable) {

        folderHelper.validateSearchKeyword(keyword);

        String safeKeyword = keyword.trim();

        return folderRepository
                .findByNameContainingIgnoreCaseAndStatusOrderByNameAsc(safeKeyword, FolderStatus.ACTIVE, pageable)
                .map(folderMapper::toFolderResponse);
    }

    // Helper

    // Dữ liệu tạo trước khi có bất biến "chỉ root mới không có cha": gắn lại vào root.
    private void backfillOrphanFolders(Folder rootFolder) {

        List<Folder> orphanFolders = folderRepository.findByParentIsNull().stream()
                .filter(folder -> !folder.getId().equals(rootFolder.getId()))
                .toList();

        if (orphanFolders.isEmpty()) {
            return;
        }

        orphanFolders.forEach(folder -> folder.setParent(rootFolder));

        folderRepository.saveAll(orphanFolders);
    }

    private void backfillOrphanDocuments(Folder rootFolder) {

        List<Document> orphanDocuments = documentRepository.findByFolderIsNull();

        if (orphanDocuments.isEmpty()) {
            return;
        }

        orphanDocuments.forEach(document -> document.setFolder(rootFolder));

        documentRepository.saveAll(orphanDocuments);
    }

    private void hardDeleteRecursive(Folder folder) {

        // Xoá con trước (không lọc theo status để đảm bảo dọn sạch toàn bộ cây,
        // kể cả khi có dữ liệu không đồng bộ status do thao tác thủ công trước đó).
        List<Folder> children = new ArrayList<>(folderRepository.findByParentId(folder.getId()));
        for (Folder child : children) {
            hardDeleteRecursive(child);
        }

        List<Document> documents = documentRepository.findByFolderId(folder.getId());
        documentRepository.deleteAll(documents);

        folderRepository.delete(folder);
    }

    private void restoreRecursive(Folder folder, LocalDateTime now) {

        folder.setStatus(FolderStatus.ACTIVE);
        folder.setDeletedAt(null);
        folder.setUpdatedAt(now);
        folderRepository.save(folder);

        List<Document> documents =
                documentRepository.findByFolderIdAndStatus(folder.getId(), DocumentStatus.DELETED);

        documents.forEach(document -> {
            document.setStatus(DocumentStatus.ACTIVE);
            document.setDeletedAt(null);
        });

        documentRepository.saveAll(documents);

        // Khôi phục toàn bộ cây con đã bị xoá cùng lượt (mirror của softDeleteRecursive).
        List<Folder> children =
                folderRepository.findByParentIdAndStatusOrderByNameAsc(folder.getId(), FolderStatus.DELETED);

        for (Folder child : children) {
            restoreRecursive(child, now);
        }
    }

    private void softDeleteRecursive(Folder folder, LocalDateTime now) {

        folder.setStatus(FolderStatus.DELETED);
        folder.setDeletedAt(now);
        folderRepository.save(folder);

        List<Document> documents =
                documentRepository.findByFolderIdAndStatus(folder.getId(), DocumentStatus.ACTIVE);

        documents.forEach(document -> {
            document.setStatus(DocumentStatus.DELETED);
            document.setDeletedAt(now);
        });

        documentRepository.saveAll(documents);

        List<Folder> children =
                folderRepository.findByParentIdAndStatusOrderByNameAsc(folder.getId(), FolderStatus.ACTIVE);

        for (Folder child : children) {
            softDeleteRecursive(child, now);
        }
    }

    private Folder getRootFolder() {
        return folderRepository.findFirstByNameAndParentIsNull(ROOT_FOLDER_NAME)
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_ROOT_NOT_INITIALIZED));
    }


}
