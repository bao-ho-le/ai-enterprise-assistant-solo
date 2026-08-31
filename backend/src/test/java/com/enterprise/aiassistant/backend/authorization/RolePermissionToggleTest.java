package com.enterprise.aiassistant.backend.authorization;

import com.enterprise.aiassistant.backend.admin.role.dto.RolePermissionResponse;
import com.enterprise.aiassistant.backend.admin.role.service.AdminRoleService;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.helper.AIUsageScopeHelper;
import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.helper.DocumentAuthorizationHelper;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.folder.helper.FolderAuthorizationHelper;
import com.enterprise.aiassistant.backend.user.entity.RolePermission;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.enums.Role;
import com.enterprise.aiassistant.backend.user.helper.RolePermissionHelper;
import com.enterprise.aiassistant.backend.user.repository.RolePermissionRepository;
import com.enterprise.aiassistant.backend.user.service.RolePermissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Admin bật/tắt permission cho MANAGER/EMPLOYEE/SUPERVISOR rồi kiểm tra hiệu lực thật:
// AdminRoleService -> RolePermissionService (ghi DB + xoá cache) -> UserPrincipal nạp lại
// -> Document/Folder/AIUsage authorization. Repository stub bằng map in-memory nên chạy
// thuần JUnit, không cần Spring context hay database.
class RolePermissionToggleTest {

    private static final long IT_DEPARTMENT_ID = 1L;
    private static final long HR_DEPARTMENT_ID = 2L;

    private static final long ADMIN_ID = 1L;
    private static final long EMPLOYEE_ID = 10L;
    private static final long COLLEAGUE_ID = 11L;
    private static final long MANAGER_ID = 20L;
    private static final long SUPERVISOR_ID = 30L;

    private final RolePermissionRepository rolePermissionRepository = mock(RolePermissionRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);

    private final RolePermissionHelper rolePermissionHelper = new RolePermissionHelper();
    private final DocumentAuthorizationHelper documentAuthorizationHelper = new DocumentAuthorizationHelper();
    private final FolderAuthorizationHelper folderAuthorizationHelper = new FolderAuthorizationHelper();
    private final AIUsageScopeHelper aiUsageScopeHelper = new AIUsageScopeHelper();

    // Đóng vai bảng role_permissions.
    private final Map<Role, Set<Permission>> grantStore = new EnumMap<>(Role.class);

    private RolePermissionServiceImpl rolePermissionService;
    private AdminRoleService adminRoleService;

    @BeforeEach
    void setUp() {

        when(rolePermissionRepository.findByRole(any())).thenAnswer(invocation -> {

            Role role = invocation.getArgument(0);

            return grantStore.getOrDefault(role, EnumSet.noneOf(Permission.class)).stream()
                    .map(permission -> RolePermission.builder().role(role).permission(permission).build())
                    .toList();
        });

        doAnswer(invocation -> {
            grantStore.remove((Role) invocation.getArgument(0));
            return null;
        }).when(rolePermissionRepository).deleteByRole(any());

        when(rolePermissionRepository.saveAll(any())).thenAnswer(invocation -> {

            List<RolePermission> grants = invocation.getArgument(0);

            grants.forEach(grant -> grantStore
                    .computeIfAbsent(grant.getRole(), role -> EnumSet.noneOf(Permission.class))
                    .add(grant.getPermission()));

            return grants;
        });

        rolePermissionService = new RolePermissionServiceImpl(rolePermissionRepository, rolePermissionHelper);
        adminRoleService = new AdminRoleService(rolePermissionService, currentUserService);

        // Mọi thao tác toggle trong test đều chạy dưới tài khoản ADMIN, trừ test kiểm tra chặn non-admin.
        loginAs(Role.ADMIN, ADMIN_ID, null);

        rolePermissionService.seedDefaultRolePermissionsIfMissing();
    }

    // ---------- A. Cơ chế bật/tắt của admin ----------

    @Test
    void defaultMatrixMatchesSpecificationBeforeAnyToggle() {

        assertEquals(24, rolePermissionService.getPermissions(Role.MANAGER).size());
        assertEquals(18, rolePermissionService.getPermissions(Role.EMPLOYEE).size());
        assertEquals(11, rolePermissionService.getPermissions(Role.SUPERVISOR).size());
        assertEquals(Permission.values().length, rolePermissionService.getPermissions(Role.ADMIN).size());

        // EMPLOYEE mặc định chỉ đọc folder, không có quyền ghi folder nào.
        Set<Permission> employee = rolePermissionService.getPermissions(Role.EMPLOYEE);
        assertTrue(employee.contains(Permission.FOLDER_READ));
        assertFalse(employee.contains(Permission.FOLDER_CREATE));
        assertFalse(employee.contains(Permission.FOLDER_UPDATE));
        assertFalse(employee.contains(Permission.FOLDER_DELETE));

        // SUPERVISOR là auditor: không có bất kỳ quyền ghi nào.
        Set<Permission> supervisor = rolePermissionService.getPermissions(Role.SUPERVISOR);
        assertFalse(supervisor.contains(Permission.DOCUMENT_CREATE));
        assertFalse(supervisor.contains(Permission.DOCUMENT_UPDATE));
        assertFalse(supervisor.contains(Permission.DOCUMENT_DELETE));
        assertFalse(supervisor.contains(Permission.DOCUMENT_MANAGE_ACCESS));
    }

