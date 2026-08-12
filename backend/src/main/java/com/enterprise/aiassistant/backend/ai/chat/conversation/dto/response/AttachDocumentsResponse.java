package com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachDocumentsResponse {

    private List<DocumentQaConversationDetailResponse.AttachedDocumentItem> documents;
}
