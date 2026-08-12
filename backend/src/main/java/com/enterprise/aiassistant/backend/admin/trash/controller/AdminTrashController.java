package com.enterprise.aiassistant.backend.admin.trash.controller;

import com.enterprise.aiassistant.backend.admin.trash.dto.TrashItemResponse;
import com.enterprise.aiassistant.backend.admin.trash.service.AdminTrashService;
import com.enterprise.aiassistant.backend.document.service.DocumentService;
import com.enterprise.aiassistant.backend.folder.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/admin/trash")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTrashController {

    private final AdminTrashService adminTrashService;

    private final DocumentService documentService;

    private final FolderService folderService;

    @GetMapping
    public Page<TrashItemResponse> getTrashItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return adminTrashService.getTrashItems(keyword, type, pageable);
    }

    @PostMapping("/documents/{documentId}/restore")
    public ResponseEntity<Void> restoreDocument(@PathVariable Long documentId) {
        documentService.restoreDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/folders/{folderId}/restore")
    public ResponseEntity<Void> restoreFolder(@PathVariable Long folderId) {
        folderService.restoreFolder(folderId);
        return ResponseEntity.noContent().build();
    }

    // Xoá vĩnh viễn chỉ áp dụng cho folder — cơ chế hard delete an toàn đã có sẵn ở FolderService.
    @DeleteMapping("/folders/{folderId}")
    public ResponseEntity<Void> permanentlyDeleteFolder(@PathVariable Long folderId) {
        folderService.hardDeleteFolder(folderId);
        return ResponseEntity.noContent().build();
    }
}