    @Test
    void adminReadsFullMatrixAndCatalogForTheToggleScreen() {

        List<String> catalog = adminRoleService.getPermissionCatalog();
        assertEquals(Permission.values().length, catalog.size());
        assertTrue(catalog.contains("FOLDER_DELETE"));

        grant(Role.EMPLOYEE, Permission.FOLDER_DELETE);

        List<RolePermissionResponse> roles = adminRoleService.getRoles();
        assertEquals(Role.values().length, roles.size());

        RolePermissionResponse employeeRow = roles.stream()
                .filter(row -> "EMPLOYEE".equals(row.getRole()))
                .findFirst()
                .orElseThrow();

        // Màn hình admin phải thấy đúng permission vừa bật, sắp xếp theo tên.
        assertTrue(employeeRow.getPermissions().contains("FOLDER_DELETE"));
        assertEquals(19, employeeRow.getPermissions().size());
        assertEquals(employeeRow.getPermissions().stream().sorted().toList(), employeeRow.getPermissions());
    }

    @Test
    void adminGrantsThenRevokesPermissionAndBothDirectionsPersist() {

        RolePermissionResponse granted = adminRoleService.updateRolePermissions(
                Role.EMPLOYEE, plus(Role.EMPLOYEE, Permission.FOLDER_UPDATE, Permission.FOLDER_DELETE));

        assertEquals("EMPLOYEE", granted.getRole());
        assertTrue(granted.getPermissions().contains("FOLDER_DELETE"));
        assertEquals(20, granted.getPermissions().size());
        assertTrue(rolePermissionService.getPermissions(Role.EMPLOYEE).contains(Permission.FOLDER_DELETE));

        revoke(Role.EMPLOYEE, Permission.FOLDER_DELETE);

        assertFalse(rolePermissionService.getPermissions(Role.EMPLOYEE).contains(Permission.FOLDER_DELETE));
        assertTrue(rolePermissionService.getPermissions(Role.EMPLOYEE).contains(Permission.FOLDER_UPDATE));
    }

    @Test
    void adminRoleKeepsFullPermissionSetAndCannotBeStripped() {

        AuthorizationException stripped = assertThrows(AuthorizationException.class,
                () -> revoke(Role.ADMIN, Permission.USER_DELETE));
        assertEquals(ErrorCode.ADMIN_ROLE_PERMISSIONS_IMMUTABLE, stripped.getErrorCode());

        // Ghi lại đủ bộ thì hợp lệ.
        assertDoesNotThrow(() -> adminRoleService.updateRolePermissions(
                Role.ADMIN, new ArrayList<>(Arrays.asList(Permission.values()))));
        assertEquals(Permission.values().length, rolePermissionService.getPermissions(Role.ADMIN).size());
    }

    @Test
    void nonAdminCallerCannotUseAnyRolePermissionUseCase() {

        loginAs(Role.MANAGER, MANAGER_ID, IT_DEPARTMENT_ID);

        assertEquals(ErrorCode.PERMISSION_DENIED, assertThrows(AuthorizationException.class,
                () -> adminRoleService.getRoles()).getErrorCode());
        assertEquals(ErrorCode.PERMISSION_DENIED, assertThrows(AuthorizationException.class,
                () -> adminRoleService.getPermissionCatalog()).getErrorCode());
        assertEquals(ErrorCode.PERMISSION_DENIED, assertThrows(AuthorizationException.class,
                () -> adminRoleService.updateRolePermissions(
                        Role.EMPLOYEE, new ArrayList<>(List.of(Permission.FOLDER_DELETE))))
                .getErrorCode());

        // Không được ghi gì vào store.
        assertFalse(rolePermissionService.getPermissions(Role.EMPLOYEE).contains(Permission.FOLDER_DELETE));
    }

