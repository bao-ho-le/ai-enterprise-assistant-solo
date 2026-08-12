package com.enterprise.aiassistant.backend.folder.repository;

import com.enterprise.aiassistant.backend.document.dto.DocumentAccessScope;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FolderRepositoryCustom {

    // Document trong folder vẫn phải qua lọc ABAC — user chỉ thấy phần mình có quyền đọc.
    Page<DocumentListResponse> getDocumentsInFolder(
            Long folderId,
            DocumentAccessScope scope,
            Pageable pageable
    );
}
