package com.enterprise.aiassistant.backend.admin.trash.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TrashItemResponse {

    private Long itemId;

    private String name;

    // DOCUMENT hoặc FOLDER — frontend hiển thị chung một bảng.
    private String type;

    private String ownerName;

    private String departmentName;

    private String deletedByName;

    private LocalDateTime deletedAt;
}
