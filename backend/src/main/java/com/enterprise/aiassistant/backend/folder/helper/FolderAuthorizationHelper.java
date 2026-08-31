package com.enterprise.aiassistant.backend.folder.helper;

import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.enums.Role;
import org.springframework.stereotype.Component;

// Luật ABAC thuần cho Folder — không chạm repository.
@Component
public class FolderAuthorizationHelper {

    public boolean canRead(UserPrincipal principal, Folder folder) {

        if (!principal.hasPermission(Permission.FOLDER_READ)) {
            return false;
        }

        if (principal.getRole() == Role.ADMIN || principal.getRole() == Role.SUPERVISOR) {
            return true;
        }

        // Folder chưa gắn department (root và dữ liệu cũ) là folder dùng chung.
        return folder.getDepartment() == null
                || isSameDepartment(principal, folder);
    }

    public void requireRead(UserPrincipal principal, Folder folder) {

        if (!canRead(principal, folder)) {
            throw new AuthorizationException(ErrorCode.ACCESS_DENIED);
        }
    }

    // Folder mới luôn thuộc department của người tạo. Cha là root thì chỉ ADMIN được tạo;
    // cha đã thuộc department khác thì phải cùng department.
    public void requireCreate(UserPrincipal principal, Folder parent) {

        requirePermission(principal, Permission.FOLDER_CREATE);

        if (principal.getRole() == Role.ADMIN) {
            return;
        }

        if (parent.getParent() == null || !isSameDepartment(principal, parent)) {
            throw new AuthorizationException(ErrorCode.ACCESS_DENIED);
        }
    }

    public void requireUpdate(UserPrincipal principal, Folder folder) {

        requirePermission(principal, Permission.FOLDER_UPDATE);
        requireWriteScope(principal, folder);
    }

    public void requireDelete(UserPrincipal principal, Folder folder) {

        requirePermission(principal, Permission.FOLDER_DELETE);
        requireWriteScope(principal, folder);
    }

    private void requirePermission(UserPrincipal principal, Permission permission) {

        if (!principal.hasPermission(permission)) {
            throw new AuthorizationException(ErrorCode.PERMISSION_DENIED);
        }
    }

    // Folder root (không có cha) chỉ ADMIN mới được ghi.
    private void requireWriteScope(UserPrincipal principal, Folder folder) {

        if (principal.getRole() == Role.ADMIN) {
            return;
        }

        if (folder.getParent() == null || !isSameDepartment(principal, folder)) {
            throw new AuthorizationException(ErrorCode.ACCESS_DENIED);
        }
    }

    private boolean isSameDepartment(UserPrincipal principal, Folder folder) {
        return folder.getDepartment() != null
                && principal.getDepartmentId() != null
                && folder.getDepartment().getId().equals(principal.getDepartmentId());
    }
}
