package com.enterprise.aiassistant.backend.authorization;

import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.helper.AIUsageScopeHelper;
import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.helper.DocumentAuthorizationHelper;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.folder.helper.FolderAuthorizationHelper;
import com.enterprise.aiassistant.backend.user.entity.Permission;
import com.enterprise.aiassistant.backend.user.entity.Role;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.helper.RolePermissionHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Ma trận RBAC + ABAC theo yêu cầu: employee/manager/supervisor/admin trên document,
// folder và AI usage scope. Chạy thuần, không cần Spring context hay database.
class AuthorizationPolicyTest {

    private static final long IT_DEPARTMENT_ID = 1L;
    private static final long HR_DEPARTMENT_ID = 2L;

    private final RolePermissionHelper rolePermissionHelper = new RolePermissionHelper();
    private final DocumentAuthorizationHelper documentAuthorizationHelper = new DocumentAuthorizationHelper();
    private final FolderAuthorizationHelper folderAuthorizationHelper = new FolderAuthorizationHelper();
    private final AIUsageScopeHelper aiUsageScopeHelper = new AIUsageScopeHelper();

    @Test
    void employeeReadsOwnDepartmentDocumentButNotOtherDepartment() {

        UserPrincipal employee = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID);

        Document ownDepartmentDocument = document(100L, 99L, IT_DEPARTMENT_ID);
        Document otherDepartmentDocument = document(101L, 99L, HR_DEPARTMENT_ID);

