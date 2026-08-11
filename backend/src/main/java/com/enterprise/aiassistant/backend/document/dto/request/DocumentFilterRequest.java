package com.enterprise.aiassistant.backend.document.dto.request;

import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class DocumentFilterRequest {

    private String keyword;

    /**
     * newest
     * oldest
     */
    private String sort;

    private DocumentType documentType;

    private String extension;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime toDate;

    private Long minSize;

    private Long maxSize;

    // DocumentVersion.status (processing pipeline state — "Processing" filter)
    private VersionStatus status;

    // Document.status (ACTIVE/DELETED — "Status" filter)
    private DocumentStatus documentStatus;

    // Chỉ Admin dùng (qua /admin/documents) — user thường bị giới hạn bởi DocumentAccessScope.
    private Long ownerId;

    private Long departmentId;

}