    @Test
    void invalidTogglePayloadsAreRejected() {

        assertEquals(ErrorCode.ROLE_PERMISSIONS_REQUIRED, assertThrows(AuthorizationException.class,
                () -> adminRoleService.updateRolePermissions(Role.EMPLOYEE, null)).getErrorCode());

        List<Permission> withNullEntry = new ArrayList<>();
        withNullEntry.add(Permission.FOLDER_READ);
        withNullEntry.add(null);
        assertEquals(ErrorCode.PERMISSION_INVALID, assertThrows(AuthorizationException.class,
                () -> adminRoleService.updateRolePermissions(Role.EMPLOYEE, withNullEntry)).getErrorCode());

        assertEquals(ErrorCode.ROLE_REQUIRED, assertThrows(AuthorizationException.class,
                () -> rolePermissionService.replacePermissions(
                        null, new ArrayList<>(List.of(Permission.FOLDER_READ))))
                .getErrorCode());
        assertEquals(ErrorCode.ROLE_REQUIRED, assertThrows(AuthorizationException.class,
                () -> rolePermissionService.getPermissions(null)).getErrorCode());
    }

    @Test
    void duplicatePermissionsInRequestCollapseToDistinctSet() {

        RolePermissionResponse response = adminRoleService.updateRolePermissions(Role.SUPERVISOR, new ArrayList<>(List.of(
                Permission.FOLDER_READ, Permission.FOLDER_READ, Permission.DOCUMENT_READ)));

        assertEquals(List.of("DOCUMENT_READ", "FOLDER_READ"), response.getPermissions());
        assertEquals(2, rolePermissionService.getPermissions(Role.SUPERVISOR).size());
    }

    @Test
    void permissionsAreCachedUntilAdminTogglesThem() {

        clearInvocations(rolePermissionRepository);

        rolePermissionService.getPermissions(Role.EMPLOYEE);
        rolePermissionService.getPermissions(Role.EMPLOYEE);
        verify(rolePermissionRepository, times(1)).findByRole(Role.EMPLOYEE);

        grant(Role.EMPLOYEE, Permission.FOLDER_DELETE);

        assertTrue(rolePermissionService.getPermissions(Role.EMPLOYEE).contains(Permission.FOLDER_DELETE));
        verify(rolePermissionRepository, times(2)).findByRole(Role.EMPLOYEE);
    }

    @Test
    void unseededRoleFallsBackToDefaultMapping() {

        grantStore.clear();
        rolePermissionService = new RolePermissionServiceImpl(rolePermissionRepository, rolePermissionHelper);

        assertEquals(11, rolePermissionService.getPermissions(Role.SUPERVISOR).size());
        assertEquals(18, rolePermissionService.getPermissions(Role.EMPLOYEE).size());
    }

    @Test
    void revokingEveryPermissionSilentlyFallsBackToDefaults() {

        // Store rỗng không phân biệt được "chưa seed" với "admin tắt hết", nên tắt sạch
        // permission của 1 role sẽ quay về mapping mặc định thay vì thành role không quyền.
        adminRoleService.updateRolePermissions(Role.EMPLOYEE, new ArrayList<>());

        assertEquals(18, rolePermissionService.getPermissions(Role.EMPLOYEE).size());
    }

    @Test
    void seedDoesNotOverwriteExistingGrants() {

        revoke(Role.EMPLOYEE, Permission.DOCUMENT_DELETE);

        rolePermissionService.seedDefaultRolePermissionsIfMissing();

        assertFalse(rolePermissionService.getPermissions(Role.EMPLOYEE).contains(Permission.DOCUMENT_DELETE));
        assertEquals(17, rolePermissionService.getPermissions(Role.EMPLOYEE).size());
    }

    // ---------- B. Hiệu lực trên Document ----------

    @Test
    void revokingDocumentDeleteBlocksEmployeeEvenOnOwnDocument() {

        Document ownDocument = document(100L, EMPLOYEE_ID, IT_DEPARTMENT_ID);

        assertTrue(documentAuthorizationHelper.canDelete(employee(), ownDocument));

        revoke(Role.EMPLOYEE, Permission.DOCUMENT_DELETE);
        assertFalse(documentAuthorizationHelper.canDelete(employee(), ownDocument));

        grant(Role.EMPLOYEE, Permission.DOCUMENT_DELETE);
        assertTrue(documentAuthorizationHelper.canDelete(employee(), ownDocument));
    }

    @Test
    void revokingDocumentReadCascadesToDownloadAndSharedAccess() {

        Document ownDocument = document(100L, EMPLOYEE_ID, IT_DEPARTMENT_ID);
        Document hrDocument = document(101L, COLLEAGUE_ID, HR_DEPARTMENT_ID);

        revoke(Role.EMPLOYEE, Permission.DOCUMENT_READ);

        assertFalse(documentAuthorizationHelper.canRead(employee(), ownDocument, false));
        assertFalse(documentAuthorizationHelper.canRead(employee(), hrDocument, true));
        assertFalse(documentAuthorizationHelper.canDownload(employee(), ownDocument, false));
    }

    @Test
    void revokingDownloadKeepsReadIntact() {

        Document ownDocument = document(100L, EMPLOYEE_ID, IT_DEPARTMENT_ID);

        revoke(Role.EMPLOYEE, Permission.DOCUMENT_DOWNLOAD);

        assertTrue(documentAuthorizationHelper.canRead(employee(), ownDocument, false));
        assertFalse(documentAuthorizationHelper.canDownload(employee(), ownDocument, false));
    }

