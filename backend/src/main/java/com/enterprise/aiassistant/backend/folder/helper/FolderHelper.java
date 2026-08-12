package com.enterprise.aiassistant.backend.folder.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.FolderException;
import com.enterprise.aiassistant.backend.folder.dto.request.CreateFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.RenameFolderRequest;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.folder.enums.FolderStatus;
import com.enterprise.aiassistant.backend.folder.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FolderHelper {

    private final FolderRepository folderRepository;


    public void validateFolderId(Long folderId) {
        if (folderId == null) {
            throw new FolderException(ErrorCode.FOLDER_ID_REQUIRED);
        }
        if (folderId <= 0) {
            throw new FolderException(ErrorCode.FOLDER_ID_INVALID);
        }
    }

    public void validateCreateRequest(CreateFolderRequest request) {
        if (request == null) {
            throw new FolderException(ErrorCode.FOLDER_REQUEST_REQUIRED);
        }
        validateName(request.getName());
        validateParentRequired(request.getParentId());
    }

    // Root là folder duy nhất được phép không có cha, nên mọi folder tạo/di chuyển qua API đều phải có cha.
    public void validateParentRequired(Long parentFolderId) {
        if (parentFolderId == null) {
            throw new FolderException(ErrorCode.FOLDER_PARENT_REQUIRED);
        }


    }

    // Nhận diện root qua "không có cha" (bất biến của hệ thống) để khỏi phải truy vấn thêm.
    public void validateNotRootFolder(Folder folder) {
        if (folder.getParent() == null) {
            throw new FolderException(ErrorCode.FOLDER_ROOT_CANNOT_BE_MODIFIED);
        }
    }

    public void validateRenameRequest(RenameFolderRequest request) {
        if (request == null) {
            throw new FolderException(ErrorCode.FOLDER_REQUEST_REQUIRED);
        }
        validateName(request.getName());
    }

    public void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new FolderException(ErrorCode.FOLDER_NAME_REQUIRED);
        }
        if (name.length() > 255) {
            throw new FolderException(ErrorCode.FOLDER_NAME_TOO_LONG);
        }
        // Google Drive cấm các ký tự này trong tên file/thư mục trên một số OS
        if (name.contains("/") || name.contains("\\")) {
            throw new FolderException(ErrorCode.FOLDER_NAME_INVALID);
        }
    }

    public void validateSearchKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new FolderException(ErrorCode.FOLDER_SEARCH_KEYWORD_REQUIRED);
        }
    }

    public void validateFolderStatus(Folder folder) {
        if (folder.getStatus() == FolderStatus.DELETED) {
            throw new FolderException(ErrorCode.FOLDER_DELETED);
        }
    }

    // Ngăn không cho chọn chính folder đang move làm target parent của nó
    public void validateNotMoveIntoSelf(Long folderId, Long targetParentId) {
        if (targetParentId.equals(folderId)) {
            throw new FolderException(ErrorCode.FOLDER_CANNOT_MOVE_INTO_ITSELF);
        }
    }

    // Ngăn không cho di chuyển 1 folder vào chính nó hoặc vào 1 folder con cháu của nó (tránh vòng lặp vô hạn)
    public void validateNotDescendant(Folder folder, Folder targetParent) {
        Folder current = targetParent;
        while (current != null) {
            if (current.getId().equals(folder.getId())) {
                throw new FolderException(ErrorCode.FOLDER_CANNOT_MOVE_INTO_DESCENDANT);
            }
            current = current.getParent();
        }
    }

    public void validateNameNotDuplicated(String name, Folder parent) {
        boolean exists = folderRepository.existsByNameAndParentAndStatus(name, parent, FolderStatus.ACTIVE);
        if (exists) {
            throw new FolderException(ErrorCode.FOLDER_ALREADY_EXISTS);
        }
    }

    public void validateNameNotDuplicated(String name, Folder parent, Long excludeFolderId) {
        boolean exists = folderRepository.existsByNameAndParentAndStatusAndIdNot(
                name, parent, FolderStatus.ACTIVE, excludeFolderId
        );
        if (exists) {
            throw new FolderException(ErrorCode.FOLDER_ALREADY_EXISTS);
        }
    }
}
