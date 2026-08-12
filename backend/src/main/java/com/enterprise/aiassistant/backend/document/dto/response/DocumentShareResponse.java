package com.enterprise.aiassistant.backend.document.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DocumentShareResponse {

    private Long documentAccessId;

    private Long documentId;

    private String documentTitle;

    private Long ownerId;

    private String ownerName;

    private Long departmentId;

    private String departmentName;

    private Long sharedUserId;

    private String sharedUserName;

    private String sharedUserEmail;

    private String grantedByName;

    private LocalDateTime sharedAt;
}
