package com.enterprise.aiassistant.backend.user.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.user.entity.Permission;
import com.enterprise.aiassistant.backend.user.entity.Role;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

// Bảng permission mặc định của 4 base role + validate cho role/permission API.
@Component
public class RolePermissionHelper {

    private static final Set<Permission> ADMIN_PERMISSIONS =
            EnumSet.allOf(Permission.class);

    private static final Set<Permission> MANAGER_PERMISSIONS = EnumSet.of(
            Permission.USER_READ,

            Permission.DEPARTMENT_READ,
            Permission.DEPARTMENT_MANAGE_MEMBERS,

            Permission.DOCUMENT_READ,
            Permission.DOCUMENT_CREATE,
            Permission.DOCUMENT_UPDATE,
            Permission.DOCUMENT_DELETE,
            Permission.DOCUMENT_DOWNLOAD,
            Permission.DOCUMENT_MANAGE_ACCESS,

            Permission.FOLDER_READ,
            Permission.FOLDER_CREATE,
            Permission.FOLDER_UPDATE,
            Permission.FOLDER_DELETE,

            Permission.AI_CHAT,
            Permission.AI_DOCUMENT_QA,
            Permission.AI_DOCUMENT_SUMMARY,
            Permission.AI_DOCUMENT_GENERATION,
            Permission.AI_SEMANTIC_SEARCH,

            Permission.CONVERSATION_CREATE,
            Permission.CONVERSATION_READ,
            Permission.CONVERSATION_UPDATE,
            Permission.CONVERSATION_DELETE,

            Permission.AI_USAGE_READ_SELF,
            Permission.AI_USAGE_READ_DEPARTMENT
    );

    private static final Set<Permission> EMPLOYEE_PERMISSIONS = EnumSet.of(
            Permission.DEPARTMENT_READ,

            Permission.DOCUMENT_READ,
            Permission.DOCUMENT_CREATE,
            Permission.DOCUMENT_UPDATE,
            Permission.DOCUMENT_DELETE,
            Permission.DOCUMENT_DOWNLOAD,
            Permission.DOCUMENT_MANAGE_ACCESS,

            Permission.FOLDER_READ,

            Permission.AI_CHAT,
            Permission.AI_DOCUMENT_QA,
            Permission.AI_DOCUMENT_SUMMARY,
            Permission.AI_DOCUMENT_GENERATION,
            Permission.AI_SEMANTIC_SEARCH,

            Permission.CONVERSATION_CREATE,
            Permission.CONVERSATION_READ,
            Permission.CONVERSATION_UPDATE,
            Permission.CONVERSATION_DELETE,

            Permission.AI_USAGE_READ_SELF
    );

    private static final Set<Permission> SUPERVISOR_PERMISSIONS = EnumSet.of(
            Permission.DEPARTMENT_READ,

            Permission.DOCUMENT_READ,
            Permission.DOCUMENT_DOWNLOAD,

            Permission.FOLDER_READ,

            Permission.AI_DOCUMENT_QA,
            Permission.AI_DOCUMENT_SUMMARY,
            Permission.AI_SEMANTIC_SEARCH,

            Permission.CONVERSATION_CREATE,
            Permission.CONVERSATION_READ,
            Permission.CONVERSATION_UPDATE,
            Permission.CONVERSATION_DELETE
    );

    private static final Map<Role, Set<Permission>> DEFAULT_PERMISSIONS = Map.of(
            Role.ADMIN, ADMIN_PERMISSIONS,
            Role.MANAGER, MANAGER_PERMISSIONS,
            Role.EMPLOYEE, EMPLOYEE_PERMISSIONS,
            Role.SUPERVISOR, SUPERVISOR_PERMISSIONS
    );

    public Set<Permission> defaultPermissionsOf(Role role) {
        return DEFAULT_PERMISSIONS.getOrDefault(role, Set.of());
    }

    public void validateRole(Role role) {
        if (role == null) {
            throw new AuthorizationException(ErrorCode.ROLE_REQUIRED);
        }
    }

    // ADMIN luôn phải giữ đủ quyền quản trị, nếu không hệ thống sẽ tự khoá chính mình.
    public void validateUpdatePermissions(Role role, Collection<Permission> permissions) {
        validateRole(role);

        if (permissions == null) {
            throw new AuthorizationException(ErrorCode.ROLE_PERMISSIONS_REQUIRED);
        }

        if (permissions.contains(null)) {
            throw new AuthorizationException(ErrorCode.PERMISSION_INVALID);
        }

        if (role == Role.ADMIN && !permissions.containsAll(ADMIN_PERMISSIONS)) {
            throw new AuthorizationException(ErrorCode.ADMIN_ROLE_PERMISSIONS_IMMUTABLE);
        }
    }
}