        assertTrue(documentAuthorizationHelper.canRead(employee, ownDepartmentDocument, false));
        assertFalse(documentAuthorizationHelper.canRead(employee, otherDepartmentDocument, false));
    }

    @Test
    void employeeReadsSharedDocumentFromAnotherDepartment() {

        UserPrincipal employee = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID);

        Document hrDocument = document(101L, 99L, HR_DEPARTMENT_ID);

        assertTrue(documentAuthorizationHelper.canRead(employee, hrDocument, true));

        // Explicit access chỉ cấp READ, không kéo theo quyền ghi.
        assertFalse(documentAuthorizationHelper.canUpdate(employee, hrDocument));
        assertFalse(documentAuthorizationHelper.canDelete(employee, hrDocument));
        assertFalse(documentAuthorizationHelper.canManageAccess(employee, hrDocument));
    }

    @Test
    void employeeUpdatesOnlyOwnDocument() {

        UserPrincipal employee = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID);

        Document ownDocument = document(100L, 10L, IT_DEPARTMENT_ID);
        Document colleagueDocument = document(102L, 11L, IT_DEPARTMENT_ID);

        assertTrue(documentAuthorizationHelper.canUpdate(employee, ownDocument));
        assertTrue(documentAuthorizationHelper.canDelete(employee, ownDocument));
        assertFalse(documentAuthorizationHelper.canUpdate(employee, colleagueDocument));
        assertFalse(documentAuthorizationHelper.canDelete(employee, colleagueDocument));
    }

    @Test
    void managerManagesOwnDepartmentOnly() {

        UserPrincipal manager = principal(20L, Role.MANAGER, IT_DEPARTMENT_ID);

        Document itDocument = document(100L, 10L, IT_DEPARTMENT_ID);
        Document hrDocument = document(101L, 99L, HR_DEPARTMENT_ID);

        assertTrue(documentAuthorizationHelper.canUpdate(manager, itDocument));
        assertTrue(documentAuthorizationHelper.canDelete(manager, itDocument));
        assertTrue(documentAuthorizationHelper.canManageAccess(manager, itDocument));

        assertFalse(documentAuthorizationHelper.canRead(manager, hrDocument, false));
        assertFalse(documentAuthorizationHelper.canUpdate(manager, hrDocument));
    }

    @Test
    void supervisorReadsEverythingButModifiesNothing() {

        UserPrincipal supervisor = principal(30L, Role.SUPERVISOR, null);

        Document itDocument = document(100L, 10L, IT_DEPARTMENT_ID);
        Document hrDocument = document(101L, 99L, HR_DEPARTMENT_ID);

        assertTrue(documentAuthorizationHelper.canRead(supervisor, itDocument, false));
        assertTrue(documentAuthorizationHelper.canRead(supervisor, hrDocument, false));
        assertTrue(documentAuthorizationHelper.canDownload(supervisor, hrDocument, false));

        assertFalse(documentAuthorizationHelper.canCreate(supervisor));
        assertFalse(documentAuthorizationHelper.canUpdate(supervisor, itDocument));
        assertFalse(documentAuthorizationHelper.canDelete(supervisor, itDocument));
        assertFalse(documentAuthorizationHelper.canManageAccess(supervisor, itDocument));
    }

    @Test
    void adminHasSystemWideScope() {

        UserPrincipal admin = principal(1L, Role.ADMIN, null);

        Document hrDocument = document(101L, 99L, HR_DEPARTMENT_ID);

        assertTrue(documentAuthorizationHelper.canRead(admin, hrDocument, false));
        assertTrue(documentAuthorizationHelper.canUpdate(admin, hrDocument));
        assertTrue(documentAuthorizationHelper.canDelete(admin, hrDocument));
        assertTrue(documentAuthorizationHelper.canManageAccess(admin, hrDocument));
    }

    @Test
    void folderWriteIsDepartmentScopedAndEmployeeIsReadOnly() {

        UserPrincipal employee = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID);
        UserPrincipal manager = principal(20L, Role.MANAGER, IT_DEPARTMENT_ID);

        Folder itFolder = folder(IT_DEPARTMENT_ID);
        Folder hrFolder = folder(HR_DEPARTMENT_ID);

        assertTrue(folderAuthorizationHelper.canRead(employee, itFolder));
        assertFalse(folderAuthorizationHelper.canRead(employee, hrFolder));

        assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireUpdate(employee, itFolder));

        assertDoesNotThrow(() -> folderAuthorizationHelper.requireUpdate(manager, itFolder));
        assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireDelete(manager, hrFolder));
    }

    @Test
    void aiUsageScopeNarrowsFilterPerRole() {

        AIUsageLogFilterRequest employeeFilter = new AIUsageLogFilterRequest();
        employeeFilter.setDepartmentId(HR_DEPARTMENT_ID);
        aiUsageScopeHelper.applyScope(employeeFilter, principal(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID));
        assertEquals(10L, employeeFilter.getUserId());
        assertNull(employeeFilter.getDepartmentId());

        AIUsageLogFilterRequest managerFilter = new AIUsageLogFilterRequest();
        managerFilter.setDepartmentId(HR_DEPARTMENT_ID);
        aiUsageScopeHelper.applyScope(managerFilter, principal(20L, Role.MANAGER, IT_DEPARTMENT_ID));
        assertEquals(IT_DEPARTMENT_ID, managerFilter.getDepartmentId());

        AIUsageLogFilterRequest adminFilter = new AIUsageLogFilterRequest();
        adminFilter.setDepartmentId(HR_DEPARTMENT_ID);
        aiUsageScopeHelper.applyScope(adminFilter, principal(1L, Role.ADMIN, null));
        assertEquals(HR_DEPARTMENT_ID, adminFilter.getDepartmentId());
        assertNull(adminFilter.getUserId());
    }

    @Test
    void basePermissionMappingMatchesSpecification() {

        assertTrue(rolePermissionHelper.defaultPermissionsOf(Role.EMPLOYEE)
                .contains(Permission.AI_USAGE_READ_SELF));
        assertFalse(rolePermissionHelper.defaultPermissionsOf(Role.EMPLOYEE)
                .contains(Permission.AI_USAGE_READ_DEPARTMENT));

        assertTrue(rolePermissionHelper.defaultPermissionsOf(Role.MANAGER)
                .contains(Permission.AI_USAGE_READ_DEPARTMENT));
        assertFalse(rolePermissionHelper.defaultPermissionsOf(Role.MANAGER)
                .contains(Permission.AI_USAGE_READ_ALL));

        assertFalse(rolePermissionHelper.defaultPermissionsOf(Role.SUPERVISOR)
                .contains(Permission.DOCUMENT_CREATE));
        assertFalse(rolePermissionHelper.defaultPermissionsOf(Role.EMPLOYEE)
                .contains(Permission.FOLDER_CREATE));

        assertEquals(Permission.values().length,
                rolePermissionHelper.defaultPermissionsOf(Role.ADMIN).size());
    }

    // Helper

    private UserPrincipal principal(Long userId, Role role, Long departmentId) {

        User user = User.builder()
                .id(userId)
                .username("user" + userId)
                .password("x")
                .role(role)
                .enabled(true)
                .build();

        if (departmentId != null) {
            user.setDepartment(department(departmentId));
        }

        return UserPrincipal.from(user, rolePermissionHelper.defaultPermissionsOf(role));
    }

    private Department department(Long departmentId) {
        return Department.builder().id(departmentId).name("dept-" + departmentId).build();
    }

    private Document document(Long documentId, Long ownerId, Long departmentId) {

        return Document.builder()
                .id(documentId)
                .title("doc-" + documentId)
                .owner(User.builder().id(ownerId).build())
                .department(departmentId == null ? null : department(departmentId))
                .build();
    }

    private Folder folder(Long departmentId) {

        return Folder.builder()
                .name("folder-" + departmentId)
                .department(departmentId == null ? null : department(departmentId))
                .build();
    }
}