    @Test
    void grantingWritePermissionsToSupervisorDoesNotBypassAbac() {

        grant(Role.SUPERVISOR, Permission.DOCUMENT_UPDATE, Permission.DOCUMENT_DELETE,
                Permission.DOCUMENT_MANAGE_ACCESS);

        UserPrincipal supervisor = principal(SUPERVISOR_ID, Role.SUPERVISOR, IT_DEPARTMENT_ID);
        Document othersDocument = document(100L, EMPLOYEE_ID, IT_DEPARTMENT_ID);
        Document ownDocument = document(101L, SUPERVISOR_ID, IT_DEPARTMENT_ID);

        // Có permission nhưng không phải owner và không phải MANAGER cùng department -> vẫn bị chặn.
        assertFalse(documentAuthorizationHelper.canUpdate(supervisor, othersDocument));
        assertFalse(documentAuthorizationHelper.canDelete(supervisor, othersDocument));
        assertFalse(documentAuthorizationHelper.canManageAccess(supervisor, othersDocument));

        // Chính tài liệu mình sở hữu trong department mình thì permission mới có hiệu lực.
        assertTrue(documentAuthorizationHelper.canUpdate(supervisor, ownDocument));
    }

    @Test
    void managerWritePermissionsStayInsideOwnDepartmentAfterToggle() {

        UserPrincipal manager = principal(MANAGER_ID, Role.MANAGER, IT_DEPARTMENT_ID);
        Document itDocument = document(100L, EMPLOYEE_ID, IT_DEPARTMENT_ID);
        Document hrDocument = document(101L, COLLEAGUE_ID, HR_DEPARTMENT_ID);

        assertTrue(documentAuthorizationHelper.canUpdate(manager, itDocument));
        assertFalse(documentAuthorizationHelper.canUpdate(manager, hrDocument));

        revoke(Role.MANAGER, Permission.DOCUMENT_UPDATE);
        assertFalse(documentAuthorizationHelper.canUpdate(
                principal(MANAGER_ID, Role.MANAGER, IT_DEPARTMENT_ID), itDocument));
    }

    @Test
    void employeeManageAccessAppliesOnlyToOwnDocuments() {

        Document ownDocument = document(100L, EMPLOYEE_ID, IT_DEPARTMENT_ID);
        Document colleagueDocument = document(101L, COLLEAGUE_ID, IT_DEPARTMENT_ID);

        assertTrue(documentAuthorizationHelper.canManageAccess(employee(), ownDocument));
        assertFalse(documentAuthorizationHelper.canManageAccess(employee(), colleagueDocument));

        revoke(Role.EMPLOYEE, Permission.DOCUMENT_MANAGE_ACCESS);
        assertFalse(documentAuthorizationHelper.canManageAccess(employee(), ownDocument));
    }

    // ---------- C. Hiệu lực trên Folder ----------

    @Test
    void grantingFolderDeleteToEmployeeEnablesNonRootDeleteButNotRoot() {

        Folder rootFolder = root();
        Folder itFolder = folder(IT_DEPARTMENT_ID);

        // Chưa cấp quyền -> chặn ở tầng permission.
        assertEquals(ErrorCode.PERMISSION_DENIED, assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireDelete(employee(), itFolder)).getErrorCode());

        grant(Role.EMPLOYEE, Permission.FOLDER_DELETE);

        assertDoesNotThrow(() -> folderAuthorizationHelper.requireDelete(employee(), itFolder));

