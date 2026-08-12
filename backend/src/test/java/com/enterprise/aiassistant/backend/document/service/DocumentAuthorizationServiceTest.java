package com.enterprise.aiassistant.backend.document.service;

import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.document.dto.DocumentAccessScope;
import com.enterprise.aiassistant.backend.document.helper.DocumentAuthorizationHelper;
import com.enterprise.aiassistant.backend.document.repository.DocumentAccessRepository;
import com.enterprise.aiassistant.backend.user.enums.Role;
import com.enterprise.aiassistant.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// currentAccessScope() là nguồn duy nhất tạo DocumentAccessScope cho query layer:
// ADMIN/SUPERVISOR phải luôn ra unrestricted=true (không phải null), EMPLOYEE/MANAGER
// phải luôn bị bó hẹp theo owner/department/share.
class DocumentAuthorizationServiceTest {

    private static final long IT_DEPARTMENT_ID = 2L;

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);

    private final DocumentAccessRepository documentAccessRepository = mock(DocumentAccessRepository.class);

    private DocumentAuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new DocumentAuthorizationService(
                currentUserService,
                documentAccessRepository,
                new DocumentAuthorizationHelper()
        );
    }

    @Test
    void adminGetsUnrestrictedScopeNeverNull() {

        when(currentUserService.getCurrentPrincipal()).thenReturn(principal(1L, Role.ADMIN, null));

        DocumentAccessScope scope = service.currentAccessScope();

        assertNotNull(scope);
        assertTrue(scope.unrestricted());
        verify(documentAccessRepository, never()).findDocumentIdsSharedWithUser(any());
    }

    @Test
    void supervisorGetsUnrestrictedScopeNeverNull() {

        when(currentUserService.getCurrentPrincipal()).thenReturn(principal(2L, Role.SUPERVISOR, null));

        DocumentAccessScope scope = service.currentAccessScope();

        assertNotNull(scope);
        assertTrue(scope.unrestricted());
        verify(documentAccessRepository, never()).findDocumentIdsSharedWithUser(any());
    }

    @Test
    void employeeGetsScopeRestrictedToOwnUserDepartmentAndShares() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principal(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID));
        when(documentAccessRepository.findDocumentIdsSharedWithUser(10L))
                .thenReturn(List.of(55L));

        DocumentAccessScope scope = service.currentAccessScope();

        assertFalse(scope.unrestricted());
        assertEquals(10L, scope.userId());
        assertEquals(IT_DEPARTMENT_ID, scope.departmentId());
        assertEquals(List.of(55L), scope.sharedDocumentIds());
    }

    @Test
    void managerGetsScopeRestrictedToOwnUserDepartmentAndShares() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principal(20L, Role.MANAGER, IT_DEPARTMENT_ID));
        when(documentAccessRepository.findDocumentIdsSharedWithUser(20L))
                .thenReturn(List.of());

        DocumentAccessScope scope = service.currentAccessScope();

        assertFalse(scope.unrestricted());
        assertEquals(20L, scope.userId());
        assertEquals(IT_DEPARTMENT_ID, scope.departmentId());
        assertTrue(scope.sharedDocumentIds().isEmpty());
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
            user.setDepartment(Department.builder().id(departmentId).name("dept-" + departmentId).build());
        }

        return UserPrincipal.from(user, Set.of());
    }
}
