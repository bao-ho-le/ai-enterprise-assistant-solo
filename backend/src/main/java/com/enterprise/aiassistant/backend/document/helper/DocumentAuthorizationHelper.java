package com.enterprise.aiassistant.backend.document.helper;

import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.enums.Role;
import org.springframework.stereotype.Component;

// Luật ABAC thuần cho Document — không chạm repository.
// Thứ tự quyết định: Permission -> Ownership -> Department scope -> Explicit access.
@Component
public class DocumentAuthorizationHelper {

    public boolean canRead(UserPrincipal principal, Document document, boolean hasExplicitAccess) {

        if (!principal.hasPermission(Permission.DOCUMENT_READ)) {
            return false;
        }

        // SUPERVISOR là auditor: đọc được mọi department nhưng không sửa được gì.
        if (principal.getRole() == Role.ADMIN || principal.getRole() == Role.SUPERVISOR) {
            return true;
        }

        if (isOwner(principal, document)) {
            return true;
        }

        if (hasExplicitAccess) {
            return true;
        }

        // Tài liệu chưa gắn department (dữ liệu cũ) được coi là dùng chung.
        if (document.getDepartment() == null) {
            return true;
        }

        return isSameDepartment(principal, document);
    }

    public boolean canDownload(UserPrincipal principal, Document document, boolean hasExplicitAccess) {
        return principal.hasPermission(Permission.DOCUMENT_DOWNLOAD)
                && canRead(principal, document, hasExplicitAccess);
    }

    public boolean canUpdate(UserPrincipal principal, Document document) {
        return canModify(principal, document, Permission.DOCUMENT_UPDATE);
    }

    public boolean canDelete(UserPrincipal principal, Document document) {
        return canModify(principal, document, Permission.DOCUMENT_DELETE);
    }

    public boolean canManageAccess(UserPrincipal principal, Document document) {
        return canModify(principal, document, Permission.DOCUMENT_MANAGE_ACCESS);
    }

    public boolean canCreate(UserPrincipal principal) {
        return principal.hasPermission(Permission.DOCUMENT_CREATE);
    }

    // Explicit access chỉ cấp READ nên mọi thao tác ghi đều bỏ qua nó.
    private boolean canModify(UserPrincipal principal, Document document, Permission permission) {

        if (!principal.hasPermission(permission)) {
            return false;
        }

        if (principal.getRole() == Role.ADMIN) {
            return true;
        }

        if (isOwner(principal, document)
                && (document.getDepartment() == null || isSameDepartment(principal, document))) {
            return true;
        }

        return principal.getRole() == Role.MANAGER && isSameDepartment(principal, document);
    }

    private boolean isOwner(UserPrincipal principal, Document document) {
        return document.getOwner() != null
                && document.getOwner().getId().equals(principal.getId());
    }

    private boolean isSameDepartment(UserPrincipal principal, Document document) {
        return document.getDepartment() != null
                && principal.getDepartmentId() != null
                && document.getDepartment().getId().equals(principal.getDepartmentId());
    }
}
