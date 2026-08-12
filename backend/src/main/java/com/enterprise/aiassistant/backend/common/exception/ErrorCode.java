package com.enterprise.aiassistant.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
public enum ErrorCode {

    REQUEST_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "Request body is required"
    ),

    // File
    FILE_UPLOAD_FAILED(
            INTERNAL_SERVER_ERROR,
            "File upload failed"
    ),

    FILE_STORAGE_READ_FAILED(
            INTERNAL_SERVER_ERROR,
            "File storage read failed"
    ),

    FILE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "File not found"
    ),

    EMPTY_FILE(
            BAD_REQUEST,
            "File cannot be empty"
    ),

    FILE_TOO_LARGE(
            BAD_REQUEST,
            "File size exceeds limit"
    ),

    TOO_MANY_FILES(
            INTERNAL_SERVER_ERROR,
            "Maximum upload limit of 10 files exceeded"
    ),

    TEXT_EXTRACTOR_NOT_FOUND(
            INTERNAL_SERVER_ERROR,
            "No text extractor found for the file type"
    ),

    FILE_REQUIRED(
            BAD_REQUEST,
            "File is required"
    ),

    UNSUPPORTED_FILE_TYPE(
            BAD_REQUEST,
            "Unsupported file type"
    ),

    FILE_STORAGE_METADATA_INVALID(
            BAD_REQUEST,
            "File storage metadata is invalid"
    ),
    FILE_METADATA_MISMATCH(
            BAD_REQUEST,
            "Files and metadata size must match"
    ),


    // Document
    DOCUMENT_CREATION_FAILED(
            INTERNAL_SERVER_ERROR,
            "Document creation failed"
    ),

    DOCUMENTS_METADATA_REQUIRED(
            BAD_REQUEST,
            "Documents metadata is required"
    ),

    DOCUMENT_TYPE_REQUIRED(
            BAD_REQUEST,
            "Document type is required"
    ),

    DOCUMENT_VERSION_CREATION_FAILED(
            INTERNAL_SERVER_ERROR,
            "Document version creation failed"
    ),

    DOCUMENT_VERSION_INVALID_STATUS(
            CONFLICT,
            "Document version is not in a valid state for processing"
    ),

    DOCUMENT_ID_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "Document id is required"
    ),

    DOCUMENT_DELETED(
            HttpStatus.BAD_REQUEST,
            "Document has been deleted"
    ),

    DOCUMENT_NOT_DELETED(
            HttpStatus.BAD_REQUEST,
            "Document is not deleted"
    ),

    DOCUMENT_TITLE_REQUIRED(
            BAD_REQUEST,
            "Document title is required"
    ),

    DOCUMENT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Document not found"
    ),

    DOCUMENT_HAS_NO_CURRENT_VERSION(
            HttpStatus.BAD_REQUEST,
            "Document has no current version"
    ),

    DOCUMENT_VERSION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Document version not found"
    ),

    DOCUMENT_ID_INVALID(
            BAD_REQUEST,
            "Document id is invalid"
    ),

    INVALID_DOCUMENT_TYPE(
            HttpStatus.BAD_REQUEST,
            "Invalid document type"
    ),

    // Document Version
    CHANGE_NOTE_TOO_LONG(
            HttpStatus.BAD_REQUEST,
            "Change note exceeds maximum length"
    ),
    TITLE_TOO_LONG(
            HttpStatus.BAD_REQUEST,
            "Document title exceeds maximum length"
    ),

    DESCRIPTION_TOO_LONG(
            HttpStatus.BAD_REQUEST,
            "Document description exceeds maximum length"
    ),

    // ===================== Processing =====================

    TEXT_EXTRACTION_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to extract text from the document"
    ),

    DOCUMENT_CHUNKING_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to split the document into chunks"
    ),

    DOCUMENT_TEXT_EMPTY(
            INTERNAL_SERVER_ERROR,
            "No extractable text found in the document"
    ),

    DOCUMENT_PROCESSING_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to process the document"
    ),

    // ===================== Document Filter =====================

    INVALID_DATE_RANGE(
            BAD_REQUEST,
            "From date must be before or equal to to date"
    ),

    INVALID_FILE_SIZE(
            BAD_REQUEST,
            "File size must be greater than or equal to 0"
    ),

    INVALID_FILE_SIZE_RANGE(
            BAD_REQUEST,
            "Minimum file size must be less than or equal to maximum file size"
    ),

    INVALID_SORT_OPTION(
            BAD_REQUEST,
            "Sort option must be either 'newest' or 'oldest'"
    ),

    KEYWORD_TOO_LONG(
            BAD_REQUEST,
            "Keyword exceeds maximum length"
    ),

    // ===================== Embedding =====================

    EMBEDDING_TEXT_REQUIRED(
            BAD_REQUEST,
            "Text to embed is required"
    ),

    EMBEDDING_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to generate embedding"
    ),

    // ===================== Vector Store =====================

    VECTOR_UPSERT_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to save vectors to the vector store"
    ),

    VECTOR_DELETE_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to delete vectors from the vector store"
    ),

    VECTOR_SEARCH_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to search the vector store"
    ),

    VECTOR_POINTS_REQUIRED(
            BAD_REQUEST,
            "Vector points must not be null"
    ),

    VECTOR_POINT_REQUIRED(
            BAD_REQUEST,
            "Vector point must not be null"
    ),

    VECTOR_POINT_ID_REQUIRED(
            BAD_REQUEST,
            "Vector point id must not be blank"
    ),

    VECTOR_POINT_ID_INVALID(
            BAD_REQUEST,
            "Vector point id must be a valid number"
    ),

    VECTOR_REQUIRED(
            BAD_REQUEST,
            "Vector must not be null or empty"
    ),

    VECTOR_DIMENSION_INVALID(
            BAD_REQUEST,
            "Vector dimension is invalid"
    ),

    // ===================== Search =====================

    SEARCH_KEYWORD_REQUIRED(
            BAD_REQUEST,
            "Search keyword is required"
    ),

    INVALID_TOP_K(
            BAD_REQUEST,
            "topK must be between 1 and 50"
    ),

    VECTOR_SEARCH_LIMIT_INVALID(
            BAD_REQUEST,
            "Search limit is invalid"
    ),

    INVALID_DOCUMENT_ID(
            BAD_REQUEST,
            "documentId must be a positive number"
    ),
    // ===================== AI Usage =====================

    AI_USAGE_REQUEST_REQUIRED(
            BAD_REQUEST,
            "AI usage log request is required"
    ),

    AI_USAGE_CONVERSATION_TYPE_REQUIRED(
            BAD_REQUEST,
            "Conversation type is required"
    ),

    AI_USAGE_MODEL_REQUIRED(
            BAD_REQUEST,
            "Model is required"
    ),

    AI_USAGE_STATUS_REQUIRED(
            BAD_REQUEST,
            "Status is required"
    ),

    AI_USAGE_INVALID_TOKEN_COUNT(
            BAD_REQUEST,
            "Input/output tokens must not be negative"
    ),

    AI_USAGE_INVALID_ESTIMATED_COST(
            BAD_REQUEST,
            "Estimated cost must not be negative"
    ),

    AI_USAGE_MODEL_TOO_LONG(
            BAD_REQUEST,
            "Model name exceeds maximum length"
    ),

    AI_USAGE_INVALID_DAYS(
            BAD_REQUEST,
            "days must be between 1 and 90"
    ),

    // ===================== Conversation =====================

    CONVERSATION_NOT_FOUND(
            NOT_FOUND,
            "Conversation not found"
    ),

    CONVERSATION_ID_REQUIRED(
            BAD_REQUEST,
            "Conversation id is required"
    ),

    CONVERSATION_ID_INVALID(
            BAD_REQUEST,
            "Conversation id is invalid"
    ),

    RECENT_MESSAGES_LIMIT_INVALID(
            BAD_REQUEST,
            "recentMessagesLimit must be between 1 and 100"
    ),

    CONVERSATION_TYPE_NOT_GENERATION(
            BAD_REQUEST,
            "Conversation type is not a generation type"
    ),

    CONVERSATION_NOT_DELETED(
            BAD_REQUEST,
            "Conversation is not deleted"
    ),

    // ===================== Generation =====================

    GENERATION_NOT_FOUND(
            NOT_FOUND,
            "Generation not found"
    ),

    GENERATION_ID_REQUIRED(
            BAD_REQUEST,
            "Generation ID is required"
    ),

    GENERATION_ID_INVALID(
            BAD_REQUEST,
            "Generation ID must be greater than 0"
    ),

    // ===================== Generated Content =====================

    GENERATED_CONTENT_ID_REQUIRED(
            BAD_REQUEST,
            "Generated content id is required"
    ),

    GENERATED_CONTENT_ID_INVALID(
            BAD_REQUEST,
            "Generated content id is invalid"
    ),


    // ===================== Generated Document =====================


    GENERATED_CONTENT_NOT_FOUND(
            NOT_FOUND,
            "Generated content not found"
    ),


    GENERATED_CONTENT_TITLE_REQUIRED(
            BAD_REQUEST,
            "Generated content title is required"
    ),

    GENERATED_CONTENT_TITLE_TOO_LONG(
            BAD_REQUEST,
            "Generated content title exceeds maximum length"
    ),

    GENERATED_CONTENT_BODY_REQUIRED(
            BAD_REQUEST,
            "Generated content body is required"
    ),

    GENERATED_CONTENT_UPDATE_REQUEST_REQUIRED(
            BAD_REQUEST,
            "Update generated content request is required"
    ),

    // ===================== AI Conversation =====================


    DOCUMENT_NOT_ATTACHED_TO_CONVERSATION(
            BAD_REQUEST,
            "Document is not attached to the conversation"
    ),

    ATTACHED_DOCUMENT_DELETED(
            BAD_REQUEST,
            "Tài liệu đính kèm đã bị xoá, vui lòng khôi phục trước khi tiếp tục"
    ),

    // ===================== AI Conversation Message =====================


    MESSAGE_CONTENT_REQUIRED(
            BAD_REQUEST,
            "Message content is required"
    ),

    MESSAGE_CONTENT_TOO_LONG(
            BAD_REQUEST,
            "Message content exceeds maximum length"
    ),

    MESSAGE_ID_REQUIRED(
            BAD_REQUEST,
            "Message ID is required"
    ),

    MESSAGE_NOT_FOUND(
            NOT_FOUND,
            "Message not found"
    ),

    CONVERSATION_TYPE_NOT_CHAT(
            BAD_REQUEST,
            "Conversation type does not support chat messages"
    ),

    // ===================== Generation =====================

    GENERATION_INPUT_DATA_REQUIRED(
            BAD_REQUEST,
            "Generation input data is required"
    ),

    GENERATION_INPUT_DATA_INVALID(
            BAD_REQUEST,
            "Generation input data is invalid"
    ),

    GENERATION_HANDLER_NOT_FOUND(
            BAD_REQUEST,
            "No generation handler available for this conversation type"
    ),

    GENERATION_RUN_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to run generation"
    ),

    EMAIL_GENERATION_PURPOSE_REQUIRED(
            BAD_REQUEST,
            "Email purpose is required"
    ),

    REPORT_GENERATION_TITLE_REQUIRED(
            BAD_REQUEST,
            "Report title is required"
    ),

    SUMMARY_GENERATION_STYLE_REQUIRED(
            BAD_REQUEST,
            "Summary style is required"
    ),

    FORM_GENERATION_PURPOSE_REQUIRED(
            BAD_REQUEST,
            "Form purpose is required"
    ),

    GENERATION_SOURCE_DOCUMENTS_REQUIRED(
            BAD_REQUEST,
            "At least one source document must be attached"
    ),

    // ===================== Folder =====================

    FOLDER_ID_REQUIRED(
            BAD_REQUEST,
            "Folder id is required"
    ),

    FOLDER_ID_INVALID(
            BAD_REQUEST,
            "Folder id is invalid"
    ),

    FOLDER_NOT_FOUND(
            NOT_FOUND,
            "Folder not found"
    ),

    FOLDER_PARENT_NOT_FOUND(
            NOT_FOUND,
            "Parent folder not found"
    ),

    FOLDER_PARENT_REQUIRED(
            BAD_REQUEST,
            "Parent folder is required, only the root folder has no parent"
    ),

    FOLDER_REQUEST_REQUIRED(
            BAD_REQUEST,
            "Folder request is required"
    ),

    FOLDER_NAME_REQUIRED(
            BAD_REQUEST,
            "Folder name is required"
    ),

    FOLDER_NAME_TOO_LONG(
            BAD_REQUEST,
            "Folder name exceeds maximum length"
    ),

    FOLDER_NAME_INVALID(
            BAD_REQUEST,
            "Folder name contains invalid characters"
    ),

    FOLDER_ALREADY_EXISTS(
            CONFLICT,
            "A folder with this name already exists in the destination"
    ),

    FOLDER_DELETED(
            BAD_REQUEST,
            "Folder has been deleted"
    ),

    FOLDER_NOT_DELETED(
            BAD_REQUEST,
            "Folder has not been deleted, cannot restore or permanently delete"
    ),

    FOLDER_SEARCH_KEYWORD_REQUIRED(
            BAD_REQUEST,
            "Search keyword is required"
    ),

    FOLDER_CANNOT_MOVE_INTO_ITSELF(
            BAD_REQUEST,
            "A folder cannot be moved into itself"
    ),

    FOLDER_CANNOT_MOVE_INTO_DESCENDANT(
            BAD_REQUEST,
            "A folder cannot be moved into one of its own subfolders"
    ),

    FOLDER_NOT_EMPTY(
            CONFLICT,
            "Folder is not empty"
    ),

    FOLDER_ROOT_CANNOT_BE_MODIFIED(
            BAD_REQUEST,
            "Root folder cannot be renamed, moved or deleted"
    ),

    FOLDER_ROOT_NOT_INITIALIZED(
            INTERNAL_SERVER_ERROR,
            "Root folder has not been initialized"
    ),

    // ===================== AI/LLM =====================

    LLM_GENERATION_FAILED(
            INTERNAL_SERVER_ERROR,
            "Không thể tạo nội dung từ LLM"
    ),

    LLM_INVALID_PROMPT(
            BAD_REQUEST,
            "Prompt is required"
    ),

    LLM_INPUT_TOO_LARGE(
            BAD_REQUEST,
            "Nội dung tài liệu quá lớn, vượt quá giới hạn xử lý của mô hình AI. Vui lòng giảm bớt số lượng hoặc dung lượng tài liệu đính kèm"
    ),

    // ===================== Authentication/JWT =====================
    JWT_INVALID(
            UNAUTHORIZED,
            "Invalid or expired token."
    ),

    REFRESH_TOKEN_NOT_FOUND(
            UNAUTHORIZED,
            "Refresh token not found."
    ),
    REFRESH_TOKEN_REVOKED(
            UNAUTHORIZED,
            "Refresh token has been revoked."
    ),
    REFRESH_TOKEN_EXPIRED(
            UNAUTHORIZED,
            "Refresh token has expired."
    ),
    INVALID_REFRESH_TOKEN(
            UNAUTHORIZED,
            "Invalid refresh token."
    ),
    USERNAME_ALREADY_EXISTS(
            BAD_REQUEST,
            "Username already exists."
    ),
    EMAIL_ALREADY_EXISTS(
            BAD_REQUEST,
            "Email already exists."
    ),



    // ===================== User =====================
    USER_NOT_FOUND(
            UNAUTHORIZED,
            "User not found."
    ),

    // Khác USER_NOT_FOUND ở chỗ đây là user bị thao tác (admin quản lý), không phải người đang đăng nhập,
    // nên phải trả 404 chứ không phải 401 (401 sẽ khiến frontend tự đăng xuất).
    TARGET_USER_NOT_FOUND(
            NOT_FOUND,
            "User not found"
    ),

    USER_ID_REQUIRED(
            BAD_REQUEST,
            "User id is required"
    ),

    USER_ID_INVALID(
            BAD_REQUEST,
            "User id is invalid"
    ),

    USER_REQUEST_REQUIRED(
            BAD_REQUEST,
            "User request is required"
    ),

    USERNAME_REQUIRED(
            BAD_REQUEST,
            "Username is required"
    ),

    USER_EMAIL_REQUIRED(
            BAD_REQUEST,
            "Email is required"
    ),

    USER_PASSWORD_REQUIRED(
            BAD_REQUEST,
            "Password is required"
    ),

    USER_PASSWORD_TOO_SHORT(
            BAD_REQUEST,
            "Password must be at least 8 characters"
    ),

    USER_FULL_NAME_REQUIRED(
            BAD_REQUEST,
            "Full name is required"
    ),

    USER_CANNOT_MODIFY_SELF(
            BAD_REQUEST,
            "Không thể tự thay đổi role/trạng thái của chính mình"
    ),

    // ===================== Authorization =====================
    AUTHENTICATION_REQUIRED(
            UNAUTHORIZED,
            "Yêu cầu đăng nhập"
    ),

    ACCESS_DENIED(
            FORBIDDEN,
            "Bạn không có quyền truy cập tài nguyên này"
    ),

    PERMISSION_DENIED(
            FORBIDDEN,
            "Bạn không có quyền thực hiện thao tác này"
    ),

    ROLE_REQUIRED(
            BAD_REQUEST,
            "Role is required"
    ),

    PERMISSION_INVALID(
            BAD_REQUEST,
            "Permission is invalid"
    ),

    ROLE_PERMISSIONS_REQUIRED(
            BAD_REQUEST,
            "Danh sách permission là bắt buộc"
    ),

    ADMIN_ROLE_PERMISSIONS_IMMUTABLE(
            BAD_REQUEST,
            "Không thể thu hồi permission của role ADMIN"
    ),

    // ===================== Department =====================
    DEPARTMENT_NOT_FOUND(
            NOT_FOUND,
            "Department not found"
    ),

    DEPARTMENT_ID_REQUIRED(
            BAD_REQUEST,
            "Department id is required"
    ),

    DEPARTMENT_ID_INVALID(
            BAD_REQUEST,
            "Department id is invalid"
    ),

    DEPARTMENT_REQUEST_REQUIRED(
            BAD_REQUEST,
            "Department request is required"
    ),

    DEPARTMENT_NAME_REQUIRED(
            BAD_REQUEST,
            "Department name is required"
    ),

    DEPARTMENT_NAME_TOO_LONG(
            BAD_REQUEST,
            "Department name exceeds maximum length"
    ),

    DEPARTMENT_DESCRIPTION_TOO_LONG(
            BAD_REQUEST,
            "Department description exceeds maximum length"
    ),

    DEPARTMENT_ALREADY_EXISTS(
            CONFLICT,
            "Department name already exists"
    ),

    DEPARTMENT_NOT_EMPTY(
            CONFLICT,
            "Department vẫn còn thành viên, không thể xoá"
    ),

    DEPARTMENT_MANAGER_NOT_MEMBER(
            BAD_REQUEST,
            "Manager phải là thành viên của department này"
    ),

    DEPARTMENT_MANAGER_ROLE_INVALID(
            BAD_REQUEST,
            "Chỉ user có role MANAGER hoặc ADMIN mới được gán làm manager"
    ),

    USER_HAS_NO_DEPARTMENT(
            BAD_REQUEST,
            "User chưa thuộc department nào"
    ),

    // ===================== Document Access / Sharing =====================
    DOCUMENT_ACCESS_NOT_FOUND(
            NOT_FOUND,
            "Document access not found"
    ),

    DOCUMENT_ACCESS_ALREADY_GRANTED(
            CONFLICT,
            "Tài liệu đã được chia sẻ cho người dùng này"
    ),

    DOCUMENT_ACCESS_OWNER_CANNOT_BE_TARGET(
            BAD_REQUEST,
            "Không thể chia sẻ tài liệu cho chính chủ sở hữu"
    ),

    DOCUMENT_ACCESS_USER_REQUIRED(
            BAD_REQUEST,
            "Người nhận chia sẻ là bắt buộc"
    ),

    DOCUMENT_ACCESS_ID_REQUIRED(
            BAD_REQUEST,
            "Document access id is required"
    );



    private final HttpStatus status;
    private final String message;


    ErrorCode(
            HttpStatus status,
            String message
    ) {
        this.status = status;
        this.message = message;
    }
}