package com.enterprise.aiassistant.backend.document.dto.response;

import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;


@Builder
public record DocumentDetailResponse(
        DocumentInfo documentInfo,
        CurrentVersionInfo currentVersion,
        List<VersionHistoryItem> versionHistory,
        AdvancedInfo advancedInfo
) {

    public record DocumentInfo(
            String title,
            String description,
            DocumentStatus status,
            DocumentType documentType,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long ownerId,
            String ownerName
    ) {
    }

    public record CurrentVersionInfo(
            Long versionId,
            Integer versionNumber,
            VersionStatus versionStatus,
            String fileName,
            String extension,
            Long fileSize,
            LocalDateTime uploadedDate
    ) {
    }

    public record VersionHistoryItem(
            Long versionId,
            Integer versionNumber,
            String fileName,
            String changeNote,
            VersionStatus status,
            LocalDateTime createdAt
    ) {
    }

    public record AdvancedInfo(
            String storageProvider,
            String bucketName,
            String objectKey,
            String checksum
    ) {
    }
}