        // Root vẫn chỉ ADMIN được ghi.
        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireDelete(employee(), rootFolder)).getErrorCode());
        assertDoesNotThrow(() -> folderAuthorizationHelper.requireDelete(admin(), rootFolder));
    }

    @Test
    void grantingFolderCreateToEmployeeStillBlocksCreatingUnderRoot() {

        grant(Role.EMPLOYEE, Permission.FOLDER_CREATE);

        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireCreate(employee(), root())).getErrorCode());

        assertDoesNotThrow(() -> folderAuthorizationHelper.requireCreate(employee(), folder(IT_DEPARTMENT_ID)));

        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireCreate(employee(), folder(HR_DEPARTMENT_ID)))
                .getErrorCode());

        assertDoesNotThrow(() -> folderAuthorizationHelper.requireCreate(admin(), root()));
    }

    @Test
    void revokingFolderReadHidesEvenOwnDepartmentFolder() {

        Folder itFolder = folder(IT_DEPARTMENT_ID);

        assertTrue(folderAuthorizationHelper.canRead(employee(), itFolder));

        revoke(Role.EMPLOYEE, Permission.FOLDER_READ);

        assertFalse(folderAuthorizationHelper.canRead(employee(), itFolder));
        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireRead(employee(), itFolder)).getErrorCode());
    }

    @Test
    void revokingFolderUpdateFromManagerReportsPermissionDeniedNotAccessDenied() {

        Folder itFolder = folder(IT_DEPARTMENT_ID);
        UserPrincipal manager = principal(MANAGER_ID, Role.MANAGER, IT_DEPARTMENT_ID);

        assertDoesNotThrow(() -> folderAuthorizationHelper.requireUpdate(manager, itFolder));

        revoke(Role.MANAGER, Permission.FOLDER_UPDATE);

        assertEquals(ErrorCode.PERMISSION_DENIED, assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireUpdate(
                        principal(MANAGER_ID, Role.MANAGER, IT_DEPARTMENT_ID), itFolder)).getErrorCode());
    }

    @Test
    void grantingFolderWriteDoesNotCrossDepartmentBoundary() {

        // Bật đủ 3 quyền ghi folder cho EMPLOYEE: chỉ có hiệu lực trong department của họ,
        // folder department khác vẫn ACCESS_DENIED (permission có nhưng ABAC scope không cho).
        grant(Role.EMPLOYEE, Permission.FOLDER_CREATE, Permission.FOLDER_UPDATE, Permission.FOLDER_DELETE);

        Folder itFolder = folder(IT_DEPARTMENT_ID);
        Folder hrFolder = folder(HR_DEPARTMENT_ID);

        assertDoesNotThrow(() -> folderAuthorizationHelper.requireUpdate(employee(), itFolder));
        assertDoesNotThrow(() -> folderAuthorizationHelper.requireDelete(employee(), itFolder));
        assertDoesNotThrow(() -> folderAuthorizationHelper.requireCreate(employee(), itFolder));

        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireUpdate(employee(), hrFolder)).getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireDelete(employee(), hrFolder)).getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireCreate(employee(), hrFolder)).getErrorCode());

        // Folder chưa gắn department (dữ liệu cũ) cũng không ghi được dù không phải root.
        Folder orphanFolder = Folder.builder().id(999L).name("orphan").parent(root()).build();
        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireDelete(employee(), orphanFolder)).getErrorCode());
        assertDoesNotThrow(() -> folderAuthorizationHelper.requireDelete(admin(), orphanFolder));
    }

    // ---------- D. Hiệu lực trên AI usage scope ----------

    @Test
    void aiUsageScopeFollowsToggledPermissions() {

        AIUsageLogFilterRequest selfScope = filterAskingFor(HR_DEPARTMENT_ID);
        aiUsageScopeHelper.applyScope(selfScope, employee());
        assertEquals(EMPLOYEE_ID, selfScope.getUserId());
        assertNull(selfScope.getDepartmentId());

        grant(Role.EMPLOYEE, Permission.AI_USAGE_READ_DEPARTMENT);
        AIUsageLogFilterRequest departmentScope = filterAskingFor(HR_DEPARTMENT_ID);
        aiUsageScopeHelper.applyScope(departmentScope, employee());
        assertEquals(IT_DEPARTMENT_ID, departmentScope.getDepartmentId());

        grant(Role.EMPLOYEE, Permission.AI_USAGE_READ_ALL);
        AIUsageLogFilterRequest allScope = filterAskingFor(HR_DEPARTMENT_ID);
        aiUsageScopeHelper.applyScope(allScope, employee());
        assertEquals(HR_DEPARTMENT_ID, allScope.getDepartmentId());
        assertNull(allScope.getUserId());

        revoke(Role.EMPLOYEE, Permission.AI_USAGE_READ_ALL, Permission.AI_USAGE_READ_DEPARTMENT,
                Permission.AI_USAGE_READ_SELF);
        assertEquals(ErrorCode.PERMISSION_DENIED, assertThrows(AuthorizationException.class,
                () -> aiUsageScopeHelper.applyScope(filterAskingFor(HR_DEPARTMENT_ID), employee()))
                .getErrorCode());
    }

    // ---------- E. Document có department = null ----------

    @Test
    void nullDepartmentDocumentIsReadableByEveryRoleHoldingReadPermission() {

        Document sharedDocument = document(100L, COLLEAGUE_ID, null);

        // Không gắn department -> coi là tài liệu dùng chung, mọi department đọc được.
        assertTrue(documentAuthorizationHelper.canRead(employee(), sharedDocument, false));
        assertTrue(documentAuthorizationHelper.canRead(
                principal(EMPLOYEE_ID, Role.EMPLOYEE, null), sharedDocument, false));
        assertTrue(documentAuthorizationHelper.canRead(
                principal(MANAGER_ID, Role.MANAGER, HR_DEPARTMENT_ID), sharedDocument, false));

        // Tắt DOCUMENT_READ của EMPLOYEE thì mất quyền đọc, MANAGER không bị ảnh hưởng.
        revoke(Role.EMPLOYEE, Permission.DOCUMENT_READ);

        assertFalse(documentAuthorizationHelper.canRead(employee(), sharedDocument, false));
        assertTrue(documentAuthorizationHelper.canRead(
                principal(MANAGER_ID, Role.MANAGER, HR_DEPARTMENT_ID), sharedDocument, false));
    }

    @Test
    void nullDepartmentDocumentIsWritableByOwnerOnlyNotByManager() {

        Document ownDocument = document(100L, EMPLOYEE_ID, null);

        // Owner ghi được dù tài liệu không thuộc department nào.
        assertTrue(documentAuthorizationHelper.canUpdate(employee(), ownDocument));
        assertTrue(documentAuthorizationHelper.canDelete(employee(), ownDocument));

        // Đồng nghiệp cùng role nhưng không phải owner thì không.
        assertFalse(documentAuthorizationHelper.canUpdate(
                principal(COLLEAGUE_ID, Role.EMPLOYEE, IT_DEPARTMENT_ID), ownDocument));

        // MANAGER đọc được (dùng chung) nhưng KHÔNG ghi được: nhánh MANAGER đòi isSameDepartment,
        // mà document.department = null nên luôn false.
        UserPrincipal manager = principal(MANAGER_ID, Role.MANAGER, IT_DEPARTMENT_ID);
        assertTrue(documentAuthorizationHelper.canRead(manager, ownDocument, false));
        assertFalse(documentAuthorizationHelper.canUpdate(manager, ownDocument));
        assertFalse(documentAuthorizationHelper.canDelete(manager, ownDocument));

        revoke(Role.EMPLOYEE, Permission.DOCUMENT_UPDATE);
        assertFalse(documentAuthorizationHelper.canUpdate(employee(), ownDocument));
    }

    @Test
    void userWithoutDepartmentLosesWriteAccessToOwnDepartmentalDocument() {

        UserPrincipal orphanEmployee = principal(EMPLOYEE_ID, Role.EMPLOYEE, null);
        Document ownItDocument = document(100L, EMPLOYEE_ID, IT_DEPARTMENT_ID);

        // Vẫn đọc được tài liệu của chính mình (nhánh isOwner nằm trước check department)...
        assertTrue(documentAuthorizationHelper.canRead(orphanEmployee, ownItDocument, false));

        // ...nhưng mất quyền ghi vì canModify đòi thêm cùng department. Gỡ department của user
        // là mất quyền sửa tài liệu do chính họ tạo, bật permission thế nào cũng không cứu được.
        assertFalse(documentAuthorizationHelper.canUpdate(orphanEmployee, ownItDocument));
        assertFalse(documentAuthorizationHelper.canDelete(orphanEmployee, ownItDocument));
        assertFalse(documentAuthorizationHelper.canManageAccess(orphanEmployee, ownItDocument));
    }

    @Test
    void userWithoutDepartmentReadsOnlyNullDepartmentOwnAndSharedDocuments() {

        UserPrincipal orphanEmployee = principal(EMPLOYEE_ID, Role.EMPLOYEE, null);

        Document sharedPoolDocument = document(100L, COLLEAGUE_ID, null);
        Document itDocument = document(101L, COLLEAGUE_ID, IT_DEPARTMENT_ID);
        Document ownItDocument = document(102L, EMPLOYEE_ID, IT_DEPARTMENT_ID);

        assertTrue(documentAuthorizationHelper.canRead(orphanEmployee, sharedPoolDocument, false));
        assertFalse(documentAuthorizationHelper.canRead(orphanEmployee, itDocument, false));
        assertTrue(documentAuthorizationHelper.canRead(orphanEmployee, itDocument, true));
        assertTrue(documentAuthorizationHelper.canRead(orphanEmployee, ownItDocument, false));
    }

    @Test
    void managerWithoutDepartmentCanOnlyWriteOwnNullDepartmentDocuments() {

        UserPrincipal orphanManager = principal(MANAGER_ID, Role.MANAGER, null);

        Document ownNullDepartmentDocument = document(100L, MANAGER_ID, null);
        Document ownItDocument = document(101L, MANAGER_ID, IT_DEPARTMENT_ID);
        Document itDocument = document(102L, EMPLOYEE_ID, IT_DEPARTMENT_ID);

        assertTrue(documentAuthorizationHelper.canUpdate(orphanManager, ownNullDepartmentDocument));
        assertFalse(documentAuthorizationHelper.canUpdate(orphanManager, ownItDocument));
        assertFalse(documentAuthorizationHelper.canUpdate(orphanManager, itDocument));
    }

    @Test
    void grantingEveryPermissionCannotOverrideDepartmentMismatch() {

        // Bật toàn bộ catalog cho EMPLOYEE: permission là điều kiện cần, ABAC vẫn là điều kiện đủ.
        adminRoleService.updateRolePermissions(
                Role.EMPLOYEE, new ArrayList<>(Arrays.asList(Permission.values())));

        UserPrincipal orphanEmployee = principal(EMPLOYEE_ID, Role.EMPLOYEE, null);
        Document itDocument = document(100L, COLLEAGUE_ID, IT_DEPARTMENT_ID);

        assertEquals(Permission.values().length, orphanEmployee.getPermissions().size());
        assertFalse(documentAuthorizationHelper.canRead(orphanEmployee, itDocument, false));
        assertFalse(documentAuthorizationHelper.canUpdate(orphanEmployee, itDocument));
        assertFalse(documentAuthorizationHelper.canDelete(orphanEmployee, itDocument));

        // Cùng bộ quyền đó nhưng user có department khớp thì mới có hiệu lực.
        assertTrue(documentAuthorizationHelper.canRead(employee(), itDocument, false));
    }

    // ---------- F. Folder có department = null ----------

    @Test
    void nullDepartmentFolderIsReadableButNeverWritableByNonAdmin() {

        Folder orphanFolder = folderWithoutDepartment();

        assertTrue(folderAuthorizationHelper.canRead(employee(), orphanFolder));
        assertTrue(folderAuthorizationHelper.canRead(
                principal(EMPLOYEE_ID, Role.EMPLOYEE, null), orphanFolder));
        assertTrue(folderAuthorizationHelper.canRead(
                principal(MANAGER_ID, Role.MANAGER, HR_DEPARTMENT_ID), orphanFolder));

        // Bật đủ quyền ghi vẫn không ghi được: requireWriteScope đòi isSameDepartment,
        // folder.department = null nên không bao giờ khớp.
        grant(Role.EMPLOYEE, Permission.FOLDER_UPDATE, Permission.FOLDER_DELETE);

        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireUpdate(employee(), orphanFolder)).getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireDelete(employee(), orphanFolder)).getErrorCode());

        // Chỉ ADMIN xử lý được folder loại này.
        assertDoesNotThrow(() -> folderAuthorizationHelper.requireUpdate(admin(), orphanFolder));
        assertDoesNotThrow(() -> folderAuthorizationHelper.requireDelete(admin(), orphanFolder));

        revoke(Role.EMPLOYEE, Permission.FOLDER_READ);
        assertFalse(folderAuthorizationHelper.canRead(employee(), orphanFolder));
    }

    @Test
    void userWithoutDepartmentCannotWriteAnyFolderEvenWithEveryFolderPermission() {

        grant(Role.EMPLOYEE, Permission.FOLDER_CREATE, Permission.FOLDER_UPDATE, Permission.FOLDER_DELETE);

        UserPrincipal orphanEmployee = principal(EMPLOYEE_ID, Role.EMPLOYEE, null);

        for (Folder target : List.of(folder(IT_DEPARTMENT_ID), folderWithoutDepartment(), root())) {

            assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                    () -> folderAuthorizationHelper.requireUpdate(orphanEmployee, target)).getErrorCode());
            assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                    () -> folderAuthorizationHelper.requireDelete(orphanEmployee, target)).getErrorCode());
            assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                    () -> folderAuthorizationHelper.requireCreate(orphanEmployee, target)).getErrorCode());
        }

        // Đọc thì vẫn được với folder không gắn department.
        assertTrue(folderAuthorizationHelper.canRead(orphanEmployee, folderWithoutDepartment()));
        assertFalse(folderAuthorizationHelper.canRead(orphanEmployee, folder(IT_DEPARTMENT_ID)));
    }

    @Test
    void cannotCreateSubfolderInsideNullDepartmentParent() {

        grant(Role.EMPLOYEE, Permission.FOLDER_CREATE);

        Folder orphanParent = folderWithoutDepartment();

        // Cha không gắn department (dữ liệu cũ) -> non-admin không tạo được folder con bên trong.
        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                () -> folderAuthorizationHelper.requireCreate(employee(), orphanParent)).getErrorCode());

        assertDoesNotThrow(() -> folderAuthorizationHelper.requireCreate(admin(), orphanParent));
    }

    @Test
    void supervisorFolderWritePermissionsAreDeadWeightWithoutDepartment() {

        // SUPERVISOR mặc định không có department -> bật quyền ghi folder cho role này là vô nghĩa.
        grant(Role.SUPERVISOR, Permission.FOLDER_CREATE, Permission.FOLDER_UPDATE, Permission.FOLDER_DELETE);

        UserPrincipal supervisor = principal(SUPERVISOR_ID, Role.SUPERVISOR, null);

        for (Folder target : List.of(folder(IT_DEPARTMENT_ID), folderWithoutDepartment(), root())) {

            // Vẫn đọc được mọi folder nhờ bypass theo role.
            assertTrue(folderAuthorizationHelper.canRead(supervisor, target));

            assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                    () -> folderAuthorizationHelper.requireUpdate(supervisor, target)).getErrorCode());
            assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(AuthorizationException.class,
                    () -> folderAuthorizationHelper.requireDelete(supervisor, target)).getErrorCode());
        }
    }

    // ---------- G. AI usage khi user không có department ----------

    @Test
    void aiUsageDepartmentPermissionIsIneffectiveWithoutDepartment() {

        grant(Role.EMPLOYEE, Permission.AI_USAGE_READ_DEPARTMENT);

        UserPrincipal orphanEmployee = principal(EMPLOYEE_ID, Role.EMPLOYEE, null);

        // Có AI_USAGE_READ_DEPARTMENT nhưng departmentId = null -> rơi xuống scope self.
        AIUsageLogFilterRequest fallback = filterAskingFor(HR_DEPARTMENT_ID);
        aiUsageScopeHelper.applyScope(fallback, orphanEmployee);
        assertEquals(EMPLOYEE_ID, fallback.getUserId());
        assertNull(fallback.getDepartmentId());

        // Tắt AI_USAGE_READ_SELF, chỉ còn AI_USAGE_READ_DEPARTMENT: user không department mất luôn quyền.
        revoke(Role.EMPLOYEE, Permission.AI_USAGE_READ_SELF);
        UserPrincipal stillOrphan = principal(EMPLOYEE_ID, Role.EMPLOYEE, null);

        assertEquals(ErrorCode.PERMISSION_DENIED, assertThrows(AuthorizationException.class,
                () -> aiUsageScopeHelper.applyScope(filterAskingFor(HR_DEPARTMENT_ID), stillOrphan))
                .getErrorCode());

        // Cùng bộ permission đó, user có department thì dùng được scope department.
        AIUsageLogFilterRequest departmentScope = filterAskingFor(HR_DEPARTMENT_ID);
        aiUsageScopeHelper.applyScope(departmentScope, employee());
        assertEquals(IT_DEPARTMENT_ID, departmentScope.getDepartmentId());
    }

    // ---------- Helper ----------

    private void grant(Role role, Permission... permissions) {
        adminRoleService.updateRolePermissions(role, plus(role, permissions));
    }

    private void revoke(Role role, Permission... permissions) {
        adminRoleService.updateRolePermissions(role, minus(role, permissions));
    }

    private List<Permission> plus(Role role, Permission... permissions) {

        Set<Permission> updated = EnumSet.copyOf(rolePermissionService.getPermissions(role));
        updated.addAll(Arrays.asList(permissions));

        // Trả ArrayList (mutable) giống payload Jackson dựng từ request thật: validateUpdatePermissions
        // gọi permissions.contains(null), List.of()/List.copyOf() sẽ ném NPE thay vì PERMISSION_INVALID.
        return new ArrayList<>(updated);
    }

    private List<Permission> minus(Role role, Permission... permissions) {

        Set<Permission> updated = EnumSet.copyOf(rolePermissionService.getPermissions(role));
        Arrays.asList(permissions).forEach(updated::remove);

        return new ArrayList<>(updated);
    }

    private void loginAs(Role role, Long userId, Long departmentId) {
        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principalOf(userId, role, departmentId, rolePermissionHelper.defaultPermissionsOf(role)));
    }

    // Mô phỏng đúng luồng thật: mỗi request nạp lại permission của role từ RolePermissionService.
    private UserPrincipal principal(Long userId, Role role, Long departmentId) {
        return principalOf(userId, role, departmentId, rolePermissionService.getPermissions(role));
    }

    private UserPrincipal employee() {
        return principal(EMPLOYEE_ID, Role.EMPLOYEE, IT_DEPARTMENT_ID);
    }

    private UserPrincipal admin() {
        return principal(ADMIN_ID, Role.ADMIN, null);
    }

    private UserPrincipal principalOf(Long userId, Role role, Long departmentId, Set<Permission> permissions) {

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

        return UserPrincipal.from(user, permissions);
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

    private Folder root() {
        return Folder.builder().id(1L).name("root").build();
    }

    private Folder folder(Long departmentId) {

        return Folder.builder()
                .id(100L + departmentId)
                .name("folder-" + departmentId)
                .parent(root())
                .department(department(departmentId))
                .build();
    }

    // Folder thường (có cha) nhưng chưa gắn department: dữ liệu cũ trước migration department.
    private Folder folderWithoutDepartment() {

        return Folder.builder()
                .id(999L)
                .name("legacy-folder")
                .parent(root())
                .build();
    }

    private AIUsageLogFilterRequest filterAskingFor(Long departmentId) {

        AIUsageLogFilterRequest filter = new AIUsageLogFilterRequest();
        filter.setDepartmentId(departmentId);

        return filter;
    }
}
