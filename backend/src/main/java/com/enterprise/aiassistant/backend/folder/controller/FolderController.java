package com.enterprise.aiassistant.backend.folder.controller;

import com.enterprise.aiassistant.backend.folder.dto.request.CreateFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.MoveFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.RenameFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderContentsResponse;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderResponse;
import com.enterprise.aiassistant.backend.folder.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(
            @RequestBody CreateFolderRequest request
    ) {
        return ResponseEntity.ok(folderService.createFolder(request));
    }

    @PutMapping("/{folderId}")
    public ResponseEntity<FolderResponse> renameFolder(
            @PathVariable Long folderId,
            @RequestBody RenameFolderRequest request
    ) {
        return ResponseEntity.ok(folderService.renameFolder(folderId, request));
    }

    @PutMapping("/{folderId}/move")
    public ResponseEntity<FolderResponse> moveFolder(
            @PathVariable Long folderId,
            @RequestBody MoveFolderRequest request
    ) {
        return ResponseEntity.ok(folderService.moveFolder(folderId, request));
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long folderId) {
        folderService.deleteFolder(folderId);
        return ResponseEntity.noContent().build();
    }

    // Khôi phục folder đã xoá mềm (status DELETED -> ACTIVE), kèm toàn bộ cây con.
    @PostMapping("/{folderId}/restore")
    public ResponseEntity<FolderResponse> restoreFolder(@PathVariable Long folderId) {
        return ResponseEntity.ok(folderService.restoreFolder(folderId));
    }

    // Xoá vĩnh viễn (chỉ áp dụng cho folder đang ở thùng rác / status DELETED).
    @DeleteMapping("/{folderId}/hard")
    public ResponseEntity<Void> hardDeleteFolder(@PathVariable Long folderId) {
        folderService.hardDeleteFolder(folderId);
        return ResponseEntity.noContent().build();
    }

    // Literal path, khai báo tách biệt với /{folderId} — danh sách thùng rác (chỉ folder đã xoá mềm).
    @GetMapping("/deleted")
    public ResponseEntity<Page<FolderResponse>> getDeletedFolders(
            @RequestParam(required = false) String keyword,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(folderService.getDeletedFolders(keyword, pageable));
    }

    // Tìm kiếm folder theo tên (trong các folder đang hoạt động).
    @GetMapping("/search")
    public ResponseEntity<Page<FolderResponse>> searchFolders(
            @RequestParam String keyword,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(folderService.searchFolders(keyword, pageable));
    }

    @GetMapping("/{folderId}")
    public ResponseEntity<FolderResponse> getFolderDetail(@PathVariable Long folderId) {
        return ResponseEntity.ok(folderService.getFolderDetail(folderId));
    }

    @GetMapping("/{folderId}/contents")
    public ResponseEntity<FolderContentsResponse> getFolderContents(
            @PathVariable Long folderId,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(folderService.getFolderContents(folderId, pageable));
    }

    // Nội dung thư mục gốc ("My Drive")
    @GetMapping("/root/contents")
    public ResponseEntity<FolderContentsResponse> getRootContents(
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(folderService.getFolderContents(null, pageable));
    }
}
