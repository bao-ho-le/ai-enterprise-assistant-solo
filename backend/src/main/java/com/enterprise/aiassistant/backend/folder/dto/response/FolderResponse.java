package com.enterprise.aiassistant.backend.folder.dto.response;

import com.enterprise.aiassistant.backend.folder.enums.FolderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderResponse {

    private Long id;

    private String name;

    private Long parentId;

    private FolderStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Thời điểm xoá mềm (null nếu folder đang ACTIVE) — dùng cho màn Trash.
    private LocalDateTime deletedAt;
}
