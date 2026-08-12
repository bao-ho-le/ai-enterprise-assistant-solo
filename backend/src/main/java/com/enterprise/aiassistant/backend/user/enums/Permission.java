package com.enterprise.aiassistant.backend.user.enums;

// Permission catalog do hệ thống định nghĩa — không cho phép tạo permission tuỳ ý lúc runtime.
// Admin chỉ được gán/bỏ gán các permission trong danh sách này cho 4 base role.
public enum Permission {

    // User
    USER_READ,
    USER_CREATE,
    USER_UPDATE,
    USER_DELETE,
    USER_ENABLE,
    USER_DISABLE,
    USER_ASSIGN_ROLE,
    USER_CHANGE_DEPARTMENT,

    // Department
    DEPARTMENT_READ,
    DEPARTMENT_CREATE,
    DEPARTMENT_UPDATE,
    DEPARTMENT_DELETE,
    DEPARTMENT_MANAGE_MEMBERS,

    // Document
    DOCUMENT_READ,
    DOCUMENT_CREATE,
    DOCUMENT_UPDATE,
    DOCUMENT_DELETE,
    DOCUMENT_DOWNLOAD,
    DOCUMENT_MANAGE_ACCESS,

    // Folder
    FOLDER_READ,
    FOLDER_CREATE,
    FOLDER_UPDATE,
    FOLDER_DELETE,

    // AI
    AI_CHAT,
    AI_DOCUMENT_QA,
    AI_DOCUMENT_SUMMARY,
    AI_DOCUMENT_GENERATION,
    AI_SEMANTIC_SEARCH,

    // Conversation
    CONVERSATION_CREATE,
    CONVERSATION_READ,
    CONVERSATION_UPDATE,
    CONVERSATION_DELETE,

    // AI Usage
    AI_USAGE_READ_SELF,
    AI_USAGE_READ_DEPARTMENT,
    AI_USAGE_READ_ALL
}
