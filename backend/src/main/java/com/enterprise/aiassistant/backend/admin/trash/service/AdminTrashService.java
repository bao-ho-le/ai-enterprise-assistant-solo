package com.enterprise.aiassistant.backend.admin.trash.service;

import com.enterprise.aiassistant.backend.admin.trash.dto.TrashItemResponse;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.folder.enums.FolderStatus;
import com.enterprise.aiassistant.backend.folder.repository.FolderRepository;
import com.enterprise.aiassistant.backend.user.entity.Permission;
import com.enterprise.aiassistant.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Thùng rác gộp document + folder vào một danh sách duy nhất cho màn /admin/trash.
@Service
@RequiredArgsConstructor
public class AdminTrashService {

    private final DocumentRepository documentRepository;

    private final FolderRepository folderRepository;

    private final CurrentUserService currentUserService;

    // ponytail: gộp 2 nguồn rồi phân trang trong bộ nhớ — đủ cho quy mô thùng rác hiện tại,
    // đổi sang UNION view ở DB nếu số item xoá mềm lên tới hàng chục nghìn.
    @Transactional(readOnly = true)
    public Page<TrashItemResponse> getTrashItems(String keyword, String type, Pageable pageable) {

        currentUserService.requirePermission(Permission.DOCUMENT_DELETE);

        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        List<TrashItemResponse> items = new ArrayList<>();

        if (type == null || "DOCUMENT".equalsIgnoreCase(type)) {
            documentRepository
                    .findDeletedDocuments(safeKeyword, Pageable.unpaged())
                    .forEach(document -> items.add(toTrashItem(document)));
        }

        if (type == null || "FOLDER".equalsIgnoreCase(type)) {
            deletedFolders(safeKeyword).forEach(folder -> items.add(toTrashItem(folder)));
        }

        items.sort(Comparator.comparing(
                TrashItemResponse::getDeletedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        int from = (int) Math.min(pageable.getOffset(), items.size());
        int to = Math.min(from + pageable.getPageSize(), items.size());

        return new PageImpl<>(items.subList(from, to), pageable, items.size());
    }

    // Helper

    private List<Folder> deletedFolders(String keyword) {

        Page<Folder> folders = keyword == null
                ? folderRepository.findByStatusOrderByDeletedAtDesc(FolderStatus.DELETED, Pageable.unpaged())
                : folderRepository.findByStatusAndNameContainingIgnoreCaseOrderByDeletedAtDesc(
                FolderStatus.DELETED, keyword, Pageable.unpaged());

        return folders.getContent();
    }

    private TrashItemResponse toTrashItem(Document document) {

        return TrashItemResponse.builder()
                .itemId(document.getId())
                .name(document.getTitle())
                .type("DOCUMENT")
                .ownerName(fullNameOf(document.getOwner()))
                .departmentName(document.getDepartment() != null ? document.getDepartment().getName() : null)
                .deletedByName(fullNameOf(document.getDeletedBy()))
                .deletedAt(document.getDeletedAt())
                .build();
    }

    private TrashItemResponse toTrashItem(Folder folder) {

        return TrashItemResponse.builder()
                .itemId(folder.getId())
                .name(folder.getName())
                .type("FOLDER")
                .ownerName(fullNameOf(folder.getOwner()))
                .departmentName(folder.getDepartment() != null ? folder.getDepartment().getName() : null)
                .deletedByName(fullNameOf(folder.getDeletedBy()))
                .deletedAt(folder.getDeletedAt())
                .build();
    }

    private String fullNameOf(User user) {
        return user != null ? user.getFullName() : null;
    }
}
